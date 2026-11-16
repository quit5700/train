package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwaySavedData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class TrainFormationService {
    public enum PairResult {
        CONNECTED, DISCONNECTED, CANCELLED, NOT_ADJACENT,
        TOO_FAR, TOO_LONG, TWO_LOCOMOTIVES, MIXED_ORIGIN, NO_CONTINUOUS_TRACK, FAILED
    }

    private TrainFormationService() {
    }

    private static PairResult connectPair(Entity first, Entity second) {
        List<Entity> left = formation(first);
        List<Entity> right = formation(second);
        ControlMode leftMode = ((LinkedTrainCar) first).trainLink().controlMode();
        ControlMode rightMode = ((LinkedTrainCar) second).trainLink().controlMode();
        UUID leftTrainId = ((LinkedTrainCar) first).trainLink().trainId();
        UUID rightTrainId = ((LinkedTrainCar) second).trainLink().trainId();
        if (!TrainRegistryRules.canMerge(leftMode, rightMode)) return PairResult.MIXED_ORIGIN;
        if (!ConnectorRules.canMerge(left.size(), right.size())) return PairResult.TOO_LONG;
        ArrayList<Entity> joined = mergeOrder(left, right);
        List<LocomotiveEntity> locomotives = joined.stream().filter(LocomotiveEntity.class::isInstance)
                .map(LocomotiveEntity.class::cast).toList();
        if (locomotives.size() > 1) return PairResult.TWO_LOCOMOTIVES;
        if (locomotives.size() == 1) {
            int locomotiveIndex = joined.indexOf(locomotives.getFirst());
            if (locomotiveIndex != 0 && locomotiveIndex != joined.size() - 1) return PairResult.FAILED;
            if (locomotiveIndex == joined.size() - 1) Collections.reverse(joined);
        }
        Direction formationForward = locomotives.isEmpty() ? null
                : locomotives.getFirst().trainLink().formationForward().orElseGet(() -> joined.size() < 2 ? null
                : FormationDirection.fromPositions(locomotives.getFirst().getX(), locomotives.getFirst().getZ(),
                joined.get(1).getX(), joined.get(1).getZ()).orElse(null));
        if(formationForward!=null&&!locomotives.isEmpty())locomotives.getFirst().setYRot(yaw(formationForward));
        if (!(first.level() instanceof ServerLevel serverLevel)
                || !RailFormationPlacer.place(serverLevel, joined, first, second)) {
            return PairResult.NO_CONTINUOUS_TRACK;
        }
        UUID inherited = locomotives.size() == 1
                ? ((LinkedTrainCar) locomotives.getFirst()).trainLink().trainId()
                : ((LinkedTrainCar) first).trainLink().trainId();
        boolean depotGenerated=joined.stream().anyMatch(entity->((LinkedTrainCar)entity).trainLink().depotGenerated());
        double speedLimit=((LinkedTrainCar)(locomotives.isEmpty()?first:locomotives.getFirst())).trainLink().configuredSpeedLimitKmh();
        assignFormation(joined, inherited, depotGenerated, leftMode, speedLimit, formationForward);
        if (!leftTrainId.equals(inherited)) RailwayDataManager.state(serverLevel).removeTrainRecord(leftTrainId);
        if (!rightTrainId.equals(inherited)) RailwayDataManager.state(serverLevel).removeTrainRecord(rightTrainId);
        TrainControllerManager.resetFormation(inherited, joined);
        first.level().playSound(null, first.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1, 1);
        return PairResult.CONNECTED;
    }

    public static PairResult operatePair(Entity first, Entity second) {
        if (!(first instanceof LinkedTrainCar firstCar) || !(second instanceof LinkedTrainCar secondCar)
                || first.level() != second.level()) {
            return PairResult.FAILED;
        }
        boolean sameEntity = first == second;
        if (sameEntity) return PairResult.CANCELLED;
        if (!ConnectorRules.withinRange(first.distanceToSqr(second))) return PairResult.TOO_FAR;
        boolean sameFormation = firstCar.trainLink().trainId().equals(secondCar.trainLink().trainId());
        ConnectorActionRules.Action action = ConnectorActionRules.decide(sameEntity, sameFormation,
                firstCar.trainLink().carIndex(), secondCar.trainLink().carIndex());
        return switch (action) {
            case CANCEL_SELECTION -> PairResult.CANCELLED;
            case REJECT_NON_ADJACENT -> PairResult.NOT_ADJACENT;
            case CONNECT -> connectPair(first, second);
            case DISCONNECT_ADJACENT -> splitBetween(first, second)
                    ? PairResult.DISCONNECTED : PairResult.FAILED;
        };
    }

    private static ArrayList<Entity> mergeOrder(List<Entity> leftSource, List<Entity> rightSource) {
        ArrayList<Entity> left = new ArrayList<>(leftSource);
        ArrayList<Entity> right = new ArrayList<>(rightSource);
        boolean leftHasLocomotive = left.stream().anyMatch(LocomotiveEntity.class::isInstance);
        boolean rightHasLocomotive = right.stream().anyMatch(LocomotiveEntity.class::isInstance);
        if (leftHasLocomotive ^ rightHasLocomotive) {
            ArrayList<Entity> base = leftHasLocomotive ? left : right;
            ArrayList<Entity> addon = leftHasLocomotive ? right : left;
            int locomotiveIndex = indexOfLocomotive(base);
            if (locomotiveIndex == base.size() - 1) Collections.reverse(base);
            if (!addon.isEmpty() && base.getLast().distanceToSqr(addon.getLast())
                    < base.getLast().distanceToSqr(addon.getFirst())) Collections.reverse(addon);
            base.addAll(addon);
            return base;
        }
        double normal = left.getLast().distanceToSqr(right.getFirst());
        double reverseRight = left.getLast().distanceToSqr(right.getLast());
        double reverseLeft = left.getFirst().distanceToSqr(right.getFirst());
        double reverseBoth = left.getFirst().distanceToSqr(right.getLast());
        double best = Math.min(Math.min(normal, reverseRight), Math.min(reverseLeft, reverseBoth));
        if (best == reverseLeft || best == reverseBoth) Collections.reverse(left);
        if (best == reverseRight || best == reverseBoth) Collections.reverse(right);
        left.addAll(right);
        return left;
    }

    private static int indexOfLocomotive(List<Entity> group) {
        for (int index = 0; index < group.size(); index++) if (group.get(index) instanceof LocomotiveEntity) return index;
        return -1;
    }

    private static boolean splitBetween(Entity first, Entity second) {
        List<Entity> ordered = formation(first);
        int firstIndex = ordered.indexOf(first);
        int secondIndex = ordered.indexOf(second);
        if (firstIndex < 0 || secondIndex < 0 || Math.abs(firstIndex - secondIndex) != 1) return false;
        int splitIndex = Math.max(firstIndex, secondIndex);
        List<Entity> front = new ArrayList<>(ordered.subList(0, splitIndex));
        List<Entity> rear = new ArrayList<>(ordered.subList(splitIndex, ordered.size()));
        if (front.isEmpty() || rear.isEmpty()) return false;
        UUID original = ((LinkedTrainCar) first).trainLink().trainId();
        boolean depotGenerated = ((LinkedTrainCar) first).trainLink().depotGenerated();
        ControlMode controlMode=((LinkedTrainCar)first).trainLink().controlMode();
        double speedLimit=((LinkedTrainCar)first).trainLink().configuredSpeedLimitKmh();
        Direction formationForward=((LinkedTrainCar)first).trainLink().formationForward().orElse(null);
        boolean frontHasLocomotive = front.stream().anyMatch(LocomotiveEntity.class::isInstance);
        boolean rearHasLocomotive = rear.stream().anyMatch(LocomotiveEntity.class::isInstance);
        ControlMode frontMode = frontHasLocomotive ? controlMode : ControlMode.MANUAL;
        ControlMode rearMode = rearHasLocomotive ? controlMode : ControlMode.MANUAL;
        if (TrainRegistryRules.inheritedSide(frontHasLocomotive, rearHasLocomotive) == 1) {
            assignFormation(rear, original, depotGenerated, rearMode, speedLimit, formationForward);
            assignFormation(front, UUID.randomUUID(), depotGenerated, frontMode, speedLimit, formationForward);
        } else {
            assignFormation(front, original, depotGenerated, frontMode, speedLimit, formationForward);
            assignFormation(rear, UUID.randomUUID(), depotGenerated, rearMode, speedLimit, formationForward);
        }
        TrainControllerManager.resetFormation(((LinkedTrainCar) front.getFirst()).trainLink().trainId(), front);
        TrainControllerManager.resetFormation(((LinkedTrainCar) rear.getFirst()).trainLink().trainId(), rear);
        first.level().playSound(null, first.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1, 1);
        return true;
    }

    public static List<Entity> formation(Entity seed) {
        if (!(seed instanceof LinkedTrainCar linked)) return List.of();
        UUID trainId = linked.trainLink().trainId();
        ArrayList<Entity> result = new ArrayList<>();
        for (Entity entity : seed.level().getEntities(seed, seed.getBoundingBox().inflate(160),
                entity -> entity instanceof LinkedTrainCar candidate
                        && ((LinkedTrainCar) candidate).trainLink().trainId().equals(trainId))) result.add(entity);
        if (!result.contains(seed)) result.add(seed);
        result.sort(Comparator.comparingInt(entity -> ((LinkedTrainCar) entity).trainLink().carIndex()));
        return result;
    }

    public static int dissolve(Entity seed) {
        List<Entity> formation = formation(seed);
        UUID original = ((LinkedTrainCar) seed).trainLink().trainId();
        if (seed.level() instanceof ServerLevel level) {
            RailwaySavedData data = RailwayDataManager.state(level);
            var oldManifest = data.trainManifest(original);
            long originDepotNumber = oldManifest == null
                    ? ((LinkedTrainCar) seed).trainLink().originDepotNumber()
                    : oldManifest.originDepotNumber();
            formation.forEach(entity -> {
                TrainLink link = ((LinkedTrainCar) entity).trainLink();
                link.detach(entity.getUUID());
                link.setControlMode(ControlMode.MANUAL);
            });
            data.removeTrainRecord(original);
            formation.forEach(entity -> {
                data.ensureTrain(entity.getUUID(),
                        ((LinkedTrainCar) entity).trainLink().depotGenerated(), 0L,
                        ControlMode.MANUAL);
                TrainRegistryService.replaceFormation(level, entity.getUUID(), List.of(entity), originDepotNumber);
            });
        } else {
            formation.forEach(entity -> ((LinkedTrainCar) entity).trainLink().detach(entity.getUUID()));
        }
        if (!formation.isEmpty())
            seed.level().playSound(null, seed.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1, 1);
        return formation.size();
    }

    public static boolean reorder(Entity seed, List<Integer> entityIds) {
        List<Entity> current = formation(seed);
        if (entityIds.size() != current.size()) return false;
        Map<Integer, Entity> byId = new HashMap<>();
        current.forEach(entity -> byId.put(entity.getId(), entity));
        ArrayList<Entity> ordered = new ArrayList<>();
        for (int id : entityIds) {
            Entity entity = byId.remove(id);
            if (entity == null) return false;
            ordered.add(entity);
        }
        int locomotiveIndex = -1;
        for (int i = 0; i < ordered.size(); i++)
            if (ordered.get(i) instanceof LocomotiveEntity) {
                if (locomotiveIndex >= 0) return false;
                locomotiveIndex = i;
        }
        if (locomotiveIndex > 0 && locomotiveIndex < ordered.size() - 1) return false;
        if (locomotiveIndex == ordered.size() - 1) Collections.reverse(ordered);
        TrainLink seedLink=((LinkedTrainCar)seed).trainLink();
        assignFormation(ordered, seedLink.trainId(), seedLink.depotGenerated(),
                seedLink.controlMode(),seedLink.configuredSpeedLimitKmh(),seedLink.formationForward().orElse(null));
        normalizePositions(ordered);
        return true;
    }

    private static void orientEndpoint(List<Entity> group, Entity selected, boolean selectedFirst) {
        int index = group.indexOf(selected);
        if ((selectedFirst && index == group.size() - 1) || (!selectedFirst && index == 0)) Collections.reverse(group);
    }

    private static void assignFormation(List<Entity> ordered) {
        UUID trainId = UUID.randomUUID();
        boolean depotGenerated = ordered.stream().anyMatch(entity ->
                ((LinkedTrainCar) entity).trainLink().depotGenerated());
        TrainLink source=((LinkedTrainCar)ordered.getFirst()).trainLink();
        assignFormation(ordered, trainId, depotGenerated,source.controlMode(),source.configuredSpeedLimitKmh(),
                source.formationForward().orElse(null));
    }

    private static void assignFormation(List<Entity> ordered, UUID trainId, boolean depotGenerated,
                                        ControlMode controlMode,double speedLimitKmh,Direction formationForward) {
        long originDepotNumber = ordered.stream().map(entity -> ((LinkedTrainCar) entity).trainLink())
                .mapToLong(TrainLink::originDepotNumber).filter(value -> value > 0L).findFirst().orElse(0L);
        LocomotiveEntity locomotive = ordered.stream().filter(LocomotiveEntity.class::isInstance)
                .map(LocomotiveEntity.class::cast).findFirst().orElse(null);
        long lineId = locomotive == null ? 0 : locomotive.trainLink().lineId();
        long locomotiveDestination = locomotive == null ? 0L : locomotive.trainLink().destinationStationId();
        TrainClass trainClass = ordered.stream().anyMatch(CargoCarEntity.class::isInstance)
                ? TrainClass.CARGO : locomotive == null ? TrainClass.PASSENGER : locomotive.trainLink().trainClass();
        for (int i = 0; i < ordered.size(); i++) {
            LinkedTrainCar car = (LinkedTrainCar) ordered.get(i);
            if (locomotive == null) car.trainLink().assignWithoutLocomotive(trainId, i);
            else car.trainLink().assign(trainId, locomotive.getUUID(), i, lineId);
            car.trainLink().setTrainClass(trainClass);
            car.trainLink().setDepotGenerated(depotGenerated);
            car.trainLink().setControlMode(controlMode);
            car.trainLink().setConfiguredSpeedLimitKmh(speedLimitKmh);
            if(locomotive==null)car.trainLink().clearFormationForward();
            else car.trainLink().setFormationForward(formationForward);
            if(car instanceof CargoCarEntity cargo&&locomotive!=null){
                // A zero carriage destination remains an explicit "inherit" marker. The shared
                // TrainLink value carries the locomotive default without overwriting the carriage.
                car.trainLink().setDestinationStationId(locomotiveDestination);
            }
        }
        if (!ordered.isEmpty() && ordered.getFirst().level() instanceof ServerLevel level) {
            RailwayDataManager.state(level).ensureTrain(trainId, depotGenerated, lineId,controlMode);
            TrainRegistryService.replaceFormation(level, trainId, ordered, originDepotNumber);
        }
    }

    private static void normalizePositions(List<Entity> ordered) {
        if (ordered.size() < 2) return;
        Entity leader = ordered.getFirst();
        for (int i = 1; i < ordered.size(); i++) {
            Entity car = ordered.get(i);
            car.setPos(FormationGeometry.anchor(leader.position(), car.position(), i * FormationSpacing.BLOCKS));
            car.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        leader.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }

    public static int linkNearbyCars(LocomotiveEntity locomotive, long lineId) {
        return 0;
    }

    public static int linkCarGroup(LocomotiveEntity locomotive, Entity touchedCar, long lineId) {
        if (!(touchedCar instanceof LinkedTrainCar seed) || seed.trainLink().hasLocomotive()) return 0;
        UUID oldGroup = seed.trainLink().trainId();
        List<Entity> candidates = new ArrayList<>();
        for (Entity entity : locomotive.level().getEntities(locomotive, locomotive.getBoundingBox().inflate(160.0D),
                entity -> entity instanceof LinkedTrainCar linked && !(entity instanceof LocomotiveEntity)
                        && !linked.trainLink().hasLocomotive())) {
            if (((LinkedTrainCar) entity).trainLink().trainId().equals(oldGroup)) candidates.add(entity);
        }
        if (candidates.isEmpty()) candidates.add(touchedCar);
        List<Entity> ordered = orderFrom(locomotive, candidates);
        Direction formationForward=locomotive.trainLink().formationForward().orElseGet(()->
                FormationDirection.fromPositions(locomotive.getX(),locomotive.getZ(),
                        ordered.getFirst().getX(),ordered.getFirst().getZ()).orElse(null));
        if(formationForward!=null)locomotive.setYRot(yaw(formationForward));
        UUID trainId = locomotive.trainLink().trainId();
        locomotive.trainLink().assign(trainId, locomotive.getUUID(), 0, lineId);
        locomotive.trainLink().setFormationForward(formationForward);
        for (int index=0;index<ordered.size();index++)
        {
            LinkedTrainCar linked=(LinkedTrainCar)ordered.get(index);
            linked.trainLink().assign(trainId,locomotive.getUUID(),index+1,lineId);
            linked.trainLink().setFormationForward(formationForward);
        }
        boolean cargo=ordered.stream().anyMatch(CargoCarEntity.class::isInstance);
        locomotive.trainLink().setTrainClass(cargo?TrainClass.CARGO:locomotive.trainLink().trainClass());
        ordered.forEach(entity->((LinkedTrainCar)entity).trainLink().setTrainClass(locomotive.trainLink().trainClass()));
        locomotive.level().playSound(null,locomotive.blockPosition(),SoundEvents.CHAIN_PLACE,SoundSource.BLOCKS,1,1);
        if(lineId>0&&locomotive.level() instanceof ServerLevel serverLevel)
            RailwayDataManager.state(serverLevel).registerActiveTrain(trainId,lineId);
        if (locomotive.level() instanceof ServerLevel serverLevel) {
            ArrayList<Entity> formation = new ArrayList<>();
            formation.add(locomotive);
            formation.addAll(ordered);
            TrainRegistryService.replaceFormation(serverLevel, trainId, formation);
            RailwayDataManager.state(serverLevel).removeTrainRecord(oldGroup);
        }
        return ordered.size();
    }

    public static boolean mergeCarOnlyGroups(Entity first, Entity second) {
        if (!(first instanceof LinkedTrainCar a) || !(second instanceof LinkedTrainCar b)
                || first instanceof LocomotiveEntity || second instanceof LocomotiveEntity
                || a.trainLink().hasLocomotive() || b.trainLink().hasLocomotive()
                || a.trainLink().trainId().equals(b.trainLink().trainId())) return false;
        UUID firstGroup=a.trainLink().trainId(),secondGroup=b.trainLink().trainId();
        List<Entity> members=new ArrayList<>();
        for(Entity entity:first.level().getEntities(first,first.getBoundingBox().inflate(160),
                entity->entity instanceof LinkedTrainCar linked&&!(entity instanceof LocomotiveEntity)
                        &&!linked.trainLink().hasLocomotive())) {
            UUID id=((LinkedTrainCar)entity).trainLink().trainId();
            if(id.equals(firstGroup)||id.equals(secondGroup))members.add(entity);
        }
        if(!members.contains(first))members.add(first);if(!members.contains(second))members.add(second);
        Entity start = members.stream().filter(cn.quit5700.fabrictrain.entity.PassengerCarEntity.class::isInstance)
                .findFirst().orElse(first);
        List<Entity> ordered=orderFrom(start,members);UUID group=firstGroup;
        for(int i=0;i<ordered.size();i++)((LinkedTrainCar)ordered.get(i)).trainLink().assignWithoutLocomotive(group,i);
        if (first.level() instanceof ServerLevel serverLevel) {
            TrainRegistryService.replaceFormation(serverLevel, group, ordered);
            RailwayDataManager.state(serverLevel).removeTrainRecord(secondGroup);
        }
        first.level().playSound(null,first.blockPosition(),SoundEvents.CHAIN_PLACE,SoundSource.BLOCKS,1,1);
        return true;
    }

    public static boolean hasAttachedCars(LocomotiveEntity locomotive) {
        UUID train=locomotive.trainLink().trainId();
        for(Entity entity:locomotive.level().getEntities(locomotive,locomotive.getBoundingBox().inflate(160),
                entity->entity instanceof LinkedTrainCar&&entity!=locomotive))
            if(((LinkedTrainCar)entity).trainLink().hasLocomotive()
                    &&((LinkedTrainCar)entity).trainLink().trainId().equals(train))return true;
        return false;
    }

    private static List<Entity> orderFrom(Entity start,List<Entity> values){
        ArrayList<Entity> remaining=new ArrayList<>(values),result=new ArrayList<>();Entity cursor=start;
        while(!remaining.isEmpty()){Entity next=nearest(cursor,remaining);result.add(next);remaining.remove(next);cursor=next;}
        return result;
    }

    public static int detachFrom(Entity selected){
        return 0;
    }

    private static Entity nearest(Entity source, List<Entity> candidates) {
        return candidates.stream().min(Comparator.comparingDouble(source::distanceToSqr)).orElse(null);
    }

    private static float yaw(Direction direction){
        return (float)Math.toDegrees(Math.atan2(-direction.getStepX(),direction.getStepZ()));
    }
}
