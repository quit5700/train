package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticDispatchScheduleTest {
    @Test
    void dispatchesAtEveryIntervalUntilAutomaticTrainLimit() {
        assertFalse(AutomaticDispatchSchedule.shouldAttempt(true, true, 1, 5, 299, 0, 15));
        assertTrue(AutomaticDispatchSchedule.shouldAttempt(true, true, 1, 5, 300, 0, 15));
        assertTrue(AutomaticDispatchSchedule.shouldAttempt(true, true, 2, 5, 600, 300, 15));
        assertFalse(AutomaticDispatchSchedule.shouldAttempt(true, true, 5, 5, 900, 600, 15));
    }

    @Test
    void disabledOrMismatchedDepotNeverDispatches() {
        assertFalse(AutomaticDispatchSchedule.shouldAttempt(false, true, 0, 5, 300, 0, 15));
        assertFalse(AutomaticDispatchSchedule.shouldAttempt(true, false, 0, 5, 300, 0, 15));
    }
}
