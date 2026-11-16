package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.train.ControlMode;

import java.util.Set;

public final class StationSelectionRules {
    private StationSelectionRules() {}

    public static boolean canSelect(ControlMode controlMode, long lineId, long stationId,
                                    boolean registeredStation, Set<Long> lineStationIds) {
        if (stationId == 0L) return true;
        if (!registeredStation) return false;
        return controlMode == ControlMode.MANUAL
                || lineId > 0L && lineStationIds.contains(stationId);
    }
}
