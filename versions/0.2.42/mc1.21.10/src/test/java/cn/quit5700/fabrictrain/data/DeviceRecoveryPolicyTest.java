package cn.quit5700.fabrictrain.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceRecoveryPolicyTest {
    @Test void healthySavedRegistryIsAuthoritative() {
        assertFalse(DeviceRecoveryPolicy.shouldScanLegacyBlocks(1, 0));
        assertFalse(DeviceRecoveryPolicy.shouldScanLegacyBlocks(0, 1));
        assertFalse(DeviceRecoveryPolicy.shouldScanLegacyBlocks(3, 2));
    }

    @Test void emptyLegacySaveMayRecoverBlocks() {
        assertTrue(DeviceRecoveryPolicy.shouldScanLegacyBlocks(0, 0));
    }
}
