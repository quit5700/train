package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorRulesTest {
    @Test void connectorAcceptsSixteenBlocksButRejectsBeyondIt() {
        assertTrue(ConnectorRules.withinRange(256.0D));
        assertFalse(ConnectorRules.withinRange(256.01D));
    }

    @Test void mergedFormationCannotExceedLocomotivePlusOneHundredCars() {
        assertTrue(ConnectorRules.canMerge(50, 51));
        assertFalse(ConnectorRules.canMerge(51, 51));
    }
}
