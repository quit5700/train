package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DriverSelectorTest {
    @Test
    void closestOccupiedCarToLocomotiveDrives() {
        UUID farther = UUID.randomUUID();
        UUID nearer = UUID.randomUUID();
        assertEquals(nearer, DriverSelector.nearestToLocomotive(List.of(
                new DriverSelector.SeatOccupant(farther, 4),
                new DriverSelector.SeatOccupant(nearer, 1)
        )).orElseThrow());
    }
}
