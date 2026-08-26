package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormationSpacingTest {
    @Test
    void connectedVehiclesUseNearTouchingSpacing() {
        assertEquals(1.05D, FormationSpacing.BLOCKS, 0.0001D);
        assertEquals(3.15D, FormationSpacing.BLOCKS * 3, 0.0001D);
    }
}
