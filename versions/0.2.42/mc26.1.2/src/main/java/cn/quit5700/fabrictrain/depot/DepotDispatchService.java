package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.*;
import cn.quit5700.fabrictrain.registry.ModEntities;
import cn.quit5700.fabrictrain.registry.ModItems;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.OfflineTrainService;
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
        DispatchAuthorityService.refresh(server);
        for (ServerLevel level : server.getAllLevels()) {
            RailwaySavedData data = RailwayDataManager.state(level);
            for (RailwayDevice device : data.devices().values()) {
                if (device.type() != DeviceType.DEPOT
                        || !device.dimension().equals(RailwayDataManager.dimension(level))) continue;
                BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
                if (level.hasChunkAt(pos) && cn.quit5700.fabrictrain.data.LoadedRailWorld.entity(level,pos) instanceof TrainDepotBlockEntity depot)
                    { absorbConnectedVehicles(level, pos, depot); DepotRecallService.process(level,depot); }
            }
            for (RailwayLine line : data.lines().values()) {
                if (line.status() != LineStatus.RUNNING) continue;
                long last = LAST_DISPATCH_TICK.getOrDefault(line.id(), Long.MIN_VALUE / 2);
                RailwayDevice depot = data.device(line.depotId());
                if (depot == null || !depot.dimension().equals(RailwayDataManager.dimension(level))) continue;
                BlockPos depotPos = new BlockPos(depot.x(), depot.y(), depot.z());
                if (!level.hasChunkAt(depotPos) || !(cn.quit5700.fabrictrain.data.LoadedRailWorld.entity(level,depotPos) instanceof TrainDepotBlockEntity inventory)) {
                    cn.quit5700.fabrictrain.offline.InfrastructureService.dispatch(level,line); continue;
                }
                if (!cn.quit5700.fabrictrain.offline.InfrastructureService.due(level,depot.id())) continue;
                if (!AutomaticDispatchSchedule.shouldAttempt(inventory.automaticDispatchEnabled(),
                        inventory.selectedLineId() == line.id(), data.activeAutomaticTrainCount(line.id()),
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
            if (!(cn.quit5700.fabrictrain.data.LoadedRailWorld.entity(level,pos.relative(direction)) instanceof Container source) || source == depot) continue;
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
        if(controlMode==ControlMode.ROUTE_AUTOMATIC&&config.maximumTrains()<=0)return DispatchOutcome.failure(DepotDispatchResult.DIRECTION_UNRESOLVED);
        if(config.template().cars().isEmpty())return DispatchOutcome.failure(DepotDispatchResult.EMPTY_TEMPLATE);
        if(controlMode==ControlMode.ROUTE_AUTOMATIC&&line==null)return DispatchOutcome.failure(DepotDispatchResult.DIRECTION_UNRESOLVED);
        BlockPos railPos = depotPos.above();
        if (!BaseRailBlock.isRail(level, railPos)) return DispatchOutcome.failure(DepotDispatchResult.NO_RAIL);
        RailPowerService.refreshForDispatch(level);
        Vec3 forward;
        List<TrackPoint> automaticPath = List.of();
        boolean loop = false;
        int loopStartIndex = 0;
        if (controlMode == ControlMode.ROUTE_AUTOMATIC) {
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
        if (lineId > 0 && cn.quit5700.fabrictrain.freight.FreightYardService.startsAtYard(
                RailwayDataManager.state(level), line)) locomotive.trainLink().updateRouteProgress(0, 0L, 0L);
        locomotive.trainLink().setControlMode(controlMode);
        TrainClass trainClass = config.effectiveClass();
        locomotive.trainLink().setTrainClass(trainClass);
        if (config.trainClass() == TrainClass.CARGO)
            locomotive.trainLink().setDestinationStationId(config.template().destination(0));
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
            if (config.trainClass() == TrainClass.CARGO && car instanceof CargoCarEntity cargo)
                cargo.setDestinationStationId(config.template().destination(index + 1));
            linked.trainLink().setConfiguredSpeedLimitKmh(config.speedLimitKmh(trainClass));
            linked.trainLink().setFormationForward(formationForward);
            car.setPos(center.subtract(forward.scale((index + 1) * 1.25D)));
            car.setYRot(yaw);
            spawned.add(car);
        }
        for (int memberIndex = 0; memberIndex < spawned.size(); memberIndex++) {
            Entity entity = spawned.get(memberIndex);
            BlockPos occupiedRail = BlockPos.containing(entity.position());
            if (!level.hasChunkAt(occupiedRail)) return DispatchOutcome.failureAt(DepotDispatchResult.SPAWN_UNLOADED, memberIndex, occupiedRail);
            var occupiedState = level.getBlockState(occupiedRail);
            if (!(occupiedState.is(ModBlocks.SIGNAL_RAIL) || occupiedState.is(ModBlocks.SWITCH_RAIL)))
                return DispatchOutcome.failureAt(DepotDispatchResult.SPAWN_MISSING_TRACK, memberIndex, occupiedRail);
            for (var shape : level.getBlockCollisions(entity, entity.getBoundingBox()))
                if (!shape.isEmpty()) return DispatchOutcome.failureAt(DepotDispatchResult.SPAWN_BLOCK_BLOCKED, memberIndex, occupiedRail);
            if (!level.getEntities(entity,entity.getBoundingBox(),other -> other.isAlive() && !other.isSpectator() && (other instanceof LinkedTrainCar || other.isPickable())).isEmpty()
                    || !level.noCollision(entity)) return DispatchOutcome.failureAt(DepotDispatchResult.SPAWN_ENTITY_BLOCKED, memberIndex, occupiedRail);
        }
        List<Entity> added = new ArrayList<>();
        for (Entity entity : spawned) {
            if (entity instanceof cn.quit5700.fabrictrain.train.LogicallyControlledVehicle vehicle)
                vehicle.fabricTrain$setLogicallyControlled(true);
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
                RailwayDataManager.state(level).classSpeedLimit(trainClass))));
        else locomotive.setDeltaMovement(Vec3.ZERO);
        if(controlMode==ControlMode.ROUTE_AUTOMATIC) {
            cn.quit5700.fabrictrain.offline.InfrastructureService.observePlan(level,depotPos,config,automaticPath,loop,loopStartIndex);
            OfflineTrainService.checkpoint(level,locomotive,spawned,automaticPath,loop,loopStartIndex,0,0,
                    Math.max(0.01D,locomotive.getDeltaMovement().length()));
        }
        return new DispatchOutcome(DepotDispatchResult.SUCCESS, trainId, spawned);
    }

    public static void configureManualMode(DispatchOutcome outcome,boolean stationMode){
        if(!outcome.succeeded())return;
        for(Entity entity:outcome.spawned())if(entity instanceof LinkedTrainCar linked&&linked.trainLink().controlMode()==ControlMode.MANUAL){
            linked.trainLink().manualStationControl().restore(stationMode,false);linked.trainLink().setParked(true);entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    public record DispatchOutcome(DepotDispatchResult result, UUID trainId, List<Entity> spawned, int failedMember, BlockPos failedPosition) {
        public DispatchOutcome { spawned=List.copyOf(spawned); failedPosition=failedPosition==null?null:failedPosition.immutable(); }
        public DispatchOutcome(DepotDispatchResult result,UUID trainId,List<Entity> spawned){this(result,trainId,spawned,-1,null);}
        public DispatchOutcome(DepotDispatchResult result,UUID trainId){this(result,trainId,List.of());}
        public static DispatchOutcome failure(DepotDispatchResult result){return new DispatchOutcome(result,null);}
        public static DispatchOutcome failureAt(DepotDispatchResult result,int member,BlockPos pos){return new DispatchOutcome(result,null,List.of(),member,pos);}
        public String messageKey(){return result.translationKey()+(failedPosition==null?"":failedMember==0?".locomotive":".car");}
        public String[] messageArguments(){
            if(failedPosition==null)return new String[0];
            String x=Integer.toString(failedPosition.getX()),y=Integer.toString(failedPosition.getY()),z=Integer.toString(failedPosition.getZ());
            return failedMember==0?new String[]{x,y,z}:new String[]{Integer.toString(failedMember),x,y,z};
        }
        public boolean succeeded(){return result.succeeded()&&trainId!=null;}
    }

    private static boolean reachesNode(ServerLevel level, BlockPos origin, Vec3i initialStep) {
        BlockPos previous = origin;
        BlockPos current = origin.offset(initialStep);
        for (int distance = 0; distance < WorldLineValidator.MAXIMUM_VISITED_TRACKS_PER_SEGMENT; distance++) {
            if (!level.hasChunkAt(current)) return false;
            var state = level.getBlockState(current);
            if (state.is(ModBlocks.SWITCH_RAIL) || level.getBlockState(current.below()).is(ModBlocks.FREIGHT_YARD)) return true;
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
            if (cn.quit5700.fabrictrain.data.LoadedRailWorld.entity(level,pos.relative(direction)) instanceof Container container) result.add(container);
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
