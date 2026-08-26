package cn.quit5700.fabrictrain.network;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmediateDispatchSwitchSyncContractTest {
    @Test void dispatchAndSettingsUseTheSameSwitchEntrySynchronizer() throws Exception {
        String depot = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"), StandardCharsets.UTF_8);
        String settings = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DeviceSettingsNetworking.java"), StandardCharsets.UTF_8);
        assertTrue(depot.indexOf("SwitchEntryStateSynchronizer.synchronizeLine")
                < depot.indexOf("validateForImmediateDispatchDetailed"));
        assertTrue(settings.contains("SwitchEntryStateSynchronizer.synchronizeDevice"));
    }
}
