package cn.quit5700.fabrictrain.data;

import java.util.UUID;

public record TrainMember(UUID entityId, TrainVehicleKind kind, int sequence) {
    public TrainMember {
        if (entityId == null) throw new IllegalArgumentException("entityId");
        if (kind == null) throw new IllegalArgumentException("kind");
        if (sequence < 1) throw new IllegalArgumentException("sequence");
    }
}
