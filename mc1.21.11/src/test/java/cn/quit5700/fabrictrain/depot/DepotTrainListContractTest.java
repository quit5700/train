package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotTrainListContractTest {
    @Test
    void depotScreenRequestsServerOwnedCurrentTrainRegistry() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/DepotSettingsScreen.java"), StandardCharsets.UTF_8);
        String networking = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"), StandardCharsets.UTF_8);

        assertTrue(screen.contains("screen.fabric_train_simulator.depot.train_list"));
        assertTrue(screen.contains("new DepotNetworking.RequestTrainListPayload(depotPos)"));
        assertTrue(screen.contains("trainListButton.active = lineIndex >= 0"));
        assertTrue(networking.contains("depot.selectedLineId()"));
        assertTrue(networking.contains("data.presentDepotTrainRecords(lineId).values()"));
        assertTrue(networking.contains("record TrainListEntry(java.util.UUID trainId, long number, boolean automatic)"));
        assertTrue(networking.contains("record TrainListPayload(BlockPos depotPos"));
        assertTrue(networking.contains("record DeleteTrainPayload"));
        assertTrue(networking.contains("deleteTrainEverywhere"));
    }

    @Test
    void clientHasScrollableTrainListScreenAndReceiver() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/TrainListScreen.java"), StandardCharsets.UTF_8);
        String client = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/FabricTrainClient.java"), StandardCharsets.UTF_8);

        assertTrue(screen.contains("mouseScrolled"));
        assertTrue(screen.contains("screen.fabric_train_simulator.train_list.manual"));
        assertTrue(screen.contains("screen.fabric_train_simulator.train_list.automatic"));
        assertTrue(client.contains("DepotNetworking.TrainListPayload.ID"));
        assertTrue(client.contains("new TrainListScreen(payload)"));
        assertTrue(screen.contains("new DepotNetworking.RequestOpenPayload(depotPos)"));
        assertTrue(screen.contains("screen.fabric_train_simulator.train_list.back"));
        assertTrue(screen.contains("screen.fabric_train_simulator.train_list.delete"));
        assertTrue(screen.contains("new DepotNetworking.DeleteTrainPayload"));
        assertTrue(screen.contains("new DepotNetworking.RequestTrainListPayload(depotPos)"));
        assertTrue(screen.contains("refreshTicks"));
    }
}
