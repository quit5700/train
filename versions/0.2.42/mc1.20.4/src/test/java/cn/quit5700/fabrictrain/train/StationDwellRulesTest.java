package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationDwellRulesTest {
    private final RailwayDevice station = new RailwayDevice(1, DeviceType.STATION, "minecraft:overworld",
            0, 64, 0, "站点1", 30, 120, 1, "", false);

    @Test void cargoUsesDedicatedStationDwell() {
        assertEquals(120, StationDwellRules.automaticSeconds(station, TrainClass.CARGO));
    }

    @Test void passengerAndExpressUseNormalDwell() {
        assertEquals(30, StationDwellRules.automaticSeconds(station, TrainClass.PASSENGER));
        assertEquals(30, StationDwellRules.automaticSeconds(station, TrainClass.EXPRESS));
    }
}
