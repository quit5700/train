package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.LineStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StartupAutomaticResumeRulesTest {
    @Test
    void onlyRunningAutomaticTrainWithRestoredPathResumes() {
        assertTrue(StartupAutomaticResumeRules.shouldResume(
                ControlMode.ROUTE_AUTOMATIC, LineStatus.RUNNING, true));
        assertFalse(StartupAutomaticResumeRules.shouldResume(
                ControlMode.MANUAL, LineStatus.RUNNING, true));
        assertFalse(StartupAutomaticResumeRules.shouldResume(
                ControlMode.ROUTE_AUTOMATIC, LineStatus.PAUSED, true));
        assertFalse(StartupAutomaticResumeRules.shouldResume(
                ControlMode.ROUTE_AUTOMATIC, LineStatus.RUNNING, false));
    }
}
