package cn.quit5700.fabrictrain.train;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class DriverSelector {
    private DriverSelector() {
    }

    public static Optional<UUID> nearestToLocomotive(List<SeatOccupant> occupants) {
        return occupants.stream()
                .min(Comparator.comparingInt(SeatOccupant::carIndex))
                .map(SeatOccupant::playerId);
    }

    public record SeatOccupant(UUID playerId, int carIndex) {
        public SeatOccupant {
            if (carIndex < 0) {
                throw new IllegalArgumentException("Car index cannot be negative");
            }
        }
    }
}
