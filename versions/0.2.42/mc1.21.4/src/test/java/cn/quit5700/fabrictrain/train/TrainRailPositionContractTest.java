package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainRailPositionContractTest {
    @Test
    void derailAndPowerChecksUseCursorRailWithThreeHeightFallback() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);
        assertTrue(controller.contains("motion.cursor.currentRail()"));
        assertTrue(controller.contains("RailPositionResolver.resolve"));
        assertTrue(controller.contains("RailPowerService.isPowered(level, actualRail)"));
    }
}
