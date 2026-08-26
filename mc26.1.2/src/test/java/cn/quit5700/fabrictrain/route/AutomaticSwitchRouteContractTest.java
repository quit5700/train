package cn.quit5700.fabrictrain.route;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticSwitchRouteContractTest {
    @Test
    void switchScreenOnlyExposesManualLeftStraightRightChoice() throws Exception {
        String screen = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/DeviceSettingsScreen.java"), StandardCharsets.UTF_8);

        assertFalse(screen.contains("entryButton("));
        assertFalse(screen.contains("screen.fabric_train_simulator.switch.entry"));
        assertTrue(screen.contains("screen.fabric_train_simulator.switch.left"));
        assertTrue(screen.contains("screen.fabric_train_simulator.switch.straight"));
        assertTrue(screen.contains("screen.fabric_train_simulator.switch.right"));
    }

    @Test
    void automaticTrainUsesHiddenCachedRouteWithoutChangingManualSwitchAppearance() throws Exception {
        String controller = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/train/TrainControllerManager.java"), StandardCharsets.UTF_8);

        assertTrue(controller.contains("routeShapeAt(pos)"));
        assertFalse(controller.contains("setValue(DeviceRailBlock.SWITCH_CHOICE, choice)"));
        assertFalse(controller.contains("restoreManualSwitchChoice(level, key)"));
        assertTrue(controller.contains("shapeConnected(level, pos, shape.get())"));
    }
}
