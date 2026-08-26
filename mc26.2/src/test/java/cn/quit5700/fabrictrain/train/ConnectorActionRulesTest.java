package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static cn.quit5700.fabrictrain.train.ConnectorActionRules.Action.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectorActionRulesTest {
    @Test void clickingTheSameVehicleOnlyCancelsSelection() {
        assertEquals(CANCEL_SELECTION, ConnectorActionRules.decide(true, true, 2, 2));
    }

    @Test void adjacentVehiclesInTheSameFormationAreDisconnected() {
        assertEquals(DISCONNECT_ADJACENT, ConnectorActionRules.decide(false, true, 2, 3));
    }

    @Test void nonAdjacentVehiclesInTheSameFormationAreRejected() {
        assertEquals(REJECT_NON_ADJACENT, ConnectorActionRules.decide(false, true, 1, 3));
    }

    @Test void vehiclesFromDifferentFormationsAreConnected() {
        assertEquals(CONNECT, ConnectorActionRules.decide(false, false, 0, 0));
    }
}
