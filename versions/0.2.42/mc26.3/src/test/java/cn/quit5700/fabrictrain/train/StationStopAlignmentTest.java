package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.StopTrack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationStopAlignmentTest {
    @Test void stopPositionIsTheExactCentreOfTheBoundStationTrack() {
        StationStopAlignment.Position position = StationStopAlignment.position(new StopTrack(10, 64, -3));
        assertEquals(10.5D, position.x(), 0.0001D);
        assertEquals(64.1D, position.y(), 0.0001D);
        assertEquals(-2.5D, position.z(), 0.0001D);
    }
}
