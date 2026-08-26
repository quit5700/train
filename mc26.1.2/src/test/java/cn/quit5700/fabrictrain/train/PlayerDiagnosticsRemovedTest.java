package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class PlayerDiagnosticsRemovedTest {
    @Test void developmentDiagnosticsAreNotSentToPlayers() throws Exception {
        String controller = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"));
        String connector = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/train/ConnectorInteractionService.java"));
        assertFalse(controller.contains("message.fabric_train_simulator.diagnostic.input"));
        assertFalse(controller.contains("message.fabric_train_simulator.diagnostic.motion"));
        assertFalse(connector.contains("message.fabric_train_simulator.diagnostic.connection"));
    }
}
