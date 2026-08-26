package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrainRegistryRulesTest {
    @Test void manualAndRouteAutomaticTrainsCannotMerge() {
        assertFalse(TrainRegistryRules.canMerge(ControlMode.MANUAL, ControlMode.ROUTE_AUTOMATIC));
        assertFalse(TrainRegistryRules.canMerge(ControlMode.ROUTE_AUTOMATIC, ControlMode.MANUAL));
        assertTrue(TrainRegistryRules.canMerge(ControlMode.MANUAL, ControlMode.MANUAL));
        assertTrue(TrainRegistryRules.canMerge(ControlMode.ROUTE_AUTOMATIC, ControlMode.ROUTE_AUTOMATIC));
    }

    @Test void sideContainingLocomotiveInheritsNumber() {
        assertEquals(0, TrainRegistryRules.inheritedSide(true, false));
        assertEquals(1, TrainRegistryRules.inheritedSide(false, true));
        assertEquals(0, TrainRegistryRules.inheritedSide(false, false));
    }
}
