package cn.quit5700.fabrictrain.cargo;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class CargoUnloadPersistenceContractTest {
    @Test
    void successfulUnloadNeverClearsTheConfiguredCarDestination() throws Exception {
        String destinationService = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/cargo/DestinationUnloadService.java"), StandardCharsets.UTF_8);
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);

        assertFalse(destinationService.contains("cargo.setDestinationStationId(0L)"));
        assertFalse(controller.contains("if (cargo.isEmpty()) cargo.setDestinationStationId(0L)"));
    }

    @Test
    void everyUnloadPathAcceptsAnEmptyDestinationSlot() throws Exception {
        String transfer = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/cargo/CargoTransferService.java"), StandardCharsets.UTF_8);

        assertFalse(transfer.contains("destinations.stream().noneMatch(container -> containsMatching(container, source))"));
    }
}
