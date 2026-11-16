package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogicalVehicleTickPolicyTest {
    @Test
    void clientMustKeepPositionsReceivedFromMinecartInterpolation() {
        assertFalse(LogicallyControlledVehicle.shouldRestorePosition(true, true, false));
        assertFalse(LogicallyControlledVehicle.shouldRestorePosition(true, false, true));
    }

    @Test
    void serverRemainsTheOnlyLogicalMovementAuthority() {
        assertTrue(LogicallyControlledVehicle.shouldRestorePosition(false, true, false));
        assertTrue(LogicallyControlledVehicle.shouldRestorePosition(false, false, true));
        assertFalse(LogicallyControlledVehicle.shouldRestorePosition(false, false, false));
    }
}
