package cn.quit5700.fabrictrain.route;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchEntryPersistenceContractTest {
    @Test
    void serverDoesNotPeriodicallyReplaceEntryWithNearestDepot() throws Exception {
        String mod = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/FabricTrainMod.java"));
        assertFalse(mod.contains("SwitchEntryDirectionService::tick"));
    }

    @Test
    void switchEntryTravelsThroughSavedDataAndSettingsPayload() throws Exception {
        String device = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/data/RailwayDevice.java"));
        String network = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/network/DeviceSettingsNetworking.java"));
        assertTrue(device.contains("switchEntryOrdinal"));
        assertTrue(network.contains("switchEntryOrdinal"));
    }
}
