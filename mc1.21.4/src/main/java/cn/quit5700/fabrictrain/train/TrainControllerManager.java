package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.cargo.CargoTransferService;
import cn.quit5700.fabrictrain.cargo.CargoUnloadRules;
import cn.quit5700.fabrictrain.cargo.CargoLedgerService;
import cn.quit5700.fabrictrain.depot.DepotRecoveryService;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.properties.RailShape;
import cn.quit5700.fabrictrain.route.RuntimePathCache;
import cn.quit5700.fabrictrain.route.TrackPoint;
import cn.quit5700.fabrictrain.route.LineLoopRules;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.power.PowerSectionCache;
import net.minecraft.network.chat.Component;
import cn.quit5700.fabrictrain.route.SwitchRailRouting;
import cn.quit5700.fabrictrain.route.RealtimeSwitchTraffic;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import cn.quit5700.fabrictrain.registry.ModBlocks;

import java.util.*;

public final class TrainControllerManager {
    private static final double CAR_SPACING = FormationSpacing.BLOCKS;
    private static final Map<UUID, TrackTrail> TRAILS = new HashMap<>();
    private static final Map<UUID, PausedTrain> PAUSED = new HashMap<>();
    private static final Map<UUID, RouteProgress> ROUTES = new HashMap<>();
    private static final Map<UUID, LogicalMotion> LOGICAL_MOTIONS = new HashMap<>();
    private static final Map<UUID, AutomaticPath> AUTOMATIC_PATHS = new HashMap<>();
    private static long ticks;
    private static final Set<Long> EMERGENCY_STATIONS=new HashSet<>();
    private static final Map<UUID, Integer> CAR_ONLY_INPUT = new HashMap<>();
    private static final Map<UUID, UUID> DIAGNOSTIC_DRIVERS = new HashMap<>();
    private static final Map<UUID, String> LAST_DIAGNOSTIC_REASON = new HashMap<>();
    private static final Map<UUID,ManualInput> MANUAL=new HashMap<>();
    private static final Map<SwitchTrafficKey, UUID> REALTIME_SWITCH_RIGHTS = new HashMap<>();
    private static final Map<UUID, ServerLevel> PENDING_DESTROYED_TRAINS = new HashMap<>();
    private static final Set<UUID> OFFLINE_REMOVALS = new HashSet<>();

    private TrainControllerManager() {
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        Set<UUID> active = new HashSet<>();
        Set<UUID> observed = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            Map<UUID, List<Entity>> trains = new HashMap<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LinkedTrainCar linked && !entity.isRemoved()) {
                    trains.computeIfAbsent(linked.trainLink().trainId(), ignored -> new ArrayList<>()).add(entity);
                }
            }
            retireDestroyedTrainsMissingFromLevel(level, trains);
            RailwaySavedData levelData = RailwayDataManager.state(level);
            trains.entrySet().removeIf(entry -> {
                if (!levelData.isTrainPendingDeletion(entry.getKey())) return false;
                entry.getValue().forEach(entity -> {
                    entity.ejectPassengers();
                    entity.discard();
                });
                retireTrainResources(level, entry.getKey());
                return true;
            });
            observed.addAll(trains.keySet());
            rebuildRealtimeSwitchRights(level, trains);
            trains.forEach((trainId, cars) -> {
                LinkedTrainCar sample = (LinkedTrainCar) cars.getFirst();
                RailwaySavedData registryData = RailwayDataManager.state(level);
                boolean firstMembershipObservation = registryData.trainManifest(trainId) == null;
                registryData.ensureTrain(trainId,
                        sample.trainLink().depotGenerated(), sample.trainLink().lineId(),sample.trainLink().controlMode());
                cars.forEach(entity -> TrainRegistryService.observeLoadedVehicle(
                        level, entity, (LinkedTrainCar) entity));
                if (firstMembershipObservation)
                    TrainRegistryService.replaceFormation(level, trainId,
                            TrainRegistryService.stableOrder(cars));
                boolean hasLocomotive = cars.stream().anyMatch(LocomotiveEntity.class::isInstance);
                boolean controlled = hasLocomotive || cars.size() > 1;
                cars.forEach(entity -> {
                    if (entity instanceof LogicallyControlledVehicle vehicle)
                        vehicle.fabricTrain$setLogicallyControlled(controlled);
                });
                if (hasLocomotive) tickTrain(level, trainId, cars, active);
                else if (cars.size() > 1) tickCarOnlyGroup(level, trainId, cars, active);
            });
        }
        ArrayList<Map.Entry<UUID, ServerLevel>> destroyed = new ArrayList<>(PENDING_DESTROYED_TRAINS.entrySet());
        for (Map.Entry<UUID, ServerLevel> entry : destroyed) {
            if (observed.contains(entry.getKey())) continue;
            retireTrainResources(entry.getValue(), entry.getKey());
        }
        TRAILS.keySet().removeIf(id -> !active.contains(id));
        LOGICAL_MOTIONS.keySet().removeIf(id -> !active.contains(id));
        AUTOMATIC_PATHS.keySet().removeIf(id -> !active.contains(id));
        ROUTES.keySet().removeIf(id -> !active.contains(id));
        REALTIME_SWITCH_RIGHTS.entrySet().removeIf(entry -> !active.contains(entry.getValue()));
        PAUSED.entrySet().removeIf(entry -> {
            if (entry.getValue().resumeAtTick() > ticks) return false;
            Entity entity = findEntity(server, entry.getKey());
            if (entity instanceof LocomotiveEntity locomotive) {
                if(EMERGENCY_STATIONS.contains(locomotive.trainLink().lastDeviceId()))return false;
                RailwayLine line = RailwayDataManager.state((ServerLevel) locomotive.level())
                        .line(locomotive.trainLink().lineId());
                if (AutomaticLinePauseRules.holdAtStation(locomotive.trainLink().controlMode(),
                        line == null ? null : line.status())) return false;
                locomotive.setDeltaMovement(entry.getValue().resumeVelocity());
                locomotive.trainLink().setAutomaticRunning(true);
            }
            return true;
        });
    }

    public static void noteVehicleDestroyed(ServerLevel level, UUID trainId) {
        if (level == null || trainId == null) return;
        if (OFFLINE_REMOVALS.contains(trainId)) return;
        PENDING_DESTROYED_TRAINS.put(trainId, level);
    }

    public static void noteVehicleRemoved(ServerLevel level, UUID trainId, UUID entityId,
                                          Entity.RemovalReason reason) {
        if (level == null || trainId == null || entityId == null || reason == null) return;
        if (reason.shouldDestroy()) {
            if (OFFLINE_REMOVALS.contains(trainId)) return;
            RailwaySavedData.MemberRemovalResult result = TrainRegistryService.removeVehicle(
                    level, trainId, entityId);
            if (result == RailwaySavedData.MemberRemovalResult.TRAIN_EMPTY)
                retireTrainResources(level, trainId);
            else if (result == RailwaySavedData.MemberRemovalResult.UNKNOWN_MEMBER)
                noteVehicleDestroyed(level, trainId);
        }
        else if (!OFFLINE_REMOVALS.contains(trainId))
            RailwayDataManager.state(level).updateTrainPresence(trainId, TrainPresence.UNLOADED);
    }

    static void beginOfflineRemoval(UUID trainId) { OFFLINE_REMOVALS.add(trainId); }
    static void finishOfflineRemoval(ServerLevel level, UUID trainId) {
        OFFLINE_REMOVALS.remove(trainId);
        releaseTrainSwitches(trainId);
        TRAILS.remove(trainId); LOGICAL_MOTIONS.remove(trainId); AUTOMATIC_PATHS.remove(trainId);
        ROUTES.remove(trainId); PAUSED.entrySet().removeIf(entry -> entry.getKey().equals(trainId));
    }

    static void restoreOfflinePath(OfflineTrainState state, LocomotiveEntity locomotive) {
        AUTOMATIC_PATHS.put(state.trainId(), new AutomaticPath(state.path(), state.loop(),
                state.loopStartIndex(), state.loopStartIndex()));
        LogicalMotion motion = new LogicalMotion();
        motion.speed.restore(state.speed());
        LOGICAL_MOTIONS.put(state.trainId(), motion);
        ROUTES.put(state.trainId(), new RouteProgress(state.nextNodeIndex(), state.lastDeviceId(), 0L));
    }

    private static void retireDestroyedTrainsMissingFromLevel(ServerLevel level,
                                                              Map<UUID, List<Entity>> trains) {
        new ArrayList<>(PENDING_DESTROYED_TRAINS.entrySet()).stream()
                .filter(entry -> entry.getValue() == level && !trains.containsKey(entry.getKey()))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(trainId -> retireTrainResources(level, trainId));
    }

    public static void retireTrainResources(ServerLevel level, UUID trainId) {
        if (level == null || trainId == null) return;
        releaseTrainSwitches(trainId);
        TRAILS.remove(trainId);
        LOGICAL_MOTIONS.remove(trainId);
        AUTOMATIC_PATHS.remove(trainId);
        ROUTES.remove(trainId);
        CAR_ONLY_INPUT.remove(trainId);
        DIAGNOSTIC_DRIVERS.remove(trainId);
        LAST_DIAGNOSTIC_REASON.remove(trainId);
        PENDING_DESTROYED_TRAINS.remove(trainId);
        RailwaySavedData data = RailwayDataManager.state(level);
        data.markTrainForDeletion(trainId);
        OfflineTrainService.state(level.getServer()).remove(trainId);
    }

    /** Administrative deletion: removes the physical formation and every server-side resource without drops. */
    public static boolean deleteTrainEverywhere(MinecraftServer server, UUID trainId) {
        if (server == null || trainId == null) return false;
        boolean found = false;
        for (ServerLevel level : server.getAllLevels()) {
            RailwaySavedData data = RailwayDataManager.state(level);
            if (data.trainRecord(trainId) != null) found = true;
            data.markTrainForDeletion(trainId);
            List<Entity> vehicles = new ArrayList<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LinkedTrainCar car && car.trainLink().trainId().equals(trainId))
                    vehicles.add(entity);
            }
            if (!vehicles.isEmpty()) found = true;
            vehicles.forEach(entity -> { entity.ejectPassengers(); entity.discard(); });
            retireTrainResources(level, trainId);
        }
        return found;
    }

    public static Set<UUID> automaticTrainIds(ServerLevel level, long lineId) {
        Set<UUID> result = new HashSet<>();
        RailwaySavedData data = RailwayDataManager.state(level);
        data.presentTrainRecords().forEach((trainId, record) -> {
            if (record.lineId() == lineId && record.controlMode() == ControlMode.ROUTE_AUTOMATIC)
                result.add(trainId);
        });
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LinkedTrainCar car
                    && car.trainLink().lineId() == lineId
                    && car.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC)
                result.add(car.trainLink().trainId());
        }
        return Set.copyOf(result);
    }

    /** Removes only the previous automatic formations after their replacement was spawned successfully. */
    public static void refreshAutomaticLine(ServerLevel level, long lineId, UUID replacementTrainId,
                                            Set<UUID> previousAutomaticTrains) {
        if (level == null || previousAutomaticTrains == null) return;
        Set<UUID> retired = new HashSet<>(previousAutomaticTrains);
        retired.remove(replacementTrainId);
        if (retired.isEmpty()) return;
        for (UUID trainId : retired) RailwayDataManager.state(level).markTrainForDeletion(trainId);
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LinkedTrainCar car) || !retired.contains(car.trainLink().trainId())) continue;
            entity.ejectPassengers();
            entity.discard();
        }
        retired.forEach(trainId -> retireTrainResources(level, trainId));
    }

    public static void drive(ServerPlayer player, Entity occupiedVehicle, boolean forward, boolean brake) {
        if (!(occupiedVehicle instanceof LinkedTrainCar occupiedCar)) return;
        UUID trainId = occupiedCar.trainLink().trainId();
        List<Entity> diagnosticFormation = TrainFormationService.formation(occupiedVehicle);
        Entity diagnosticLeader = diagnosticFormation.isEmpty() ? occupiedVehicle : diagnosticFormation.getFirst();
        UUID winner = closestDriverId(player, trainId, diagnosticLeader);
        if (forward || brake) {
            DIAGNOSTIC_DRIVERS.put(trainId, player.getUUID());
            LAST_DIAGNOSTIC_REASON.remove(trainId);
        }
        if (!occupiedCar.trainLink().controlMode().playerDriveAllowed()) {
            cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable("message.fabric_train_simulator.train.depot_controlled"));
            diagnostic(player, "message.fabric_train_simulator.diagnostic.rejected",
                    Component.translatable("message.fabric_train_simulator.diagnostic.reason.depot"));
            return;
        }
        UUID locomotiveId = occupiedCar.trainLink().locomotiveId();
        Entity found = findEntity(player.getServer(), locomotiveId);
        if (!(found instanceof LocomotiveEntity locomotive)) {
            if (occupiedCar.trainLink().hasLocomotive()) {
                if (forward || brake) diagnostic(player,
                        "message.fabric_train_simulator.diagnostic.rejected",
                        Component.translatable("message.fabric_train_simulator.diagnostic.reason.locomotive_missing"));
                return;
            }
            List<Entity> formation = TrainFormationService.formation(occupiedVehicle);
            if (formation.isEmpty()) return;
            Entity leader = formation.getFirst();
            if (!isClosestDriver(player, occupiedCar.trainLink().trainId(), leader)) {
                if (forward || brake) cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable(
                        "message.fabric_train_simulator.train.no_control"));
                if (forward || brake) diagnostic(player,
                        "message.fabric_train_simulator.diagnostic.rejected",
                        Component.translatable("message.fabric_train_simulator.diagnostic.reason.authority"));
                return;
            }
            int input = forward ? 1 : brake ? -1 : 0;
            CAR_ONLY_INPUT.put(occupiedCar.trainLink().trainId(), input);
            formation.forEach(entity -> {
                if (entity instanceof PassengerCarEntity passenger) passenger.releasePlacementBrake();
                if (entity instanceof CargoCarEntity cargo) cargo.releasePlacementBrake();
            });
            return;
        }
        if (!isClosestDriver(player, occupiedCar.trainLink().trainId(), locomotive)) {
            if (forward || brake) cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable(
                    "message.fabric_train_simulator.train.no_control"));
            if (forward || brake) diagnostic(player,
                    "message.fabric_train_simulator.diagnostic.rejected",
                    Component.translatable("message.fabric_train_simulator.diagnostic.reason.authority"));
            return;
        }
        if (forward && railDirection(locomotive) == null) {
            cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable("message.fabric_train_simulator.train.not_on_rail"));
            diagnostic(player, "message.fabric_train_simulator.diagnostic.rejected",
                    Component.translatable("message.fabric_train_simulator.diagnostic.reason.not_on_rail"));
            return;
        }
        locomotive.trainLink().setParked(false);
        for (Entity entity : player.serverLevel().getAllEntities()) {
            if (entity instanceof LinkedTrainCar car
                    && car.trainLink().trainId().equals(occupiedCar.trainLink().trainId())) {
                car.trainLink().setParked(false);
                if (entity instanceof PassengerCarEntity passenger) passenger.releasePlacementBrake();
                if (entity instanceof CargoCarEntity cargo) cargo.releasePlacementBrake();
            }
        }
        PAUSED.remove(locomotiveId);
        if(forward){MANUAL.remove(locomotiveId);locomotive.trainLink().setAutomaticRunning(true);}
        else if(brake){locomotive.trainLink().setAutomaticRunning(false);MANUAL.put(locomotiveId,new ManualInput(false,true,ticks));}
        else if(MANUAL.remove(locomotiveId)!=null){locomotive.setDeltaMovement(Vec3.ZERO);PAUSED.put(locomotiveId,new PausedTrain(ticks+AutoResumeTimer.DEFAULT_DELAY_TICKS,direction(locomotive).scale(targetSpeed(locomotive))));}
    }

    public static void registerAutomaticPath(UUID trainId, List<cn.quit5700.fabrictrain.route.TrackPoint> path,
                                             boolean loop, int loopStartIndex) {
        if (trainId == null || path == null || path.size() < 2) return;
        AUTOMATIC_PATHS.put(trainId, new AutomaticPath(List.copyOf(path), loop,
                Math.clamp(loopStartIndex, 0, path.size() - 2), Math.max(0, loopStartIndex)));
        LOGICAL_MOTIONS.remove(trainId);
    }

    private static void diagnostic(ServerPlayer player, String key, Object... arguments) {
        // End-of-development diagnostics are intentionally no longer player-visible.
    }

    public static void emergencyStopAt(long stationId){EMERGENCY_STATIONS.add(stationId);}
    public static void releaseEmergencyAt(long stationId){EMERGENCY_STATIONS.remove(stationId);}
    public static void continueAutomaticLine(MinecraftServer server, long lineId) {
        PAUSED.replaceAll((locomotiveId, paused) -> {
            Entity entity = findEntity(server, locomotiveId);
            if (entity instanceof LocomotiveEntity locomotive
                    && locomotive.trainLink().lineId() == lineId
                    && locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC) {
                return new PausedTrain(ticks, paused.resumeVelocity());
            }
            return paused;
        });
    }
    public static void departStation(MinecraftServer server,long stationId){
        EMERGENCY_STATIONS.remove(stationId);
        for(ServerLevel level:server.getAllLevels())for(Entity entity:level.getAllEntities())if(entity instanceof LocomotiveEntity locomotive&&locomotive.trainLink().lastDeviceId()==stationId){
            PausedTrain paused=PAUSED.remove(locomotive.getUUID());Vec3 velocity=paused==null?direction(locomotive).scale(targetSpeed(locomotive)):paused.resumeVelocity();locomotive.setDeltaMovement(velocity);locomotive.trainLink().setAutomaticRunning(true);
        }
    }
    public static void startLine(MinecraftServer server,long lineId){for(ServerLevel level:server.getAllLevels())for(Entity entity:level.getAllEntities())if(entity instanceof LocomotiveEntity locomotive&&locomotive.trainLink().lineId()==lineId){locomotive.trainLink().setAutomaticRunning(true);PAUSED.remove(locomotive.getUUID());}}

    private static boolean isClosestDriver(ServerPlayer player, UUID trainId, Entity leader) {
        return player.getUUID().equals(closestDriverId(player, trainId, leader));
    }

    private static UUID closestDriverId(ServerPlayer player, UUID trainId, Entity leader) {
        ArrayList<TrainDriverAuthority.Rider> riders = new ArrayList<>();
        for (Entity vehicle : player.serverLevel().getAllEntities()) {
            if (!(vehicle instanceof LinkedTrainCar linked) || !linked.trainLink().trainId().equals(trainId)) continue;
            for (Entity passenger : vehicle.getPassengers()) {
                if (passenger instanceof ServerPlayer rider)
                    riders.add(new TrainDriverAuthority.Rider(rider.getUUID(), rider.distanceToSqr(leader)));
            }
        }
        return TrainDriverAuthority.closest(riders);
    }

    private static Vec3 direction(LocomotiveEntity locomotive) {
        return locomotive.trainLink().formationForward()
                .map(value->new Vec3(value.getStepX(),0,value.getStepZ()))
                .orElseGet(()->direction((Entity)locomotive));
    }

    private static Vec3 railDirection(LocomotiveEntity locomotive) {
        BlockPos railPos = locomotive.getCurrentBlockPosOrRailBelow();
        if (!BaseRailBlock.isRail(locomotive.level(), railPos)) return null;
        var state = locomotive.level().getBlockState(railPos);
        if (!state.hasProperty(RailBlock.SHAPE)) return null;
        var exits = AbstractMinecart.exits(state.getValue(RailBlock.SHAPE));
        return RailStartDirection.choose(exits.getFirst(), exits.getSecond(), direction(locomotive));
    }

    private static void rebuildRealtimeSwitchRights(ServerLevel level, Map<UUID, List<Entity>> trains) {
        String dimension = RailwayDataManager.dimension(level);
        REALTIME_SWITCH_RIGHTS.keySet().removeIf(key -> key.dimension().equals(dimension));
        RailwaySavedData data = RailwayDataManager.state(level);
        Map<Long, List<RealtimeSwitchTraffic.Candidate>> candidates = new HashMap<>();
        List<RailwayDevice> switches = data.devices().values().stream()
                .filter(device -> device.type() == DeviceType.SWITCH && device.dimension().equals(dimension))
                .toList();

        for (RailwayDevice device : switches) {
            Vec3 center = Vec3.atCenterOf(new BlockPos(device.x(), device.y(), device.z()));
            for (Map.Entry<UUID, List<Entity>> train : trains.entrySet()) {
                double nearest = train.getValue().stream().mapToDouble(entity -> entity.distanceToSqr(center))
                        .min().orElse(Double.MAX_VALUE);
                if (nearest <= 16.0D) candidates.computeIfAbsent(device.id(), ignored -> new ArrayList<>())
                        .add(new RealtimeSwitchTraffic.Candidate(train.getKey(), nearest, true));
            }
        }

        trains.forEach((trainId, cars) -> cars.stream().filter(LocomotiveEntity.class::isInstance)
                .map(LocomotiveEntity.class::cast).findFirst().ifPresent(locomotive -> {
                    RailwayDevice target = locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC
                            ? nextAutomaticSwitch(data, locomotive) : nextManualSwitch(level, data, locomotive);
                    if (target == null) return;
                    double distanceSquared = locomotive.distanceToSqr(
                            Vec3.atCenterOf(new BlockPos(target.x(), target.y(), target.z())));
                    double reservation = RealtimeSwitchTraffic.reservationDistance(currentLogicalSpeed(trainId));
                    if (distanceSquared <= reservation * reservation) {
                        candidates.computeIfAbsent(target.id(), ignored -> new ArrayList<>())
                                .add(new RealtimeSwitchTraffic.Candidate(trainId, distanceSquared, false));
                    }
                }));

        candidates.forEach((deviceId, values) -> RealtimeSwitchTraffic.chooseWinner(values)
                .ifPresent(trainId -> REALTIME_SWITCH_RIGHTS.put(
                        new SwitchTrafficKey(dimension, deviceId), trainId)));
    }

    private static RailwayDevice nextAutomaticSwitch(RailwaySavedData data, LocomotiveEntity locomotive) {
        RailwayLine line = data.line(locomotive.trainLink().lineId());
        if (line == null || line.nodeIds().isEmpty()) return null;
        RouteProgress progress = routeProgress(locomotive);
        int index = Math.min(progress.nextNodeIndex, line.nodeIds().size() - 1);
        RailwayDevice device = data.device(line.nodeIds().get(index));
        return device != null && device.type() == DeviceType.SWITCH ? device : null;
    }

    private static RailwayDevice nextManualSwitch(ServerLevel level, RailwaySavedData data,
                                                   LocomotiveEntity locomotive) {
        net.minecraft.core.Direction travel = travelDirection(locomotive);
        BlockPos railPos = locomotive.getCurrentBlockPosOrRailBelow();
        for (int distance = 0; distance <= 2; distance++) {
            BlockPos switchPos = switchAt(level, railPos.relative(travel, distance));
            if (switchPos == null) continue;
            return data.find(RailwayDataManager.dimension(level),
                    switchPos.getX(), switchPos.getY(), switchPos.getZ());
        }
        return null;
    }

    private static double currentLogicalSpeed(UUID trainId) {
        LogicalMotion motion = LOGICAL_MOTIONS.get(trainId);
        return motion == null ? 0.0D : Math.abs(motion.speed.speed());
    }

    private static boolean hasSwitchRight(ServerLevel level, long deviceId, UUID trainId) {
        UUID winner = REALTIME_SWITCH_RIGHTS.get(
                new SwitchTrafficKey(RailwayDataManager.dimension(level), deviceId));
        return winner == null || winner.equals(trainId);
    }

    private static FollowingTrainDecision automaticFollowingDecision(ServerLevel level,
                                                                     LocomotiveEntity locomotive) {
        if (locomotive.trainLink().controlMode() != ControlMode.ROUTE_AUTOMATIC)
            return FollowingTrainDecision.clear();
        UUID trainId = locomotive.trainLink().trainId();
        net.minecraft.core.Direction direction = travelDirection(locomotive);
        double forwardX = direction.getStepX();
        double forwardZ = direction.getStepZ();
        double required = TrainTrafficSafety.stoppingDistance(currentLogicalSpeed(trainId));
        FollowingTrainDecision result = FollowingTrainDecision.clear();
        for (Entity other : level.getAllEntities()) {
            if (!(other instanceof LinkedTrainCar linked) || other.isRemoved()
                    || linked.trainLink().trainId().equals(trainId)) continue;
            double dx = other.getX() - locomotive.getX();
            double dz = other.getZ() - locomotive.getZ();
            double forward = dx * forwardX + dz * forwardZ;
            double lateralSquared = Math.max(0.0D, dx * dx + dz * dz - forward * forward);
            if (!TrainTrafficSafety.blocks(forward, lateralSquared,
                    other.getY() - locomotive.getY(), required)) continue;
            LocomotiveEntity otherLocomotive = locomotiveForTrain(level, linked.trainLink().trainId());
            boolean sameDirection = otherLocomotive != null
                    && travelDirection(locomotive).getUnitVec3().dot(
                    travelDirection(otherLocomotive).getUnitVec3()) > 0.5D;
            FollowingTrainDecision candidate = FollowingTrainDecision.evaluate(true, sameDirection,
                    currentLogicalSpeed(linked.trainLink().trainId()));
            result = result.restrictWith(candidate);
            if (result.hardStop()) {
                notifyDiagnostic(level, trainId, "waiting_train", currentLogicalSpeed(trainId),
                        locomotive.blockPosition());
                return result;
            }
        }
        return result;
    }

    private static LocomotiveEntity locomotiveForTrain(ServerLevel level, UUID trainId) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LocomotiveEntity locomotive
                    && locomotive.trainLink().trainId().equals(trainId) && !entity.isRemoved()) return locomotive;
        }
        return null;
    }

    private static void tickTrain(ServerLevel level, UUID trainId, List<Entity> cars, Set<UUID> active) {
        LocomotiveEntity locomotive = cars.stream()
                .filter(LocomotiveEntity.class::isInstance)
                .map(LocomotiveEntity.class::cast)
                .findFirst().orElse(null);
        if (locomotive == null) return;
        active.add(trainId);
        RouteProgress persistedProgress = routeProgress(locomotive);
        if (persistedProgress.lockedSwitchId != 0L) {
            persistedProgress.lockedSwitchId = 0L;
            syncRouteProgress(locomotive, persistedProgress);
        }
        LogicalMotion motion = LOGICAL_MOTIONS.get(trainId);
        BlockPos actualRail = motion != null && motion.cursor != null
                && BaseRailBlock.isRail(level, motion.cursor.currentRail())
                ? motion.cursor.currentRail() : RailPositionResolver.resolve(level, locomotive);
        if (locomotive.trainLink().lineId() > 0 && actualRail == null) {
            releaseTrainSwitches(trainId);
            DerailedTrainRecoveryService.dropAsItems(level, cars);
            return;
        }
        if (locomotive.trainLink().loadedFromSave() && tryStartupRecovery(level, locomotive, cars)) return;
        RailwayLine operatingLine = RailwayDataManager.state(level).line(locomotive.trainLink().lineId());
        if (operatingLine == null && !prepareManualSwitch(level, locomotive, cars)) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return;
        }
        if (operatingLine != null && operatingLine.status() == LineStatus.MAINTENANCE) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return;
        }
        if (operatingLine != null && locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC
                && !ensureAutomaticCursor(level, trainId, locomotive)) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return;
        }
        motion = LOGICAL_MOTIONS.get(trainId);
        boolean nextLoaded = motion != null && motion.cursor != null
                ? nextRouteChunkLoaded(level, motion.cursor)
                : nextTrainChunkLoaded(level, locomotive);
        if (!nextLoaded) {
            AutomaticPath path = AUTOMATIC_PATHS.get(trainId);
            LogicalMotion offlineMotion = LOGICAL_MOTIONS.get(trainId);
            if (path != null && offlineMotion != null && offlineMotion.cursor != null
                    && locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC
                    && OfflineTrainService.transition(level, locomotive, cars, path.points(), path.loop(),
                    path.loopStartIndex(), offlineMotion.cursor.routeIndex(), offlineMotion.cursor.segmentProgress(),
                    Math.max(offlineMotion.speed.speed(), targetSpeed(locomotive)))) return;
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return;
        }
        motion = LOGICAL_MOTIONS.get(trainId);
        AutomaticPath activePath = AUTOMATIC_PATHS.get(trainId);
        boolean powered = operatingLine == null || (motion != null && motion.cursor != null && activePath != null
                ? motion.cursor.routeIndex() < activePath.linePathStartIndex()
                    ? actualRail != null && RailPowerService.isPowered(level, actualRail)
                    : PowerSectionCache.canProceed(operatingLine.id(),
                        motion.cursor.routeIndex() - activePath.linePathStartIndex())
                : actualRail != null && RailPowerService.isPowered(level, actualRail));
        if (!powered) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return;
        }
        FollowingTrainDecision following = automaticFollowingDecision(level, locomotive);
        boolean routeClear = !following.hardStop() && prepareNextSwitch(level, locomotive, cars);
        if (routeClear) {
            if (operatingLine == null) processManualStationAnnouncement(level, locomotive);
            else processLineNode(level, locomotive, cars);
        }
        TrackTrail trail = TRAILS.computeIfAbsent(trainId, ignored -> new TrackTrail(140.0D));
        if (!advanceLogicalTrain(level, trainId, locomotive, routeClear, following.speedLimit(), trail)) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
        }
        cars.stream()
                .filter(entity -> entity != locomotive)
                .sorted(Comparator.comparingInt(entity -> ((LinkedTrainCar) entity).trainLink().carIndex()))
                .forEach(entity -> follow(level, locomotive, entity, trail));
    }

    private static boolean advanceLogicalTrain(ServerLevel level, UUID trainId, LocomotiveEntity locomotive,
                                               boolean routeClear, double followingSpeedLimit, TrackTrail trail) {
        LogicalMotion motion = LOGICAL_MOTIONS.computeIfAbsent(trainId, ignored -> new LogicalMotion());
        ManualInput manual = MANUAL.get(locomotive.getUUID());
        double target = 0;
        if (routeClear && !PAUSED.containsKey(locomotive.getUUID())) {
            if (manual != null && manual.backward()) target = -targetSpeed(locomotive);
            else if (manual != null && manual.forward()) target = targetSpeed(locomotive);
            else if (locomotive.trainLink().automaticRunning()) target = targetSpeed(locomotive);
            if (Double.isFinite(followingSpeedLimit))
                target = Math.copySign(Math.min(Math.abs(target), followingSpeedLimit), target);
        }
        double acceleration = routeClear ? 0.015D : TrainTrafficSafety.EMERGENCY_DECELERATION;
        double speed = motion.speed.approach(target, acceleration);
        if (Math.abs(speed) < 0.0001D) {
            locomotive.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        if (motion.cursor == null) {
            AutomaticPath automaticPath = locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC
                    ? AUTOMATIC_PATHS.get(trainId) : null;
            motion.cursor = automaticPath == null
                    ? RailMotionCursor.create(level, locomotive, direction(locomotive))
                    : RailMotionCursor.createRoute(level, locomotive, automaticPath.points(), automaticPath.loop(),
                    automaticPath.loopStartIndex());
            if (motion.cursor == null) {
                motion.speed.stop();
                locomotive.trainLink().setAutomaticRunning(false);
                releaseTrainSwitches(trainId);
                notifyDiagnostic(level, trainId, "cursor_missing", motion.speed.speed(), locomotive.blockPosition());
                return false;
            }
        }
        RailMotionCursor.Pose pose = motion.cursor.advance(level, speed);
        if (pose == null || !level.hasChunkAt(BlockPos.containing(pose.position()))) {
            motion.speed.stop();
            locomotive.trainLink().setAutomaticRunning(false);
            if (pose == null) releaseTrainSwitches(trainId);
            notifyDiagnostic(level, trainId, pose == null ? "rail_ended" : "chunk_unloaded",
                    motion.speed.speed(), locomotive.blockPosition());
            return false;
        }
        locomotive.setPos(pose.position());
        locomotive.setYRot(pose.yaw());
        FormationDirection.fromPositions(-Math.sin(Math.toRadians(pose.yaw())),
                Math.cos(Math.toRadians(pose.yaw())),0,0).ifPresent(locomotive.trainLink()::setFormationForward);
        locomotive.setXRot(0);
        locomotive.setDeltaMovement(Vec3.ZERO);
        trail.record(pose.position().x, pose.position().y, pose.position().z);
        notifyDiagnostic(level, trainId, "moving", speed, BlockPos.containing(pose.position()));
        return true;
    }

    private static boolean ensureAutomaticCursor(ServerLevel level, UUID trainId, LocomotiveEntity locomotive) {
        LogicalMotion motion = LOGICAL_MOTIONS.computeIfAbsent(trainId, ignored -> new LogicalMotion());
        if (motion.cursor != null) return true;
        AutomaticPath automaticPath = AUTOMATIC_PATHS.get(trainId);
        if (automaticPath == null) return false;
        motion.cursor = RailMotionCursor.createRoute(level, locomotive, automaticPath.points(), automaticPath.loop(),
                automaticPath.loopStartIndex());
        if (motion.cursor != null) return true;
        motion.speed.stop();
        locomotive.trainLink().setAutomaticRunning(false);
        releaseTrainSwitches(trainId);
        notifyDiagnostic(level, trainId, "cursor_missing", motion.speed.speed(), locomotive.blockPosition());
        return false;
    }

    private static void notifyDiagnostic(ServerLevel level, UUID trainId, String reason, double speed, BlockPos pos) {
        LAST_DIAGNOSTIC_REASON.put(trainId, reason);
    }

    private static void applyManual(LocomotiveEntity locomotive){ManualInput input=MANUAL.get(locomotive.getUUID());if(input==null||(!input.forward&&!input.backward))return;Vec3 forward=direction(locomotive);double signed=locomotive.getDeltaMovement().dot(forward);double target=(input.forward?1:-1)*targetSpeed(locomotive);double acceleration=0.015;double next=signed<target?Math.min(target,signed+acceleration):Math.max(target,signed-acceleration);if(Math.abs(next)<0.005)next=0;locomotive.setDeltaMovement(forward.scale(next));}

    private static void tickCarOnlyGroup(ServerLevel level, UUID trainId, List<Entity> cars, Set<UUID> active) {
        Entity leader = cars.stream().min(Comparator.comparingInt(entity ->
                ((LinkedTrainCar) entity).trainLink().carIndex())).orElse(null);
        if (leader == null) return;
        active.add(trainId);
        TrackTrail trail = TRAILS.computeIfAbsent(trainId, ignored -> new TrackTrail(140.0D));
        LogicalMotion motion = LOGICAL_MOTIONS.computeIfAbsent(trainId, ignored -> new LogicalMotion());
        int input = CAR_ONLY_INPUT.getOrDefault(trainId, 0);
        double limit = SpeedRules.blocksPerTick(SpeedRules.classLimit(
                ((LinkedTrainCar) leader).trainLink().trainClass()));
        double speed = motion.speed.approach(input * limit, 0.015D);
        if (Math.abs(speed) > 0.0001D && leader instanceof AbstractMinecart minecart) {
            if (motion.cursor == null) motion.cursor = RailMotionCursor.create(level, minecart, direction(leader));
            RailMotionCursor.Pose pose = motion.cursor == null ? null : motion.cursor.advance(level, speed);
            if (pose == null || !level.hasChunkAt(BlockPos.containing(pose.position()))) {
                motion.speed.stop();
                CAR_ONLY_INPUT.put(trainId, 0);
                notifyDiagnostic(level, trainId, pose == null ? "cursor_missing" : "chunk_unloaded",
                        motion.speed.speed(), leader.blockPosition());
            } else {
                leader.setPos(pose.position());
                leader.setYRot(pose.yaw());
                leader.setDeltaMovement(Vec3.ZERO);
                trail.record(pose.position().x, pose.position().y, pose.position().z);
                notifyDiagnostic(level, trainId, "moving", speed, BlockPos.containing(pose.position()));
            }
        } else {
            leader.setDeltaMovement(Vec3.ZERO);
            trail.record(leader.getX(), leader.getY(), leader.getZ());
        }
        cars.stream().filter(entity -> entity != leader)
                .sorted(Comparator.comparingInt(entity -> ((LinkedTrainCar) entity).trainLink().carIndex()))
                .forEach(entity -> {
                    int index = Math.max(1, ((LinkedTrainCar) entity).trainLink().carIndex());
                    TrackTrail.Point target = trail.pointBehind(index * CAR_SPACING);
                    if (target == null) {
                        Vec3 anchor = FormationGeometry.anchor(leader.position(), entity.position(), index * CAR_SPACING);
                        entity.setPos(anchor);
                    } else entity.setPos(target.x(), target.y(), target.z());
                    if (entity instanceof CargoCarEntity cargo) {
                        cargo.beginAuthorizedPush();
                        try { entity.setDeltaMovement(leader.getDeltaMovement()); }
                        finally { cargo.endAuthorizedPush(); }
                    } else entity.setDeltaMovement(leader.getDeltaMovement());
                });
    }

    private static Vec3 direction(Entity entity) {
        double radians = Math.toRadians(entity.getYRot());
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }

    private static void maintainAutomaticSpeed(LocomotiveEntity locomotive) {
        if (!locomotive.trainLink().automaticRunning() || PAUSED.containsKey(locomotive.getUUID())) return;
        double target = targetSpeed(locomotive);
        Vec3 velocity = locomotive.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.horizontalDistanceSqr());
        Vec3 travelDirection = horizontal > 0.001D
                ? new Vec3(velocity.x / horizontal, 0, velocity.z / horizontal)
                : railDirection(locomotive);
        if (travelDirection == null) {
            locomotive.setDeltaMovement(Vec3.ZERO);
            locomotive.trainLink().setAutomaticRunning(false);
            return;
        }
        double nextSpeed = Math.min(target, horizontal + 0.015D);
        locomotive.setDeltaMovement(travelDirection.x * nextSpeed, velocity.y, travelDirection.z * nextSpeed);
    }

    private static boolean nextTrainChunkLoaded(ServerLevel level, LocomotiveEntity locomotive) {
        Vec3 velocity = locomotive.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.horizontalDistanceSqr());
        Vec3 travelDirection = horizontal > 0.001D
                ? new Vec3(velocity.x / horizontal, 0, velocity.z / horizontal)
                : direction(locomotive);
        double lookAhead = Math.max(1.0D, Math.max(horizontal, targetSpeed(locomotive)) + 0.5D);
        Vec3 next = locomotive.position().add(travelDirection.scale(lookAhead));
        return level.hasChunkAt(BlockPos.containing(next));
    }

    private static boolean nextRouteChunkLoaded(ServerLevel level, RailMotionCursor cursor) {
        if (cursor == null) return true;
        return level.hasChunkAt(cursor.currentRail()) && level.hasChunkAt(cursor.nextRail());
    }

    public static void noteInteraction(LinkedTrainCar car) {
        PausedTrain paused = PAUSED.get(car.trainLink().locomotiveId());
        if (paused != null) PAUSED.put(car.trainLink().locomotiveId(), new PausedTrain(
                ticks + AutoResumeTimer.DEFAULT_DELAY_TICKS, paused.resumeVelocity()));
    }

    public static void resetFormation(UUID trainId, List<Entity> cars) {
        TRAILS.remove(trainId);
        LOGICAL_MOTIONS.remove(trainId);
        ROUTES.remove(trainId);
        cars.stream().filter(LocomotiveEntity.class::isInstance).findFirst().ifPresent(locomotive -> {
            MANUAL.remove(locomotive.getUUID());
            PAUSED.remove(locomotive.getUUID());
        });
        cars.forEach(car -> {
            car.setDeltaMovement(Vec3.ZERO);
            if (car instanceof LinkedTrainCar linked) {
                linked.trainLink().setAutomaticRunning(false);
                linked.trainLink().setParked(true);
            }
            if (car instanceof LogicallyControlledVehicle vehicle)
                vehicle.fabricTrain$setLogicallyControlled(cars.stream().anyMatch(LocomotiveEntity.class::isInstance)
                        || cars.size() > 1);
        });
        TrackTrail trail = new TrackTrail(140.0D);
        cars.stream()
                .sorted(Comparator.comparingInt(entity ->
                        ((LinkedTrainCar) entity).trainLink().carIndex()).reversed())
                .forEach(entity -> trail.record(entity.getX(), entity.getY(), entity.getZ()));
        TRAILS.put(trainId, trail);
    }

    public static void detachLine(MinecraftServer server, long lineId) {
        for (ServerLevel level : server.getAllLevels()) {
            RailwaySavedData data = RailwayDataManager.state(level);
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LinkedTrainCar linked) || linked.trainLink().lineId() != lineId) continue;
                UUID trainId = linked.trainLink().trainId();
                releaseTrainSwitches(trainId);
                PAUSED.remove(linked.trainLink().locomotiveId());
                linked.trainLink().detachFromLine();
                data.unregisterActiveTrain(trainId);
            }
        }
    }

    public static void handleDeletedLine(MinecraftServer server, long lineId, long recoveryDepotId) {
        for (ServerLevel level : server.getAllLevels()) {
            RailwaySavedData data = RailwayDataManager.state(level);
            Map<UUID, List<Entity>> trains = new HashMap<>();
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LinkedTrainCar linked && linked.trainLink().lineId() == lineId
                        && linked.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC) {
                    trains.computeIfAbsent(linked.trainLink().trainId(), ignored -> new ArrayList<>()).add(entity);
                }
            }
            RailwayDevice recovery = data.device(recoveryDepotId);
            for (Map.Entry<UUID, List<Entity>> entry : trains.entrySet()) {
                boolean recovered = false;
                if (recovery != null && recovery.dimension().equals(RailwayDataManager.dimension(level))) {
                    BlockPos pos = new BlockPos(recovery.x(), recovery.y(), recovery.z());
                    if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof TrainDepotBlockEntity depot)
                        recovered = DepotRecoveryService.recover(entry.getValue(), depot);
                }
                if (!recovered) entry.getValue().forEach(entity -> {
                    entity.setDeltaMovement(Vec3.ZERO);
                    if (entity instanceof LinkedTrainCar linked) linked.trainLink().setAutomaticRunning(false);
                });
                releaseTrainSwitches(entry.getKey());
                data.unregisterActiveTrain(entry.getKey());
            }
        }
    }

    private static boolean prepareNextSwitch(ServerLevel level, LocomotiveEntity locomotive, List<Entity> cars) {
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayLine line = data.line(locomotive.trainLink().lineId());
        if (line == null || line.nodeIds().isEmpty()) return true;
        RouteProgress progress = routeProgress(locomotive);
        int nextIndex = Math.min(progress.nextNodeIndex, line.nodeIds().size() - 1);
        RailwayDevice next = data.device(line.nodeIds().get(nextIndex));
        if (next == null || next.type() != DeviceType.SWITCH
                || !next.dimension().equals(RailwayDataManager.dimension(level))) return true;
        BlockPos pos = new BlockPos(next.x(), next.y(), next.z());
        UUID trainId = locomotive.trainLink().trainId();
        double reservation = RealtimeSwitchTraffic.reservationDistance(currentLogicalSpeed(trainId));
        if (locomotive.distanceToSqr(Vec3.atCenterOf(pos)) > reservation * reservation) return true;
        if (!hasSwitchRight(level, next.id(), trainId)) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            notifyDiagnostic(level, trainId, "waiting_switch", currentLogicalSpeed(trainId),
                    locomotive.blockPosition());
            return false;
        }
        if (level.hasChunkAt(pos) && level.getBlockState(pos).hasProperty(RailBlock.SHAPE)) {
            LogicalMotion motion = LOGICAL_MOTIONS.get(trainId);
            var shape = motion == null || motion.cursor == null
                    ? Optional.<RailShape>empty() : motion.cursor.routeShapeAt(pos);
            if (shape.isEmpty() || !shapeConnected(level, pos, shape.get())) {
                cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
                return false;
            }
        }
        return true;
    }

    private static net.minecraft.core.Direction travelDirection(LocomotiveEntity locomotive) {
        return travelDirection(locomotive,false);
    }

    private static net.minecraft.core.Direction travelDirection(LocomotiveEntity locomotive,boolean reverse) {
        LogicalMotion motion=LOGICAL_MOTIONS.get(locomotive.trainLink().trainId());
        if(motion!=null&&motion.cursor!=null){
            var cursorDirection=motion.cursor.travelDirection(!reverse);
            if(cursorDirection.isPresent())return cursorDirection.get();
        }
        Vec3 movement = direction(locomotive);
        if(reverse)movement=movement.scale(-1);
        return Math.abs(movement.x) >= Math.abs(movement.z)
                ? (movement.x >= 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST)
                : (movement.z >= 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH);
    }

    private static boolean prepareManualSwitch(ServerLevel level, LocomotiveEntity locomotive, List<Entity> cars) {
        LogicalMotion motion=LOGICAL_MOTIONS.get(locomotive.trainLink().trainId());
        ManualInput input=MANUAL.get(locomotive.getUUID());
        boolean reverse=motion!=null&&motion.speed.speed()<-0.0001D
                ||(motion!=null&&Math.abs(motion.speed.speed())<0.0001D&&input!=null&&input.backward());
        net.minecraft.core.Direction travel = travelDirection(locomotive,reverse);
        BlockPos railPos = locomotive.getCurrentBlockPosOrRailBelow();
        RailwaySavedData data = RailwayDataManager.state(level);
        UUID trainId = locomotive.trainLink().trainId();
        for (int distance = 0; distance <= 2; distance++) {
            BlockPos pos = switchAt(level,railPos.relative(travel, distance));
            if (pos==null) continue;
            RailwayDevice device = data.find(RailwayDataManager.dimension(level), pos.getX(), pos.getY(), pos.getZ());
            if (device != null) {
                if (!hasSwitchRight(level, device.id(), trainId)) {
                    notifyDiagnostic(level, trainId, "waiting_switch", currentLogicalSpeed(trainId),
                            locomotive.blockPosition());
                    return false;
                }
                var state = level.getBlockState(pos);
                var shape = SwitchRailRouting.shapeForFixedChoice(
                        SwitchRailRouting.entryFor(device,state),
                        travel, device.switchShapeOrdinal());
                if (shape.isEmpty() || !shapeConnected(level, pos, shape.get())) {
                    return false;
                }
                level.setBlock(pos, state.setValue(RailBlock.SHAPE, shape.get()), 3);
            }
            return true;
        }
        return true;
    }

    private static void releaseTrainSwitches(UUID trainId) {
        REALTIME_SWITCH_RIGHTS.entrySet().removeIf(entry -> entry.getValue().equals(trainId));
        RouteProgress progress = ROUTES.get(trainId);
        if (progress != null) progress.lockedSwitchId = 0L;
    }

    private static void escrowUnloadedDestinations(ServerLevel level, LocomotiveEntity locomotive, List<Entity> cars) {
        RailwaySavedData data = RailwayDataManager.state(level);
        for (Entity entity : cars) {
            if (!(entity instanceof CargoCarEntity cargo) || cargo.isEmpty()) continue;
            long destinationStationId = CargoUnloadRules.effectiveDestination(cargo.destinationStationId(),
                    locomotive.trainLink().destinationStationId());
            if (destinationStationId <= 0L) continue;
            RailwayDevice station = data.device(destinationStationId);
            if (station == null || !station.dimension().equals(RailwayDataManager.dimension(level))) continue;
            BlockPos destination = new BlockPos(station.x(), station.y(), station.z());
            if (level.hasChunkAt(destination)) continue;
            double blocksPerTick = Math.max(0.001D, targetSpeed(locomotive));
            long travelTicks = Math.max(20L, (long) Math.ceil(Math.sqrt(locomotive.distanceToSqr(Vec3.atCenterOf(destination))) / blocksPerTick));
            CargoLedgerService.escrow(level, cargo, station.id(), level.getGameTime() + travelTicks);
        }
    }

    private static double targetSpeed(LocomotiveEntity locomotive) {
        double lineLimit = Double.MAX_VALUE;
        if (locomotive.level() instanceof ServerLevel level) {
            RailwayLine line = RailwayDataManager.state(level).line(locomotive.trainLink().lineId());
            if (line != null) lineLimit = line.speedLimitKmh();
        }
        double classLimit = locomotive.level() instanceof ServerLevel level
                ? RailwayDataManager.state(level).classSpeedLimit(locomotive.trainLink().trainClass())
                : SpeedRules.classLimit(locomotive.trainLink().trainClass());
        return SpeedRules.blocksPerTick(SpeedRules.actualSpeed(lineLimit, classLimit,
                locomotive.trainLink().configuredSpeedLimitKmh()));
    }

    private static void enforceSpeedLimit(ServerLevel level, LocomotiveEntity locomotive) {
        Vec3 velocity = locomotive.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.horizontalDistanceSqr());
        double maximum = targetSpeed(locomotive);
        if (horizontal > maximum && horizontal > 0) {
            locomotive.setDeltaMovement(velocity.x * maximum / horizontal, velocity.y, velocity.z * maximum / horizontal);
        }
    }

    private static boolean tryStartupRecovery(ServerLevel level, LocomotiveEntity locomotive, List<Entity> cars) {
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayLine line = data.line(locomotive.trainLink().lineId());
        if (line == null) {
            locomotive.trainLink().markStartupRecovered();
            return false;
        }
        if (locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC) {
            data.registerActiveTrain(locomotive.trainLink().trainId(), line.id());
            boolean pathRestored = restoreLoadedAutomaticPath(level, locomotive, line, data);
            RailwayLine restoredLine = data.line(line.id());
            boolean resume = StartupAutomaticResumeRules.shouldResume(
                    locomotive.trainLink().controlMode(),
                    restoredLine == null ? line.status() : restoredLine.status(), pathRestored);
            locomotive.trainLink().setAutomaticRunning(resume);
            if (!resume) cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            if (pathRestored) locomotive.trainLink().markStartupRecovered();
            return true;
        }
        data.registerActiveTrain(locomotive.trainLink().trainId(), line.id());
        RailwayDevice depotDevice = data.device(data.recoveryDepotId(line.id()));
        if (depotDevice == null || !depotDevice.dimension().equals(RailwayDataManager.dimension(level))) return false;
        BlockPos depotPos = new BlockPos(depotDevice.x(), depotDevice.y(), depotDevice.z());
        if (!level.hasChunkAt(depotPos)) {
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return true;
        }
        if (level.getBlockEntity(depotPos) instanceof TrainDepotBlockEntity depot) {
            if (DepotRecoveryService.recover(cars, depot)) return true;
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            return true;
        }
        return false;
    }

    private static boolean restoreLoadedAutomaticPath(ServerLevel level, LocomotiveEntity locomotive,
                                                       RailwayLine line, RailwaySavedData data) {
        List<TrackPoint> linePath = RuntimePathCache.get(line.id());
        if (linePath.isEmpty()) {
            if (!WorldLineValidator.validate(level, line, data)) return false;
            linePath = RuntimePathCache.get(line.id());
            if (linePath.isEmpty()) return false;
        }
        WorldLineValidator.DispatchAccess access = WorldLineValidator.validateDispatchAccess(level, line, data);
        ArrayList<TrackPoint> complete = new ArrayList<>();
        if (!access.path().isEmpty()) complete.addAll(access.path());
        if (!complete.isEmpty() && complete.getLast().equals(linePath.getFirst())) complete.removeLast();
        int loopStartIndex = complete.size();
        complete.addAll(linePath);
        if (complete.size() < 2) return false;
        boolean loop = LineLoopRules.isLoop(line.nodeIds(), id -> {
            RailwayDevice device = data.device(id);
            return device == null ? null : device.type();
        });
        registerAutomaticPath(locomotive.trainLink().trainId(), complete, loop, loopStartIndex);
        return true;
    }

    private static void processLineNode(ServerLevel level, LocomotiveEntity locomotive, List<Entity> cars) {
        long lineId = locomotive.trainLink().lineId();
        if (lineId <= 0) return;
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayLine line = data.line(lineId);
        if (line == null || line.nodeIds().size() < 2) return;
        BlockPos railPos = locomotive.getCurrentBlockPosOrRailBelow();
        RouteProgress progress = routeProgress(locomotive);
        int expectedIndex = Math.min(progress.nextNodeIndex, line.nodeIds().size() - 1);
        RailwayDevice expected = data.device(line.nodeIds().get(expectedIndex));
        RailwayDevice current = null;
        if (expected != null && expected.type() == DeviceType.STATION) {
            StopTrack stop = WorldLineValidator.resolveStationStopTrack(level, line, expectedIndex, expected, data);
            if (stop != null && railPos.getX() == stop.x() && railPos.getY() == stop.y() && railPos.getZ() == stop.z())
                current = expected;
        } else {
            current = data.find(RailwayDataManager.dimension(level), railPos.getX(), railPos.getY(), railPos.getZ());
            if (current == null) current = data.find(RailwayDataManager.dimension(level), railPos.getX(), railPos.getY() - 1, railPos.getZ());
        }
        if (current == null) {
            progress.lastDeviceId = 0;
            syncRouteProgress(locomotive, progress);
            return;
        }
        final RailwayDevice currentNode = current;
        if (current.id() == progress.lastDeviceId) return;
        if (line.nodeIds().get(expectedIndex) != current.id()) return;
        progress.lastDeviceId = current.id();
        if (current.type() == DeviceType.STATION) {
            Vec3 velocity = locomotive.getDeltaMovement();
            if (velocity.horizontalDistanceSqr() < 0.0001D) velocity = direction(locomotive).scale(targetSpeed(locomotive));
            StopTrack stop = WorldLineValidator.resolveStationStopTrack(level, line, expectedIndex, current, data);
            if (stop != null) hardStopAtStation(locomotive, cars, stop);
            else cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            locomotive.trainLink().setAutomaticRunning(false);
            if (locomotive.trainLink().controlMode() == ControlMode.ROUTE_AUTOMATIC) {
                int dwell = StationDwellRules.automaticSeconds(current, locomotive.trainLink().trainClass());
                PAUSED.put(locomotive.getUUID(), new PausedTrain(
                        EMERGENCY_STATIONS.contains(current.id()) ? ticks + 2 : ticks + (long) dwell * 20L, velocity));
            } else {
                PAUSED.remove(locomotive.getUUID());
            }
            broadcastStationAnnouncement(level, currentNode);
            cars.stream().filter(CargoCarEntity.class::isInstance).map(CargoCarEntity.class::cast)
                    .filter(cargo -> CargoUnloadRules.shouldUnloadAt(currentNode.id(), cargo.destinationStationId(),
                            locomotive.trainLink().destinationStationId()))
                    .forEach(cargo -> {
                        CargoTransferService.unloadAtStation(level,
                                new BlockPos(currentNode.x(), currentNode.y(), currentNode.z()), cargo);
                    });
            progress.nextNodeIndex++;
        } else if (current.type() == DeviceType.SWITCH) {
            if (hasSwitchRight(level, current.id(), locomotive.trainLink().trainId())) {
                progress.lockedSwitchId = 0L;
                progress.nextNodeIndex++;
            } else {
                cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
                notifyDiagnostic(level, locomotive.trainLink().trainId(), "waiting_switch",
                        currentLogicalSpeed(locomotive.trainLink().trainId()), locomotive.blockPosition());
            }
        } else if (current.type() == DeviceType.DEPOT && expectedIndex == line.nodeIds().size() - 1) {
            BlockPos depotPos = new BlockPos(current.x(), current.y(), current.z());
            if (level.getBlockEntity(depotPos) instanceof TrainDepotBlockEntity depot
                    && DepotRecoveryService.recover(cars, depot)) {
                releaseTrainSwitches(locomotive.trainLink().trainId());
                return;
            }
            cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
            locomotive.trainLink().setAutomaticRunning(false);
        } else {
            progress.nextNodeIndex++;
        }
        if (progress.nextNodeIndex >= line.nodeIds().size()) {
            progress.nextNodeIndex = isLoop(line, data) ? 1 : line.nodeIds().size() - 1;
        }
        syncRouteProgress(locomotive, progress);
    }

    private static void processManualStationAnnouncement(ServerLevel level, LocomotiveEntity locomotive) {
        BlockPos railPos = locomotive.getCurrentBlockPosOrRailBelow();
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayDevice current = data.find(RailwayDataManager.dimension(level),
                railPos.getX(), railPos.getY(), railPos.getZ());
        if (current == null) current = data.find(RailwayDataManager.dimension(level),
                railPos.getX(), railPos.getY() - 1, railPos.getZ());
        RouteProgress progress = routeProgress(locomotive);
        if (current == null || current.type() != DeviceType.STATION) {
            if (progress.lastDeviceId != 0L) {
                progress.lastDeviceId = 0L;
                syncRouteProgress(locomotive, progress);
            }
            return;
        }
        if (current.id() == progress.lastDeviceId) return;
        progress.lastDeviceId = current.id();
        syncRouteProgress(locomotive, progress);
        broadcastStationAnnouncement(level, current);
    }

    private static void broadcastStationAnnouncement(ServerLevel level, RailwayDevice station) {
        if (station.announcement().isBlank()) return;
        level.players().stream()
                .filter(player -> player.distanceToSqr(station.x() + .5, station.y() + .5,
                        station.z() + .5) <= 1024)
                .forEach(player -> player.displayClientMessage(Component.literal(station.announcement()), true));
    }

    private static void hardStopAtStation(LocomotiveEntity locomotive, List<Entity> cars, StopTrack stop) {
        StationStopAlignment.Position aligned = StationStopAlignment.position(stop);
        BlockPos stopPos = new BlockPos(stop.x(), stop.y(), stop.z());
        LogicalMotion motion = LOGICAL_MOTIONS.computeIfAbsent(locomotive.trainLink().trainId(),
                ignored -> new LogicalMotion());
        motion.speed.stop();
        if (motion.cursor != null) motion.cursor.snapTo(stopPos);
        locomotive.setPos(aligned.x(), aligned.y(), aligned.z());
        cars.forEach(car -> car.setDeltaMovement(Vec3.ZERO));
        TRAILS.computeIfAbsent(locomotive.trainLink().trainId(), ignored -> new TrackTrail(140.0D))
                .record(aligned.x(), aligned.y(), aligned.z());
    }

    private static RouteProgress routeProgress(LocomotiveEntity locomotive) {
        return ROUTES.computeIfAbsent(locomotive.trainLink().trainId(), ignored -> new RouteProgress(
                locomotive.trainLink().nextNodeIndex(), locomotive.trainLink().lastDeviceId(), locomotive.trainLink().lockedSwitchId()));
    }

    private static void syncRouteProgress(LocomotiveEntity locomotive, RouteProgress progress) {
        locomotive.trainLink().updateRouteProgress(progress.nextNodeIndex, progress.lastDeviceId, progress.lockedSwitchId);
    }

    private static boolean isLoop(RailwayLine line, RailwaySavedData data) {
        return cn.quit5700.fabrictrain.route.LineLoopRules.isLoop(line.nodeIds(), id -> {
            RailwayDevice device = data.device(id);
            return device == null ? null : device.type();
        });
    }

    private static boolean shapeConnected(ServerLevel level, BlockPos pos, RailShape shape) {
        var exits = AbstractMinecart.exits(shape);
        return hasRailAtAnyHeight(level,pos.offset(exits.getFirst()))
                && hasRailAtAnyHeight(level,pos.offset(exits.getSecond()));
    }

    private static boolean hasRailAtAnyHeight(ServerLevel level,BlockPos pos){
        return RailHeightOffsets.candidates(pos).stream().anyMatch(candidate->BaseRailBlock.isRail(level,candidate));
    }

    private static BlockPos switchAt(ServerLevel level,BlockPos pos){
        return RailHeightOffsets.candidates(pos).stream().filter(candidate->level.getBlockState(candidate).is(ModBlocks.SWITCH_RAIL))
                .findFirst().orElse(null);
    }

    private static void follow(ServerLevel level, LocomotiveEntity locomotive, Entity entity, TrackTrail trail) {
        int index = ((LinkedTrainCar) entity).trainLink().carIndex();
        if (index <= 0) return;
        TrackTrail.Point target = trail.pointBehind(index * CAR_SPACING);
        if (target == null) {
            entity.setDeltaMovement(Vec3.ZERO);
            return;
        }
        BlockPos targetPos = BlockPos.containing(target.x(), target.y(), target.z());
        if (!level.hasChunkAt(targetPos)) {
            entity.setDeltaMovement(Vec3.ZERO);
            return;
        }
        entity.setPos(target.x(), target.y(), target.z());
        TrackTrail.Point ahead = trail.pointBehind(Math.max(0, index * CAR_SPACING - 0.2D));
        if (ahead != null) {
            double dx = ahead.x() - target.x();
            double dz = ahead.z() - target.z();
            if (dx * dx + dz * dz > 0.000001D)
                entity.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        }
        entity.setDeltaMovement(Vec3.ZERO);
    }

    public static void clear() {
        TRAILS.clear();
        PAUSED.clear();
        ROUTES.clear();
        REALTIME_SWITCH_RIGHTS.clear();
        EMERGENCY_STATIONS.clear();
        MANUAL.clear();
        LOGICAL_MOTIONS.clear();
        AUTOMATIC_PATHS.clear();
        CAR_ONLY_INPUT.clear();
        DIAGNOSTIC_DRIVERS.clear();
        LAST_DIAGNOSTIC_REASON.clear();
        OFFLINE_REMOVALS.clear();
        RuntimePathCache.clear();
        PENDING_DESTROYED_TRAINS.clear();
        ticks = 0;
    }

    private static Entity findEntity(MinecraftServer server, UUID entityId) {
        if (server == null || entityId == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    private record PausedTrain(long resumeAtTick, Vec3 resumeVelocity) {}
    private record ManualInput(boolean forward,boolean backward,long changedAt){}
    private record SwitchTrafficKey(String dimension, long deviceId) {}
    private record AutomaticPath(List<cn.quit5700.fabrictrain.route.TrackPoint> points, boolean loop,
                                 int loopStartIndex, int linePathStartIndex) {}
    private static final class LogicalMotion {
        private final LogicalSpeedState speed = new LogicalSpeedState();
        private RailMotionCursor cursor;
    }
    private static final class RouteProgress {
        private int nextNodeIndex;
        private long lastDeviceId;
        private long lockedSwitchId;
        private RouteProgress(int nextNodeIndex, long lastDeviceId, long lockedSwitchId) {
            this.nextNodeIndex = Math.max(1, nextNodeIndex);
            this.lastDeviceId = lastDeviceId;
            this.lockedSwitchId = lockedSwitchId;
        }
    }
}
