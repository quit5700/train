package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.train.TrainTrafficSafety;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public final class RealtimeSwitchTraffic {
    private static final double MINIMUM_RESERVATION_DISTANCE = 8.0D;
    private static final double MAXIMUM_RESERVATION_DISTANCE = 128.0D;

    private RealtimeSwitchTraffic() {}

    public static Optional<UUID> chooseWinner(Collection<Candidate> candidates) {
        return candidates.stream().min(Comparator
                .comparing(Candidate::occupying).reversed()
                .thenComparingDouble(Candidate::distanceSquared)
                .thenComparing(candidate -> candidate.trainId().toString()))
                .map(Candidate::trainId);
    }

    public static double reservationDistance(double speed) {
        return Math.clamp(TrainTrafficSafety.stoppingDistance(speed),
                MINIMUM_RESERVATION_DISTANCE, MAXIMUM_RESERVATION_DISTANCE);
    }

    public record Candidate(UUID trainId, double distanceSquared, boolean occupying) {
        public Candidate {
            if (trainId == null) throw new IllegalArgumentException("trainId");
            distanceSquared = Math.max(0.0D, distanceSquared);
        }
    }
}
