package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DepotSettingsActionContractTest {
    @Test void actionsAreSeparatedAndAutomaticTickIsExplicitlyEnabled() throws Exception {
        String networking = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"));
        String dispatch = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"));
        assertTrue(networking.contains("boolean automatic = saving || reset"));
        assertTrue(networking.contains("reset ? 0L : data.activeAutomaticTrainCount(payload.lineId())"));
        assertTrue(networking.contains("saving && (payload.lineId() == 0L || depot.maximumTrains() == 0)"));
        assertTrue(networking.contains("action == DepotSettingsAction.DISPATCH_AUTOMATIC"));
        assertTrue(networking.contains("? ControlMode.ROUTE_AUTOMATIC : ControlMode.MANUAL"));
        assertTrue(networking.contains("setAutomaticDispatchEnabled(true)"));
        assertTrue(dispatch.contains("AutomaticDispatchSchedule.shouldAttempt"));
    }

    @Test void immediateDispatchUsesDetailedPrecheckAndAutomaticValidation() throws Exception {
        String networking = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"));
        assertTrue(networking.contains("DepotDispatchPrecheck.check"));
        assertTrue(networking.contains("validateForImmediateDispatch"));
        assertTrue(networking.contains("maximum_reached"));
        assertTrue(networking.contains("first_node_not_found"));
        assertTrue(networking.contains("track_broken"));
    }

    @Test void everyDepotActionKeepsTheScreenOpenAndReturnsVisibleStatus() throws Exception {
        String screen = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/DepotSettingsScreen.java"));
        String networking = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"));

        assertFalse(screen.contains("if (action == DepotSettingsAction.SAVE) onClose()"));
        assertTrue(screen.contains("payload.operationMessageKey()"));
        assertTrue(screen.contains("depot.operation_result"));
        assertTrue(networking.contains("String operationMessageKey"));
        assertTrue(networking.contains("message.fabric_train_simulator.depot.saved"));
    }

    @Test void immediateActionsPersistTheWindowDraftBeforeSpawning() throws Exception {
        String networking = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"));
        int actions = networking.indexOf("private static void update(");
        int save = networking.indexOf("saveSettings(player, payload", actions);
        int dispatch = networking.indexOf("dispatchConfiguredOutcome", actions);
        assertTrue(actions >= 0 && save > actions && dispatch > save,
                "立即生成和立即发车必须先保存车辆段窗口草稿");
    }
}
