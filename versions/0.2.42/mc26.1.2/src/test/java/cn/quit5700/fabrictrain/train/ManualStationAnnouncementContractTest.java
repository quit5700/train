package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualStationAnnouncementContractTest {
    @Test
    void lineLessManualTrainAnnouncesAStationOnceOnEntry() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"),
                StandardCharsets.UTF_8);

        assertTrue(controller.contains("if (operatingLine == null) processManualStationAnnouncement"));
        assertTrue(controller.contains("private static void processManualStationAnnouncement"));
        assertTrue(controller.contains("current.id() == progress.lastDeviceId"));
        assertTrue(controller.contains("broadcastStationAnnouncement(level, current)"));
    }
}
