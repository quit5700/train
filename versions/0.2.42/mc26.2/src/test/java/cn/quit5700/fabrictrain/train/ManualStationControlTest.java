package cn.quit5700.fabrictrain.train;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ManualStationControlTest {
 @Test void heldForwardCannotSkipTheStop(){var s=new ManualStationControl();s.restore(true,false);assertTrue(s.arrive());for(int i=0;i<200;i++)assertFalse(s.acceptInput(true));assertTrue(s.waiting());}
 @Test void releaseThenFreshForwardDepartsAndNextStationStopsAgain(){var s=new ManualStationControl();s.restore(true,false);s.arrive();assertFalse(s.acceptInput(false));assertTrue(s.acceptInput(true));assertFalse(s.waiting());s.arrive();assertFalse(s.acceptInput(true));}
 @Test void freeModeKeepsExistingControl(){var s=new ManualStationControl();assertFalse(s.arrive());assertTrue(s.acceptInput(true));assertTrue(s.acceptInput(false));}
 @Test void reloadCannotBypassWaitingWithHeldForward(){var s=new ManualStationControl();s.restore(true,true);assertFalse(s.acceptInput(true));assertFalse(s.acceptInput(false));assertTrue(s.acceptInput(true));}
 @Test void releasedBeforeArrivalNeedsOnlyOneFreshPress(){var s=new ManualStationControl();s.restore(true,false);s.acceptInput(true);s.acceptInput(false);s.arrive();assertTrue(s.waiting());assertTrue(s.acceptInput(true));assertFalse(s.waiting());}
}
