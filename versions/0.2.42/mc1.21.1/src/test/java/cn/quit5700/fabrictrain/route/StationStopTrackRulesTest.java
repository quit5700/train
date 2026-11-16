package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.StopTrack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationStopTrackRulesTest {
    @Test
    void stopRailIsAlwaysOneBlockAboveTheStation() {
        assertEquals(new StopTrack(24, 81, -13), StationStopTrackRules.directlyAbove(24, 80, -13));
    }
}
