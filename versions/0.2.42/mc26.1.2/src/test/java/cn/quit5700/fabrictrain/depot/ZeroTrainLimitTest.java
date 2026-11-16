package cn.quit5700.fabrictrain.depot;
import org.junit.jupiter.api.Test;
import cn.quit5700.fabrictrain.data.LineStatus;
import static org.junit.jupiter.api.Assertions.*;
class ZeroTrainLimitTest {
 @Test void zeroNeverDispatches(){assertFalse(AutomaticDispatchSchedule.shouldAttempt(true,true,0,0,9999,0,1));assertEquals(DepotDispatchPrecheck.Result.MAXIMUM_REACHED,DepotDispatchPrecheck.check(LineStatus.RUNNING,0,0));}
 @Test void negativeNeverDispatches(){assertFalse(AutomaticDispatchSchedule.shouldAttempt(true,true,0,-1,9999,0,1));}
 @Test void positiveKeepsOriginalSchedule(){assertTrue(AutomaticDispatchSchedule.shouldAttempt(true,true,0,1,9999,0,1));}
}
