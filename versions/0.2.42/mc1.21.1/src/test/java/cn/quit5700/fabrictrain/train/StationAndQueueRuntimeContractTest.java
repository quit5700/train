package cn.quit5700.fabrictrain.train;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StationAndQueueRuntimeContractTest {
    @Test void controllerHardStopsAtStationAndReevaluatesDestroyedFrontTrainBeforeMovement() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("retireDestroyedTrainsMissingFromLevel(level, trains)"));
        assertTrue(source.contains("StationStopAlignment.position(stop)"));
        assertTrue(source.contains("motion.speed.stop()"));
        assertTrue(source.contains("motion.cursor.snapTo(stopPos)"));
        assertTrue(source.contains("FollowingTrainDecision.evaluate"));
    }
}
