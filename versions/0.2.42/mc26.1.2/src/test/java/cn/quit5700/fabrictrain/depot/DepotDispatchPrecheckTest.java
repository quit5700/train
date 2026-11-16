package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.data.LineStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DepotDispatchPrecheckTest {
    @Test void separatesEveryRejectedReason() {
        assertEquals(DepotDispatchPrecheck.Result.NO_LINE, DepotDispatchPrecheck.check(null, 0, 5));
        assertEquals(DepotDispatchPrecheck.Result.MAXIMUM_REACHED, DepotDispatchPrecheck.check(LineStatus.RUNNING, 5, 5));
        assertEquals(DepotDispatchPrecheck.Result.PAUSED, DepotDispatchPrecheck.check(LineStatus.PAUSED, 0, 5));
        assertEquals(DepotDispatchPrecheck.Result.CLOSED, DepotDispatchPrecheck.check(LineStatus.CLOSED, 0, 5));
        assertEquals(DepotDispatchPrecheck.Result.MAINTENANCE, DepotDispatchPrecheck.check(LineStatus.MAINTENANCE, 0, 5));
        assertEquals(DepotDispatchPrecheck.Result.NEEDS_VALIDATION, DepotDispatchPrecheck.check(LineStatus.NEEDS_VALIDATION, 0, 5));
        assertEquals(DepotDispatchPrecheck.Result.READY, DepotDispatchPrecheck.check(LineStatus.RUNNING, 0, 5));
    }
}
