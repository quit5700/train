package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutomaticLineRefreshContractTest {
    @Test void immediateAutomaticDispatchRevalidatesThenReplacesOnlyItsLinesAutomaticTrains() throws Exception {
        String networking = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"), StandardCharsets.UTF_8);
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);
        String dispatch = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"), StandardCharsets.UTF_8);

        assertTrue(networking.contains("RuntimePathCache.remove(line.id())"));
        assertTrue(networking.contains("validateForImmediateDispatchDetailed"));
        assertTrue(networking.contains("automaticTrainIds(level, line.id())"));
        assertTrue(networking.contains("refreshAutomaticLine(level, line.id(), outcome.trainId(), previousAutomaticTrains)"));
        assertTrue(dispatch.contains("DispatchOutcome"));
        assertTrue(controller.contains("controlMode() == ControlMode.ROUTE_AUTOMATIC"));
        assertTrue(controller.contains("markTrainForDeletion"));
    }
    @Test void onlyResetCollectsAndReplacesExistingAutomaticTrains() throws Exception {
        String networking = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"), StandardCharsets.UTF_8);
        String dispatch = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"), StandardCharsets.UTF_8);

        assertTrue(networking.contains("previousAutomaticTrains = reset\n                ? TrainControllerManager.automaticTrainIds"));
        assertTrue(networking.contains("result.succeeded() && automatic"));
        assertTrue(networking.contains("if (reset) TrainControllerManager.refreshAutomaticLine"));
        assertTrue(dispatch.contains("long lineId=controlMode==ControlMode.ROUTE_AUTOMATIC&&line!=null?line.id():0L"));
        int dispatchStart = dispatch.indexOf("private static DispatchOutcome dispatch(");
        int spawnedStart = dispatch.indexOf("List<Entity> spawned", dispatchStart);
        assertFalse(dispatch.substring(dispatchStart, spawnedStart).contains("activeTrainCount"),
                "立即生成不得因为线路上已有列车记录而拒绝生成");
    }
}
