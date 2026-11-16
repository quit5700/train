package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailwayManagerButtonContractTest {
    @Test void managerHasNoManualStopBindingModeOrOverlappingButton() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/RailwayManagerScreen.java"), StandardCharsets.UTF_8);
        String networking = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/RailwayManagerNetworking.java"), StandardCharsets.UTF_8);
        String item = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/item/LineSetterItem.java"), StandardCharsets.UTF_8);
        assertFalse(screen.contains("bindStop"));
        assertFalse(screen.contains("beginStopBinding"));
        assertFalse(networking.contains("BIND_STOP"));
        assertFalse(item.contains("STOP_BINDINGS"));
    }

    @Test void widgetsCancelDragCaptureBeforeTheirOwnClickHandlersRun() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/RailwayManagerScreen.java"), StandardCharsets.UTF_8);
        assertTrue(screen.contains("clearDragCapture()"));
        assertTrue(screen.indexOf("clearDragCapture();") < screen.indexOf("super.mouseClicked(mouseX, mouseY, button)"));
    }
}
