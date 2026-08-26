package cn.quit5700.fabrictrain.train;

import java.util.Collection;
import java.util.Comparator;
import java.util.UUID;

final class TrainDriverAuthority {
    private TrainDriverAuthority() {}

    static UUID closest(Collection<Rider> riders) {
        return riders.stream()
                .min(Comparator.comparingDouble(Rider::distanceSquared)
                        .thenComparing(rider -> rider.playerId().toString()))
                .map(Rider::playerId)
                .orElse(null);
    }

    record Rider(UUID playerId, double distanceSquared) {}
}
