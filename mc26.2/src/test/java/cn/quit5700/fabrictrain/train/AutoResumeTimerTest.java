package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AutoResumeTimerTest {
    @Test
    void resumesAfterThirtySeconds() {
        AutoResumeTimer timer = new AutoResumeTimer();
        timer.onStopOrInteraction();
        for (int tick = 1; tick < AutoResumeTimer.DEFAULT_DELAY_TICKS; tick++) {
            assertFalse(timer.tickAndShouldResume());
        }
        assertTrue(timer.tickAndShouldResume());
    }
}
