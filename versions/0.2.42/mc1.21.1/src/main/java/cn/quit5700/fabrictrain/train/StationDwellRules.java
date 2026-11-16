package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.RailwayDevice;

public final class StationDwellRules {
    private StationDwellRules() {}

    public static int automaticSeconds(RailwayDevice station, TrainClass trainClass) {
        return trainClass == TrainClass.CARGO ? station.cargoDwellSeconds() : station.dwellSeconds();
    }
}
