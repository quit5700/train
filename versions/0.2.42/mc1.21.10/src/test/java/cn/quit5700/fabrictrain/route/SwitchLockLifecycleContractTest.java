package cn.quit5700.fabrictrain.route;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchLockLifecycleContractTest {
    @Test void controllerUsesTickLocalTrafficRightsInsteadOfPersistentLocks() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"));
        assertTrue(source.contains("rebuildRealtimeSwitchRights(level, trains)"));
        assertTrue(source.contains("REALTIME_SWITCH_RIGHTS"));
        assertTrue(source.contains("\"waiting_switch\""));
        assertFalse(source.contains("SwitchLockManager"));
        assertFalse(source.contains("HELD_SWITCHES"));
        assertFalse(source.contains("restoreManualSwitchChoice(level, key)"));
    }
}
