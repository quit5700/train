package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwaySavedData;
import cn.quit5700.fabrictrain.data.TrainManifest;
import cn.quit5700.fabrictrain.data.TrainMember;
import cn.quit5700.fabrictrain.data.TrainRecord;
import cn.quit5700.fabrictrain.data.TrainVehicleKind;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Keeps persistent train membership separate from loaded-entity scanning. */
public final class TrainRegistryService {
    private TrainRegistryService() {}

    public static void observeLoadedVehicle(ServerLevel level, Entity entity, LinkedTrainCar linked) {
        TrainLink link = linked.trainLink();
        RailwaySavedData data = RailwayDataManager.state(level);
        TrainRecord record = data.ensureTrain(link.trainId(), link.depotGenerated(), link.lineId(), link.controlMode());
        TrainManifest existing = data.trainManifest(link.trainId());
        long origin = resolveOrigin(existing, link.originDepotNumber());
        TrainMember registered = existing == null ? null : existing.members().stream()
                .filter(member -> member.entityId().equals(entity.getUUID())).findFirst().orElse(null);
        int sequence = registered == null ? Math.max(1, link.carIndex() + 1) : registered.sequence();
        if (registered != null && registered.kind() == kind(entity)) {
            link.setRegistryAddress(origin, record.number(), sequence);
            return;
        }
        data.registerTrainMember(link.trainId(), origin,
                new TrainMember(entity.getUUID(), kind(entity), sequence));
        link.setRegistryAddress(origin, record.number(), sequence);
    }

    public static void replaceFormation(ServerLevel level, UUID trainId, List<Entity> ordered, long originHint) {
        if (ordered == null || ordered.isEmpty()) return;
        LinkedTrainCar sample = (LinkedTrainCar) ordered.getFirst();
        RailwaySavedData data = RailwayDataManager.state(level);
        TrainRecord record = data.ensureTrain(trainId, sample.trainLink().depotGenerated(),
                sample.trainLink().lineId(), sample.trainLink().controlMode());
        long origin = resolveOrigin(data.trainManifest(trainId), originHint);
        if (origin == 0L) {
            for (Entity entity : ordered) {
                long candidate = ((LinkedTrainCar) entity).trainLink().originDepotNumber();
                if (candidate > 0L) { origin = candidate; break; }
            }
        }
        ArrayList<TrainMember> members = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            Entity entity = ordered.get(index);
            int sequence = index + 1;
            members.add(new TrainMember(entity.getUUID(), kind(entity), sequence));
            ((LinkedTrainCar) entity).trainLink().setRegistryAddress(origin, record.number(), sequence);
        }
        data.replaceTrainManifest(trainId, origin, members);
    }

    public static void replaceFormation(ServerLevel level, UUID trainId, List<Entity> ordered) {
        replaceFormation(level, trainId, ordered, 0L);
    }

    public static RailwaySavedData.MemberRemovalResult removeVehicle(ServerLevel level, UUID trainId, UUID entityId) {
        return RailwayDataManager.state(level).unregisterTrainMember(trainId, entityId);
    }

    public static List<Entity> stableOrder(List<Entity> entities) {
        return entities.stream().sorted(Comparator.comparingInt(entity ->
                ((LinkedTrainCar) entity).trainLink().carIndex())).toList();
    }

    private static long resolveOrigin(TrainManifest manifest, long hint) {
        if (hint > 0L) return hint;
        return manifest == null ? 0L : manifest.originDepotNumber();
    }

    private static TrainVehicleKind kind(Entity entity) {
        if (entity instanceof LocomotiveEntity) return TrainVehicleKind.LOCOMOTIVE;
        if (entity instanceof CargoCarEntity) return TrainVehicleKind.CARGO;
        if (entity instanceof PassengerCarEntity) return TrainVehicleKind.PASSENGER;
        throw new IllegalArgumentException("Unsupported train vehicle: " + entity.getClass().getName());
    }
}
