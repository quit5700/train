package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.DeviceType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineLoopRulesTest {
    private static final Map<Long, DeviceType> TYPES = Map.of(
            1L, DeviceType.STATION, 2L, DeviceType.SWITCH, 3L, DeviceType.DEPOT, 4L, DeviceType.STATION);

    @Test void identicalFirstAndLastStationCreatesLoop() {
        assertTrue(LineLoopRules.isLoop(List.of(1L, 2L, 4L, 1L), TYPES::get));
    }

    @Test void identicalFirstAndLastSwitchCreatesLoop() {
        assertTrue(LineLoopRules.isLoop(List.of(2L, 1L, 4L, 2L), TYPES::get));
    }

    @Test void matchingInternalStationsDoNotCreateLoopWhenActualEndsDiffer() {
        assertFalse(LineLoopRules.isLoop(List.of(2L, 1L, 4L, 1L, 3L), TYPES::get));
    }

    @Test void depotCannotBeLoopAnchor() {
        assertFalse(LineLoopRules.isLoop(List.of(3L, 1L, 3L), TYPES::get));
    }
}
