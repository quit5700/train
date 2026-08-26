package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.*;
import cn.quit5700.fabrictrain.registry.ModEntities;
import cn.quit5700.fabrictrain.registry.ModItems;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.CarType;
import cn.quit5700.fabrictrain.train.TrainClass;
import cn.quit5700.fabrictrain.train.SpeedRules;
import cn.quit5700.fabrictrain.train.ControlMode;
import cn.quit5700.fabrictrain.train.FormationDirection;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import cn.quit5700.fabrictrain.train.TrainRegistryService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Container;
import net.minecraft.core.Direction;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import cn.quit5700.fabrictrain.route.TrackPoint;
import cn.quit5700.fabrictrain.route.RuntimePathCache;
import cn.quit5700.fabrictrain.route.LineLoopRules;

import java.util.*;

public final class DepotDispatchService {
    private static final Map<Long, Long> LAST_DISPATCH_TICK = new HashMap<>();
    private static long ticks;

    private DepotDispatchService() {}

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % 20 != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            RailwaySavedData data = RailwayDataManager.state(level);
            for (RailwayDevice device : data.devices().values()) {
                if (device.type() != DeviceType.DEPOT
                        || !device.dimension().equals(RailwayDataManager.dimension(level))) continue;
                BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
                if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof TrainDepotBlockEntity depot)
                    absorbConnectedVehicles(level, pos, depot);
            }
            for (RailwayLine line : data.lines().values()) {
                if (line.status() != LineStatus.RUNNING) continue;
                long last = LAST_DISPATCH_TICK.getOrDefault(line.id(), Long.MIN_VALUE / 2);
                RailwayDevice depot = data.device(line.depotId());
                if (depot == null || !depot.dimension().equals(RailwayDataManager.dimension(level))) continue;
                BlockPos depotPos = new BlockPos(depot.x(), depot.y(), depot.z());
                if (!level.hasChunkAt(depotPos) || !(level.getBlockEntity(depotPos) instanceof TrainDepotBlockEntity inventory)) continue;
                if (!AutomaticDispatchSchedule.shouldAttempt(inventory.automaticDispatchEnabled(),
                        inventory.selectedLineId() == line.id(), data.activeTrainCount(line.id()),
                        inventory.maximumTrains(), ticks, last, inventory.dispatchIntervalSeconds())) continue;
                DepotDispatchResult result = dispatch(level, depotPos, line, inventory,
                        DepotDispatchConfig.saved(inventory), !inventory.creativeAutomaticDispatch(),
                        ControlMode.ROUTE_AUTOMATIC).result();
                inventory.setLastAutomaticDispatchResult(result);
                if (result.succeeded()) LAST_DISPATCH_TICK.put(line.id(), ticks);
            }
        }
    }

    public static int absorbConnectedVehicles(ServerLevel level, BlockPos pos, TrainDepotBlockEntity depot) {
        int moved = 0;
        for (Direction direction : Direction.values()) {
            if (!(level.getBlockEntity(pos.relative(direction)) instanceof Container source) || source == depot) continue;
            for (int slot=0;slot<source.getContainerSize();slot++) {
                ItemStack stack=source.getItem(slot);
                if (stack.isEmpty() || !(stack.is(ModItems.LOCOMOTIVE)
                        || stack.is(ModItems.PASSENGER_CAR) || stack.is(ModItems.CARGO_CAR))) continue;
                int before=stack.getCount();
                insertIntoDepot(depot,stack);
                if(before!=stack.getCount()){moved+=before-stack.getCount();source.setItem(slot,stack);source.setChanged();}
            }
        }
        if(moved>0)depot.setChanged();
        return moved;
    }

    private static void insertIntoDepot(TrainDepotBlockEntity depot,ItemStack source) {
        for(int slot=0;slot<depot.getContainerSize()&&!source.isEmpty();slot++){
            ItemStack target=depot.getItem(slot);
            if(target.isEmpty()||!ItemStack.isSameItemSameComponents(target,source))continue;
            int amount=Math.min(depot.getMaxStackSize(target)-target.getCount(),source.getCount());
            if(amount>0){target.grow(amount);source.shrink(amount);depot.setItem(slot,target);}
        }
        for(int slot=0;slot<depot.getContainerSize()&&!source.isEmpty();slot++){
            if(!depot.getItem(slot).isEmpty()||!depot.canPlaceItem(slot,source))continue;
            int amount=Math.min(depot.getMaxStackSize(source),source.getCount());
            depot.setItem(slot,source.copyWithCount(amount));source.shrink(amount);
        }
    }

    public static boolean dispatchNow(ServerLevel level, BlockPos depotPos, RailwayLine line,
                                      TrainDepotBlockEntity inventory, boolean consumeItems) {
        return dispatchNowDetailed(level, depotPos, line, inventory, consumeItems).succeeded();
    }

    public static DepotDispatchResult dispatchNowDetailed(ServerLevel level, BlockPos depotPos, RailwayLine line,
                                                          TrainDepotBlockEntity inventory, boolean consumeItems) {
        return dispatch(level, depotPos, line, inventory, DepotDispatchConfig.saved(inventory), consumeItems,
                ControlMode.ROUTE_AUTOMATIC).result();
    }

    public static DepotDispatchResult dispatchConfigured(ServerLevel level, BlockPos depotPos, RailwayLine line,
                                                         TrainDepotBlockEntity inventory, DepotDispatchConfig config,
                                                         boolean consumeItems, ControlMode controlMode) {
        return dispatch(level, depotPos, line, inventory, config, consumeItems, controlMode).result();
    }

    public static DispatchOutcome dispatchConfiguredOutcome(ServerLevel level, BlockPos depotPos, RailwayLine line,
                                                            TrainDepotBlockEntity inventory, DepotDispatchConfig config,
                                                            boolean consumeItems, ControlMode controlMode) {
        return dispatch(level, depotPos, line, inventory, config, consumeItems, controlMode);
    }

    public static void resetAutomaticTimer(long lineId) { LAST_DISPATCH_TICK.put(lineId, ticks); }

    private static DispatchOutcome dispatch(ServerLevel level, BlockPos depotPos, RailwayLine line,
                                            TrainDepotBlockEntity inventory, DepotDispatchConfig config, boolean consumeItems,
                                            ControlMode controlMode) {
        if(config.template().cars().isEmpty())return DispatchOutcome.failure(DepotDispatchResult.EMPTY_TEMPLATE);
        if(controlMode==ControlMode.ROUTE_AUTOMATIC&&line==null)return DispatchOutcome.failure(DepotDispatchResult.DIRECTION_UNRESOLVED);
        BlockPos railPos = depotPos.above();
        if (!BaseRailBlock.isRail(level, railPos)) return DispatchOutcome.failure(DepotDispatchResult.NO_RAIL);
        RailPowerService.refreshNow(level);
        if (!RailPowerService.isPowered(level, railPos)) return DispatchOutcome.failure(DepotDispatchResult.UNPOWERED);
        Vec3 forward;
        List<TrackPoint> automaticPath = List.of();
        boolean loop = false;
        int loopStartIndex = 0;
        if (line != null) {
            RailwaySavedData data = RailwayDataManager.state(level);
            if (RuntimePathCache.get(line.id()).isEmpty() && !WorldLineValidator.validate(level, line, data))
                return DispatchOutcome.failure(DepotDispatchResult.DIRECTION_UNRESOLVED);
            WorldLineValidator.DispatchAccess access = WorldLineValidator.validateDispatchAccess(
                    level, depotPos, line, data);
            if (access.status() == WorldLineValidator.DispatchAccessStatus.UNPOWERED)
                return DispatchOutcome.failure(DepotDispatchResult.UNPOWERED);
            if (access.status() != WorldLineValidator.DispatchAccessStatus.READY)
                return DispatchOutcome.failure(DepotDispatchResult.DIRECTION_UNRESOLVED);
            TrackPoint start = access.path().get(0);
            TrackPoint next = access.path().get(1);
            forward = new Vec3(next.x() - start.x(), next.y() - start.y(), next.z() - start.z()).normalize();
            ArrayList<TrackPoint> combined = new ArrayList<>(access.path());
            List<TrackPoint> linePath = RuntimePathCache.get(line.id());
            if (!combined.isEmpty() && !linePath.isEmpty() && combined.getLast().equals(linePath.getFirst()))
                combined.removeLast();
            loopStartIndex = combined.size();
            combined.addAll(linePath);
            automaticPath = List.copyOf(combined);
            loop = LineLoopRules.isLoop(line.nodeIds(), id -> {
                RailwayDevice device = data.device(id);
                return device == null ? null : device.type();
            });
        } else {
            RailShape railShape = level.getBlockState(railPos).getValue(RailBlock.SHAPE);
            var exits = AbstractMinecart.exits(railShape);
            boolean firstHasNode = reachesNode(level, railPos, exits.getFirst());
            boolean secondHasNode = reachesNode(level, railPos, exits.getSecond());
            int frontIndex = DepotDirectionRules.frontIndex(firstHasNode, secondHasNode);
            if (frontIndex < 0) return DispatchOutcome.failure(DepotDispatchResult.DIRECTION_UNRESOLVED);
            Vec3i exit = frontIndex == 0 ? exits.getFirst() : exits.getSecond();
            forward = new Vec3(exit.getX(), exit.getY(), exit.getZ()).normalize();
        }
        List<Container> containers = connectedContainers(level, depotPos, inventory);
        if (consumeItems && (!has(containers, ModItems.LOCOMOTIVE, 1)
                || !has(containers, ModItems.PASSENGER_CAR,
                (int) config.template().cars().stream().filter(type -> type == CarType.PASSENGER).count())
                || !has(containers, ModItems.CARGO_CAR,
                (int) config.template().cars().stream().filter(type -> type == CarType.CARGO).count()))) {
            return DispatchOutcome.failure(DepotDispatchResult.MISSING_VEHICLE_ITEMS);
        }

        UUID trainId = UUID.randomUUID();
        Vec3 center = Vec3.atCenterOf(railPos);
        List<Entity> spawned = new ArrayList<>();
        LocomotiveEntity locomotive = ModEntities.LOCOMOTIVE.create(level, EntitySpawnReason.TRIGGERED);
        if (locomotive == null) return DispatchOutcome.failure(DepotDispatchResult.SPAWN_FAILED);
        long lineId=controlMode==ControlMode.ROUTE_AUTOMATIC&&line!=null?line.id():0L;
        locomotive.trainLink().assign(trainId, locomotive.getUUID(), 0, lineId);
        locomotive.trainLink().setDepotGenerated(true);
        locomotive.trainLink().setControlMode(controlMode);
        TrainClass trainClass = config.effectiveClass();
        locomotive.trainLink().setTrainClass(trainClass);
        locomotive.trainLink().setConfiguredSpeedLimitKmh(config.speedLimitKmh(trainClass));
        Direction formationForward=FormationDirection.fromPositions(center.x+forward.x,center.z+forward.z,
                center.x,center.z).orElse(Direction.NORTH);
        locomotive.trainLink().setFormationForward(formationForward);
        locomotive.trainLink().setAutomaticRunning(controlMode==ControlMode.ROUTE_AUTOMATIC);
        locomotive.setPos(center);
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        locomotive.setYRot(yaw);
        spawned.add(locomotive);
        for (int index = 0; index < config.template().cars().size(); index++) {
            CarType carType = config.template().cars().get(index);
            Entity car = carType == CarType.CARGO
                    ? ModEntities.CARGO_CAR.create(level, EntitySpawnReason.TRIGGERED)
                    : ModEntities.PASSENGER_CAR.create(level, EntitySpawnReason.TRIGGERED);
            if (!(car instanceof LinkedTrainCar linked)) return DispatchOutcome.failure(DepotDispatchResult.SPAWN_FAILED);
            if (car instanceof PassengerCarEntity passenger) passenger.releasePlacementBrake();
            if (car instanceof CargoCarEntity cargo) cargo.releasePlacementBrake();
            linked.trainLink().assign(trainId, locomotive.getUUID(), index + 1, lineId);
            linked.trainLink().setDepotGenerated(true);
            linked.trainLink().setControlMode(controlMode);
            linked.trainLink().setTrainClass(trainClass);
            linked.trainLink().setConfiguredSpeedLimitKmh(config.speedLimitKmh(trainClass));
            linked.trainLink().setFormationForward(formationForward);
            car.setPos(center.subtract(forward.scale((index + 1) * 1.25D)));
            car.setYRot(yaw);
            spawned.add(car);
        }
        for (Entity entity : spawned) {
            BlockPos occupiedRail = BlockPos.containing(entity.position());
            var occupiedState = level.getBlockState(occupiedRail);
            if (!(occupiedState.is(ModBlocks.SIGNAL_RAIL) || occupiedState.is(ModBlocks.SWITCH_RAIL))
                    || !level.noCollision(entity)) return DispatchOutcome.failure(DepotDispatchResult.TRACK_SPACE_INVALID);
        }
        List<Entity> added = new ArrayList<>();
        for (Entity entity : spawned) {
            if (!level.addFreshEntity(entity)) {
                added.forEach(Entity::discard);
                return DispatchOutcome.failure(DepotDispatchResult.SPAWN_FAILED);
            }
            added.add(entity);
        }
        if (consumeItems) {
            take(containers, ModItems.LOCOMOTIVE, 1);
            take(containers, ModItems.PASSENGER_CAR, (int) config.template().cars().stream().filter(type -> type == CarType.PASSENGER).count());
            take(containers, ModItems.CARGO_CAR, (int) config.template().cars().stream().filter(type -> type == CarType.CARGO).count());
            containers.forEach(Container::setChanged);
        }
        if(controlMode==ControlMode.ROUTE_AUTOMATIC){
            RailwayDataManager.state(level).registerActiveTrain(trainId, lineId);
            TrainControllerManager.registerAutomaticPath(trainId, automaticPath, loop, loopStartIndex);
        }
        RailwaySavedData savedData = RailwayDataManager.state(level);
        savedData.ensureTrain(trainId, true, lineId,controlMode);
        RailwayDevice originDepot = savedData.find(RailwayDataManager.dimension(level),
                depotPos.getX(), depotPos.getY(), depotPos.getZ());
        TrainRegistryService.replaceFormation(level, trainId, spawned,
                originDepot == null ? 0L : originDepot.id());
        if(controlMode==ControlMode.ROUTE_AUTOMATIC)locomotive.setDeltaMovement(forward.scale(SpeedRules.blocksPerTick(
                SpeedRules.actualSpeed(line.speedLimitKmh(), RailwayDataManager.state(level).classSpeedLimit(trainClass),
                        config.speedLimitKmh(trainClass)))));
        else locomotive.setDeltaMovement(Vec3.ZERO);
        return new DispatchOutcome(DepotDispatchResult.SUCCESS, trainId);
    }

    public record DispatchOutcome(DepotDispatchResult result, UUID trainId) {
        public static DispatchOutcome failure(DepotDispatchResult result) {
            return new DispatchOutcome(result, null);
        }

        public boolean succeeded() {
            return result.succeeded() && trainId != null;
        }
    }

    private static boolean reachesNode(ServerLevel level, BlockPos origin, Vec3i initialStep) {
        BlockPos previous = origin;
        BlockPos current = origin.offset(initialStep);
        for (int distance = 0; distance < WorldLineValidator.MAXIMUM_VISITED_TRACKS_PER_SEGMENT; distance++) {
            if (!level.hasChunkAt(current)) return false;
            var state = level.getBlockState(current);
            if (state.is(ModBlocks.SWITCH_RAIL)) return true;
            for (Direction direction : Direction.values())
                if (level.getBlockState(current.relative(direction)).is(ModBlocks.STATION_MARKER)) return true;
            if (!BaseRailBlock.isRail(level, current) || !state.hasProperty(RailBlock.SHAPE)) return false;
            var exits = AbstractMinecart.exits(state.getValue(RailBlock.SHAPE));
            BlockPos first = current.offset(exits.getFirst());
            BlockPos second = current.offset(exits.getSecond());
            BlockPos next = first.equals(previous) ? second : first;
            previous = current;
            current = next;
        }
        return false;
    }

    private static List<Container> connectedContainers(ServerLevel level, BlockPos pos, TrainDepotBlockEntity depot) {
        ArrayList<Container> result = new ArrayList<>(); result.add(depot);
        for (Direction direction : Direction.values())
            if (level.getBlockEntity(pos.relative(direction)) instanceof Container container) result.add(container);
        return result;
    }

    private static boolean has(List<Container> containers, Item item, int amount) {
        if (amount <= 0) return true; int found = 0;
        for (Container container : containers) for (int slot=0;slot<container.getContainerSize();slot++)
            if (container.getItem(slot).is(item) && (found += container.getItem(slot).getCount()) >= amount) return true;
        return false;
    }

    private static void take(List<Container> containers, Item item, int amount) {
        for (Container container : containers) for (int slot=0;slot<container.getContainerSize()&&amount>0;slot++) {
            ItemStack stack=container.getItem(slot); if(!stack.is(item))continue;
            int moved=Math.min(amount,stack.getCount());stack.shrink(moved);amount-=moved;container.setItem(slot,stack);
        }
    }

    private static int find(TrainDepotBlockEntity inventory, Item item, int start) {
        for (int slot = start; slot < inventory.getContainerSize(); slot++) if (inventory.getItem(slot).is(item)) return slot;
        return -1;
    }

    private static List<Integer> reserveTemplate(TrainDepotBlockEntity inventory) {
        int[] reserved = new int[inventory.getContainerSize()];
        ArrayList<Integer> slots = new ArrayList<>();
        for (CarType type : inventory.template().cars()) {
            Item required = type == CarType.CARGO ? ModItems.CARGO_CAR : ModItems.PASSENGER_CAR;
            int selected = -1;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (inventory.getItem(slot).is(required) && inventory.getItem(slot).getCount() > reserved[slot]) {
                    selected = slot;
                    break;
                }
            }
            if (selected < 0) return null;
            reserved[selected]++;
            slots.add(selected);
        }
        return slots;
    }

    public static void clear() { LAST_DISPATCH_TICK.clear(); ticks = 0; }
}
