package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.LineStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticLinePauseRulesTest {
    @Test void pausedAutomaticTrainWaitsAtItsNextStation() {
        assertTrue(AutomaticLinePauseRules.holdAtStation(ControlMode.ROUTE_AUTOMATIC, LineStatus.PAUSED));
    }

    @Test void manualTrainIsNeverHeldByLinePause() {
        assertFalse(AutomaticLinePauseRules.holdAtStation(ControlMode.MANUAL, LineStatus.PAUSED));
    }

    @Test void runningAutomaticTrainUsesNormalDwellTimer() {
        assertFalse(AutomaticLinePauseRules.holdAtStation(ControlMode.ROUTE_AUTOMATIC, LineStatus.RUNNING));
    }
}
