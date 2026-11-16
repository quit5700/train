package cn.quit5700.fabrictrain.train;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationDirectionTest {
    @Test
    void locomotiveAndCarPositionsDetermineForwardDirection() {
        assertEquals(Direction.EAST, FormationDirection.fromPositions(10, 4, 8, 4).orElseThrow());
        assertEquals(Direction.NORTH, FormationDirection.fromPositions(2, -7, 2, -4).orElseThrow());
    }

    @Test
    void coincidentVehiclesDoNotInventADirection() {
        assertTrue(FormationDirection.fromPositions(3, 3, 3, 3).isEmpty());
    }
}
