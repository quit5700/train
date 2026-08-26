package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotPauseContractTest {
    @Test void depotPauseUsesPersistentLineStatusAndNeverDispatches() throws Exception {
        String actions = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotSettingsAction.java"));
        String network = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"));
        assertTrue(actions.contains("TOGGLE_PAUSE"));
        assertTrue(network.contains("action == DepotSettingsAction.TOGGLE_PAUSE"));
        assertTrue(network.contains("LineStatus.PAUSED"));
        assertTrue(network.contains("continueAutomaticLine"));
    }

    @Test void depotScreenExposesPauseAndContinueWithoutSavingOtherFields() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/DepotSettingsScreen.java"));
        assertTrue(screen.contains("depot.pause"));
        assertTrue(screen.contains("depot.continue"));
        assertTrue(screen.contains("DepotSettingsAction.TOGGLE_PAUSE"));
    }
}
