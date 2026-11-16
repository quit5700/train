package cn.quit5700.fabrictrain.cargo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CargoUnloadRulesTest {
    @Test void carriageDestinationOverridesLocomotiveDefault() {
        assertEquals(9L, CargoUnloadRules.effectiveDestination(9L, 4L));
    }

    @Test void emptyCarriageDestinationInheritsLocomotiveDefault() {
        assertEquals(4L, CargoUnloadRules.effectiveDestination(0L, 4L));
    }

    @Test void bothDestinationsMayBeEmpty() {
        assertEquals(0L, CargoUnloadRules.effectiveDestination(0L, 0L));
        assertFalse(CargoUnloadRules.shouldUnloadAt(3L, 0L, 0L));
    }

    @Test void cargoOnlyUnloadsAtItsEffectiveDestination() {
        assertTrue(CargoUnloadRules.shouldUnloadAt(9L, 9L, 4L));
        assertFalse(CargoUnloadRules.shouldUnloadAt(4L, 9L, 4L));
        assertTrue(CargoUnloadRules.shouldUnloadAt(4L, 0L, 4L));
        assertFalse(CargoUnloadRules.shouldUnloadAt(3L, 0L, 4L));
    }
}
