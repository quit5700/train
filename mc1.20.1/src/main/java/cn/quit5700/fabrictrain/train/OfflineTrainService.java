package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.cargo.CargoLedgerService;
import cn.quit5700.fabrictrain.cargo.CargoUnloadRules;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.power.PowerSectionCache;
import cn.quit5700.fabrictrain.registry.ModEntities;
import cn.quit5700.fabrictrain.registry.ModItems;
import cn.quit5700.fabrictrain.depot.DepotRecoveryService;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.route.TrackPoint;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Simulates depot-generated automatic trains while their current route chunk is unloaded. */
public final class OfflineTrainService {
    private static final Set<UUID> STARTUP_RECOVERY = new HashSet<>();
    private OfflineTrainService() {}

    public static OfflineTrainSavedData state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                OfflineTrainSavedData::load, OfflineTrainSavedData::new, OfflineTrainSavedData.DATA_ID);
    }

    public static boolean transition(ServerLevel level, LocomotiveEntity locomotive, List<Entity> formation,
                                     List<TrackPoint> path, boolean loop, int loopStartIndex,
                                     int pathIndex, double segmentProgress, double speed) {
        if (locomotive.trainLink().controlMode() != ControlMode.ROUTE_AUTOMATIC || path.size() < 2) return false;
        ArrayList<OfflineCarState> cars = new ArrayList<>();
        formation.stream().sorted(Comparator.comparingInt(entity -> ((LinkedTrainCar) entity).trainLink().carIndex()))
                .filter(entity -> !(entity instanceof LocomotiveEntity)).forEach(entity -> {
                    if (entity instanceof CargoCarEntity cargo) {
                        ArrayList<ItemStack> items = new ArrayList<>();
                        for (int slot = 0; slot < cargo.getContainerSize(); slot++)
                            if (!cargo.getItem(slot).isEmpty()) items.add(cargo.getItem(slot).copy());
                        cars.add(new OfflineCarState(CarType.CARGO, cargo.destinationStationId(), items));
                    } else cars.add(new OfflineCarState(CarType.PASSENGER, 0L, List.of()));
                });
        OfflineTrainState offline = new OfflineTrainState(locomotive.trainLink().trainId(),
                locomotive.trainLink().lineId(), locomotive.trainLink().trainClass(),
                locomotive.trainLink().configuredSpeedLimitKmh(), pathIndex, segmentProgress,
                Math.max(0D, speed), loop, loopStartIndex, locomotive.trainLink().nextNodeIndex(),
                locomotive.trainLink().lastDeviceId(), locomotive.trainLink().destinationStationId(),
                0L, level.getGameTime(), path, cars);
        state(level.getServer()).put(offline);
        RailwaySavedData data = RailwayDataManager.state(level);
        data.updateTrainPresence(offline.trainId(), TrainPresence.OFFLINE);
        data.registerActiveTrain(offline.trainId(), offline.lineId());
        TrainControllerManager.beginOfflineRemoval(offline.trainId());
        formation.forEach(entity -> { entity.ejectPassengers(); entity.discard(); });
        TrainControllerManager.finishOfflineRemoval(level, offline.trainId());
        return true;
    }

    public static void beginServerRecovery(MinecraftServer server) {
        STARTUP_RECOVERY.clear();
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        OfflineTrainSavedData saved = state(server);
        for (OfflineTrainState original : saved.trains()) {
            ServerLevel level = levelFor(server, original.lineId());
            if (level == null) continue;
            if (STARTUP_RECOVERY.contains(original.trainId())) {
                if (recoverAfterRestart(level, original)) {
                    STARTUP_RECOVERY.remove(original.trainId());
                    saved.remove(original.trainId());
                    TrainControllerManager.retireTrainResources(level, original.trainId());
                }
                continue;
            }
            OfflineTrainState advanced = advance(level, original);
            if (advanced == null) {
                saved.remove(original.trainId());
                TrainControllerManager.retireTrainResources(level, original.trainId());
                continue;
            }
            saved.put(advanced);
            if (currentChunkLoaded(level, advanced)) materialize(level, advanced, saved);
        }
    }

    private static boolean recoverAfterRestart(ServerLevel level, OfflineTrainState state) {
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayDevice depotDevice = data.device(data.recoveryDepotId(state.lineId()));
        if (depotDevice == null || depotDevice.type() != DeviceType.DEPOT
                || !depotDevice.dimension().equals(RailwayDataManager.dimension(level))) return false;
        BlockPos depotPos = new BlockPos(depotDevice.x(), depotDevice.y(), depotDevice.z());
        if (!level.hasChunkAt(depotPos) || !(level.getBlockEntity(depotPos) instanceof TrainDepotBlockEntity depot))
            return false;
        ArrayList<ItemStack> returning = new ArrayList<>();
        returning.add(new ItemStack(ModItems.LOCOMOTIVE));
        for (OfflineCarState car : state.cars()) {
            car.items().stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).forEach(returning::add);
            returning.add(new ItemStack(car.type() == CarType.CARGO ? ModItems.CARGO_CAR : ModItems.PASSENGER_CAR));
        }
        return DepotRecoveryService.recoverItems(level, depot, returning);
    }

    public static void clearRuntime() { STARTUP_RECOVERY.clear(); }

    private static OfflineTrainState advance(ServerLevel level, OfflineTrainState state) {
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayLine line = data.line(state.lineId());
        if (line == null) return null;
        if (line.status() == LineStatus.CLOSED || line.status() == LineStatus.MAINTENANCE
                || line.status() == LineStatus.NEEDS_VALIDATION)
            return copy(state, state.pathIndex(), state.segmentProgress(), state.nextNodeIndex(),
                    state.lastDeviceId(), 0L, level.getGameTime(), state.cars());
        long now = level.getGameTime();
        long elapsed = Math.max(0L, now - state.lastGameTime());
        if (state.dwellUntilGameTime() == Long.MAX_VALUE && line.status() == LineStatus.RUNNING)
            return copy(state, state.pathIndex(), state.segmentProgress(), state.nextNodeIndex(),
                    state.lastDeviceId(), 0L, now, state.cars());
        if (state.dwellUntilGameTime() == Long.MAX_VALUE)
            return copy(state, state.pathIndex(), state.segmentProgress(), state.nextNodeIndex(),
                    state.lastDeviceId(), Long.MAX_VALUE, now, state.cars());
        if (now < state.dwellUntilGameTime()) return copy(state, state.pathIndex(), state.segmentProgress(),
                state.nextNodeIndex(), state.lastDeviceId(), state.dwellUntilGameTime(), now, state.cars());
        int section = Math.max(0, state.pathIndex() - state.loopStartIndex()) / PowerSectionCache.SECTION_LENGTH;
        if (PowerSectionCache.sectionStatus(state.lineId(), section) == cn.quit5700.fabrictrain.power.PowerSectionStatus.UNPOWERED)
            return copy(state, state.pathIndex(), state.segmentProgress(), state.nextNodeIndex(),
                    state.lastDeviceId(), 0L, now, state.cars());
        double remaining = state.speed() * elapsed;
        int pathIndex = state.pathIndex();
        double progress = state.segmentProgress();
        int nextNode = state.nextNodeIndex();
        long lastDevice = progress > 0.01D ? 0L : state.lastDeviceId();
        ArrayList<OfflineCarState> cars = new ArrayList<>(state.cars());
        int budget = Math.max(4, line.nodeIds().size() + 1);
        while (remaining > 0.000001D && budget-- > 0) {
            int expected = Math.min(nextNode, line.nodeIds().size() - 1);
            RailwayDevice device = data.device(line.nodeIds().get(expected));
            int target = device == null ? -1 : nextRouteIndex(state.path(), line, expected, device,
                    pathIndex, state.loop(), state.loopStartIndex());
            double toTarget = target < 0 ? Double.POSITIVE_INFINITY : distanceToIndex(state.path(),
                    state.loop(), state.loopStartIndex(), pathIndex, progress, target);
            if (remaining + 0.000001D < toTarget) {
                OfflineRouteMath.Position moved = OfflineRouteMath.advance(state.path(), state.loop(),
                        state.loopStartIndex(), pathIndex, progress, remaining);
                if (moved.ended() && !state.loop()) return null;
                return copy(state, moved.pathIndex(), moved.progress(), nextNode, 0L, 0L, now, cars);
            }
            if (target < 0 || !Double.isFinite(toTarget)) break;
            remaining = Math.max(0D, remaining - toTarget);
            pathIndex = target;
            progress = 0D;
            lastDevice = device.id();
            if (device.type() == DeviceType.STATION) {
                cars = unloadOfflineCargo(level, state, cars, device.id());
                long dwellUntil = line.status() == LineStatus.PAUSED ? Long.MAX_VALUE
                        : now + (long) StationDwellRules.automaticSeconds(device, state.trainClass()) * 20L;
                nextNode = nextNode(line, state.loop(), nextNode);
                return copy(state, pathIndex, 0D, nextNode, lastDevice, dwellUntil, now, cars);
            }
            if (device.type() == DeviceType.DEPOT && expected == line.nodeIds().size() - 1 && !state.loop())
                return null;
            nextNode = nextNode(line, state.loop(), nextNode);
        }
        OfflineRouteMath.Position moved = OfflineRouteMath.advance(state.path(), state.loop(),
                state.loopStartIndex(), pathIndex, progress, remaining);
        return moved.ended() && !state.loop() ? null : copy(state, moved.pathIndex(), moved.progress(),
                nextNode, lastDevice, 0L, now, cars);
    }

    private static ArrayList<OfflineCarState> unloadOfflineCargo(ServerLevel level, OfflineTrainState train,
                                                                  List<OfflineCarState> cars, long stationId) {
        ArrayList<OfflineCarState> result = new ArrayList<>();
        for (int index = 0; index < cars.size(); index++) {
            OfflineCarState car = cars.get(index);
            if (car.type() != CarType.CARGO || !CargoUnloadRules.shouldUnloadAt(stationId,
                    car.destinationStationId(), train.destinationStationId())) { result.add(car); continue; }
            CargoLedgerService.depositArrived(level, UUID.nameUUIDFromBytes(
                    (train.trainId() + ":" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    stationId, car.items());
            result.add(new OfflineCarState(CarType.CARGO, car.destinationStationId(), List.of()));
        }
        return result;
    }

    private static int nextRouteIndex(List<TrackPoint> path, RailwayLine line, int nodeIndex, RailwayDevice device,
                                      int currentIndex, boolean loop, int loopStart) {
        StopTrack stop = line.stationStopTrack(nodeIndex);
        int x = stop != null ? stop.x() : device.x();
        int y = stop != null ? stop.y() : device.type() == DeviceType.DEPOT ? device.y() + 1 : device.y();
        int z = stop != null ? stop.z() : device.z();
        for (int index = Math.max(0, currentIndex); index < path.size(); index++) {
            TrackPoint point = path.get(index);
            if (point.x() == x && point.y() == y && point.z() == z) return index;
        }
        if (loop) for (int index = cn.quit5700.fabrictrain.compat.MathCompat.clamp(loopStart, 0, path.size() - 1); index < currentIndex; index++) {
            TrackPoint point = path.get(index);
            if (point.x() == x && point.y() == y && point.z() == z) return index;
        }
        return -1;
    }

    private static double distanceToIndex(List<TrackPoint> path, boolean loop, int loopStart,
                                          int currentIndex, double progress, int targetIndex) {
        if (targetIndex == currentIndex && progress < 0.01D) return 0D;
        double distance = 0D;
        int index = currentIndex;
        double used = progress;
        int budget = path.size() + 1;
        while (budget-- > 0) {
            int next = OfflineRouteMath.nextIndex(path, loop, loopStart, index);
            if (next < 0) return Double.POSITIVE_INFINITY;
            distance += Math.max(0D, OfflineRouteMath.distance(path.get(index), path.get(next)) - used);
            index = next; used = 0D;
            if (index == targetIndex) return distance;
        }
        return Double.POSITIVE_INFINITY;
    }

    private static int nextNode(RailwayLine line, boolean loop, int current) {
        int next = current + 1;
        return next >= line.nodeIds().size() ? (loop ? 1 : line.nodeIds().size() - 1) : next;
    }

    private static boolean currentChunkLoaded(ServerLevel level, OfflineTrainState state) {
        int current = cn.quit5700.fabrictrain.compat.MathCompat.clamp(state.pathIndex(), 0, state.path().size() - 1);
        int next = OfflineRouteMath.nextIndex(state.path(), state.loop(), state.loopStartIndex(), current);
        if (next < 0) return false;
        TrackPoint currentPoint = state.path().get(current), nextPoint = state.path().get(next);
        return level.hasChunkAt(new BlockPos(currentPoint.x(), currentPoint.y(), currentPoint.z()))
                && level.hasChunkAt(new BlockPos(nextPoint.x(), nextPoint.y(), nextPoint.z()));
    }

    private static void materialize(ServerLevel level, OfflineTrainState state, OfflineTrainSavedData saved) {
        int nextIndex = OfflineRouteMath.nextIndex(state.path(), state.loop(), state.loopStartIndex(), state.pathIndex());
        if (nextIndex < 0) return;
        TrackPoint fromPoint = state.path().get(state.pathIndex()), toPoint = state.path().get(nextIndex);
        Vec3 from = Vec3.atBottomCenterOf(new BlockPos(fromPoint.x(), fromPoint.y(), fromPoint.z())).add(0, .1D, 0);
        Vec3 to = Vec3.atBottomCenterOf(new BlockPos(toPoint.x(), toPoint.y(), toPoint.z())).add(0, .1D, 0);
        double length = from.distanceTo(to);
        Vec3 position = length < .0001D ? from : from.lerp(to, cn.quit5700.fabrictrain.compat.MathCompat.clamp(state.segmentProgress() / length, 0D, 1D));
        Vec3 tangent = to.subtract(from).normalize();
        LocomotiveEntity locomotive = ModEntities.LOCOMOTIVE.create(level);
        if (locomotive == null) return;
        locomotive.trainLink().assign(state.trainId(), locomotive.getUUID(), 0, state.lineId());
        applyLink(locomotive.trainLink(), state);
        locomotive.setPos(position);
        locomotive.setYRot((float) Math.toDegrees(Math.atan2(-tangent.x, tangent.z)));
        ArrayList<Entity> spawned = new ArrayList<>(); spawned.add(locomotive);
        for (int index = 0; index < state.cars().size(); index++) {
            OfflineCarState carState = state.cars().get(index);
            Entity entity = carState.type() == CarType.CARGO ? ModEntities.CARGO_CAR.create(level)
                    : ModEntities.PASSENGER_CAR.create(level);
            if (!(entity instanceof LinkedTrainCar linked)) return;
            linked.trainLink().assign(state.trainId(), locomotive.getUUID(), index + 1, state.lineId());
            applyLink(linked.trainLink(), state);
            entity.setPos(position.subtract(tangent.scale((index + 1) * FormationSpacing.BLOCKS)));
            entity.setYRot(locomotive.getYRot());
            if (entity instanceof PassengerCarEntity passenger) passenger.releasePlacementBrake();
            if (entity instanceof CargoCarEntity cargo) {
                cargo.releasePlacementBrake(); cargo.setDestinationStationId(carState.destinationStationId());
                for (int slot = 0; slot < Math.min(cargo.getContainerSize(), carState.items().size()); slot++)
                    cargo.setItem(slot, carState.items().get(slot).copy());
            }
            spawned.add(entity);
        }
        if (spawned.stream().anyMatch(entity -> !level.hasChunkAt(entity.blockPosition()))) return;
        ArrayList<Entity> added = new ArrayList<>();
        for (Entity entity : spawned) {
            if (!level.addFreshEntity(entity)) {
                TrainControllerManager.beginOfflineRemoval(state.trainId());
                added.forEach(Entity::discard);
                TrainControllerManager.finishOfflineRemoval(level, state.trainId());
                return;
            }
            added.add(entity);
        }
        saved.remove(state.trainId());
        RailwayDataManager.state(level).updateTrainPresence(state.trainId(), TrainPresence.LOADED);
        TrainRegistryService.replaceFormation(level, state.trainId(), spawned);
        TrainControllerManager.restoreOfflinePath(state, locomotive);
    }

    private static void applyLink(TrainLink link, OfflineTrainState state) {
        link.setDepotGenerated(true); link.setControlMode(ControlMode.ROUTE_AUTOMATIC);
        link.setTrainClass(state.trainClass()); link.setConfiguredSpeedLimitKmh(state.configuredSpeedLimitKmh());
        link.setDestinationStationId(state.destinationStationId());
        link.updateRouteProgress(state.nextNodeIndex(), state.lastDeviceId(), 0L);
        link.setAutomaticRunning(true);
    }

    private static ServerLevel levelFor(MinecraftServer server, long lineId) {
        for (ServerLevel level : server.getAllLevels()) if (RailwayDataManager.state(level).line(lineId) != null) return level;
        return null;
    }

    private static OfflineTrainState copy(OfflineTrainState state, int index, double progress, int nextNode,
                                          long lastDevice, long dwellUntil, long now, List<OfflineCarState> cars) {
        return new OfflineTrainState(state.trainId(), state.lineId(), state.trainClass(),
                state.configuredSpeedLimitKmh(), index, progress, state.speed(), state.loop(),
                state.loopStartIndex(), nextNode, lastDevice, state.destinationStationId(), dwellUntil,
                now, state.path(), cars);
    }

}

