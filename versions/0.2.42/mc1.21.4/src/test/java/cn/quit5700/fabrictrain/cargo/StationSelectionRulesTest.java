package cn.quit5700.fabrictrain.cargo;

import cn.quit5700.fabrictrain.train.ControlMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StationSelectionRulesTest {
    @Test void manualTrainMaySelectAnyRegisteredStation() {
        assertTrue(StationSelectionRules.canSelect(ControlMode.MANUAL, 0L, 9L, true, Set.of()));
        assertTrue(StationSelectionRules.canSelect(ControlMode.MANUAL, 3L, 9L, true, Set.of(1L, 2L)));
    }

    @Test void routeAutomaticTrainIsLimitedToItsLineStations() {
        assertTrue(StationSelectionRules.canSelect(ControlMode.ROUTE_AUTOMATIC, 3L, 2L, true, Set.of(1L, 2L)));
        assertFalse(StationSelectionRules.canSelect(ControlMode.ROUTE_AUTOMATIC, 3L, 9L, true, Set.of(1L, 2L)));
        assertFalse(StationSelectionRules.canSelect(ControlMode.ROUTE_AUTOMATIC, 0L, 2L, true, Set.of(2L)));
    }

    @Test void unknownStationIsAlwaysRejectedButClearingIsAllowed() {
        assertFalse(StationSelectionRules.canSelect(ControlMode.MANUAL, 0L, 9L, false, Set.of()));
        assertTrue(StationSelectionRules.canSelect(ControlMode.ROUTE_AUTOMATIC, 3L, 0L, false, Set.of()));
    }
}
