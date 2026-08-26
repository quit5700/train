package cn.quit5700.fabrictrain.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Server-owned membership for one formation. Entity UUID is permanent; sequence is mutable. */
public record TrainManifest(long originDepotNumber, List<TrainMember> members) {
    public TrainManifest {
        originDepotNumber = Math.max(0L, originDepotNumber);
        LinkedHashMap<UUID, TrainMember> unique = new LinkedHashMap<>();
        if (members != null) {
            members.stream().sorted(Comparator.comparingInt(TrainMember::sequence))
                    .forEach(member -> unique.put(member.entityId(), member));
        }
        ArrayList<TrainMember> normalized = new ArrayList<>(unique.size());
        int sequence = 1;
        for (TrainMember member : unique.values())
            normalized.add(new TrainMember(member.entityId(), member.kind(), sequence++));
        members = List.copyOf(normalized);
    }

    public int totalVehicles() { return members.size(); }
    public long locomotives() { return count(TrainVehicleKind.LOCOMOTIVE); }
    public long passengerCars() { return count(TrainVehicleKind.PASSENGER); }
    public long cargoCars() { return count(TrainVehicleKind.CARGO); }
    public boolean contains(UUID entityId) {
        return members.stream().anyMatch(member -> member.entityId().equals(entityId));
    }
    public TrainManifest without(UUID entityId) {
        return new TrainManifest(originDepotNumber,
                members.stream().filter(member -> !member.entityId().equals(entityId)).toList());
    }

    private long count(TrainVehicleKind kind) {
        return members.stream().filter(member -> member.kind() == kind).count();
    }
}
