package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogicalSpeedStateTest {
    @Test
    void acceleratesWithoutJumpingToMaximumSpeed() {
        LogicalSpeedState state = new LogicalSpeedState();

        assertEquals(0.015D, state.approach(1.0D, 0.015D), 0.0001D);
        assertEquals(0.030D, state.approach(1.0D, 0.015D), 0.0001D);
    }

    @Test
    void brakingCrossesZeroBeforeReversing() {
        LogicalSpeedState state = new LogicalSpeedState();
        state.approach(0.03D, 0.03D);

        assertEquals(0.015D, state.approach(-1.0D, 0.015D), 0.0001D);
        assertEquals(0.0D, state.approach(-1.0D, 0.015D), 0.0001D);
        assertEquals(-0.015D, state.approach(-1.0D, 0.015D), 0.0001D);
    }
}
