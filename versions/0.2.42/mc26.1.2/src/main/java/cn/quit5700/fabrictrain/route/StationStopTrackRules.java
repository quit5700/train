package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.StopTrack;

public final class StationStopTrackRules {
    private StationStopTrackRules() {}

    public static StopTrack directlyAbove(int stationX, int stationY, int stationZ) {
        return new StopTrack(stationX, stationY + 1, stationZ);
    }
}
