package cn.quit5700.fabrictrain.depot;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DepotAutomaticRouteContractTest {
    @Test
    void immediateDispatchAndLegacyDispatchApiBothCreateAutomaticTrains() throws Exception {
        String networking = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"), StandardCharsets.UTF_8);
        String dispatch = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"), StandardCharsets.UTF_8);

        assertTrue(networking.contains("action == DepotSettingsAction.DISPATCH_AUTOMATIC"));
        assertTrue(networking.contains("? ControlMode.ROUTE_AUTOMATIC : ControlMode.MANUAL"));
        int legacyStart = dispatch.indexOf("dispatchNowDetailed");
        int legacyEnd = dispatch.indexOf("dispatchConfigured", legacyStart);
        assertTrue(legacyStart >= 0 && legacyEnd > legacyStart);
        assertTrue(dispatch.substring(legacyStart, legacyEnd).contains("ControlMode.ROUTE_AUTOMATIC"));
    }

    @Test
    void automaticTrainMovementUsesTheValidatedRuntimePath() throws Exception {
        String dispatch = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/depot/DepotDispatchService.java"), StandardCharsets.UTF_8);
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);

        assertTrue(dispatch.contains("TrainControllerManager.registerAutomaticPath"));
        assertTrue(controller.contains("RailMotionCursor.createRoute"));
    }
}
