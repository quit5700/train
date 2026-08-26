package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticDispatchContinuationContractTest {
    @Test
    void periodicDispatchUsesExplicitTimingRuleAndRecordsEveryAttempt() throws Exception {
        String service = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"), StandardCharsets.UTF_8);
        String depot = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/block/entity/TrainDepotBlockEntity.java"), StandardCharsets.UTF_8);

        assertTrue(service.contains("AutomaticDispatchSchedule.shouldAttempt"));
        assertTrue(service.contains("inventory.setLastAutomaticDispatchResult(result)"));
        assertTrue(service.contains("!inventory.creativeAutomaticDispatch()"));
        assertTrue(depot.contains("LastAutomaticDispatchResult"));
        assertTrue(depot.contains("CreativeAutomaticDispatch"));
    }

    @Test
    void depotScreenShowsWhyPeriodicDispatchCannotContinue() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/DepotSettingsScreen.java"), StandardCharsets.UTF_8);
        assertTrue(screen.contains("screen.fabric_train_simulator.depot.automatic_status"));
    }
}
