package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RailwayManagerScrollingContractTest {
    @Test void allThreeColumnsHaveIndependentWheelAndDraggableScrollbarState() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/RailwayManagerScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(screen.contains("leftScroll"));
        assertTrue(screen.contains("middleScroll"));
        assertTrue(screen.contains("rightScroll"));
        assertTrue(screen.contains("mouseScrolled"));
        assertTrue(screen.contains("mouseDragged"));
        assertTrue(screen.contains("renderScrollbar(graphics, 0"));
        assertTrue(screen.contains("renderScrollbar(graphics, 1"));
        assertTrue(screen.contains("renderScrollbar(graphics, 2"));
        assertTrue(screen.contains("leftDevices().get(leftScroll + row)"));
        assertTrue(screen.contains("lines.get(middleScroll + row)"));
        assertTrue(screen.contains("depots().get(rightScroll + row)"));
    }
}
