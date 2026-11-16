package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.LineStatus;

/** Keeps restart recovery decisions independent from entity and world access. */
public final class StartupAutomaticResumeRules {
    private StartupAutomaticResumeRules() {
    }

    public static boolean shouldResume(ControlMode controlMode, LineStatus lineStatus, boolean pathRestored) {
        return controlMode == ControlMode.ROUTE_AUTOMATIC
                && lineStatus == LineStatus.RUNNING
                && pathRestored;
    }
}
