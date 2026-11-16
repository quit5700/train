package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.LineStatus;

public final class AutomaticLinePauseRules {
    private AutomaticLinePauseRules() {}

    public static boolean holdAtStation(ControlMode controlMode, LineStatus status) {
        return controlMode == ControlMode.ROUTE_AUTOMATIC && status == LineStatus.PAUSED;
    }
}
