package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UnloadUiContractTest {
    @Test void stationScreenNoLongerOffersManualUnloadSwitch() throws Exception {
        String source = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/DeviceSettingsScreen.java"), StandardCharsets.UTF_8);
        assertFalse(source.contains("manual_unload_on"));
        assertFalse(source.contains("manual_unload_off"));
    }

    @Test void cargoScreenContainsEmptyChoiceAndUnloadAllAction() throws Exception {
        String source = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/CargoSettingsScreen.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("cargo.inherit"));
        assertTrue(source.contains("cargo.destination_label"));
        assertTrue(source.contains("cargo.unload_all"));
        assertTrue(source.contains("UnloadAllPayload"));
    }

    @Test void stationPresenceCheckDoesNotAutoBindOrMutateLines() throws Exception {
        String source = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/cargo/StationPresenceService.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("line.stationStopTrack(index)"));
        assertFalse(source.contains("resolveStationStopTrack"));
    }

    @Test void locomotiveDestinationOnlyReachesTheServerFromTheSaveMethod() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/TrainClassScreen.java"), StandardCharsets.UTF_8);
        int saveMethod = source.indexOf("private void save()");
        int updatePacket = source.indexOf("new TrainClassNetworking.UpdatePayload");
        assertTrue(saveMethod >= 0);
        assertTrue(updatePacket > saveMethod,
                "选择车型或卸货站时不得立即修改车厢，必须点击保存后才发送更新");
        assertEquals(updatePacket, source.lastIndexOf("new TrainClassNetworking.UpdatePayload"));
    }
}
