package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsShortcutContractTest {
    @Test void sneakRightClickIsTheOnlyDirectSettingsGestureForAllThreeBlocks() throws Exception {
        String mixin = read("src/client/java/cn/quit5700/fabrictrain/mixin/client/MinecraftUseItemMixin.java");
        String station = read("src/main/java/cn/quit5700/fabrictrain/block/DeviceBlock.java");
        String railwaySwitch = read("src/main/java/cn/quit5700/fabrictrain/block/DeviceRailBlock.java");
        String networking = read("src/main/java/cn/quit5700/fabrictrain/network/DeviceSettingsNetworking.java");
        String globalInteraction = read("src/main/java/cn/quit5700/fabrictrain/train/DeviceInteractionService.java");

        assertTrue(mixin.contains("isShiftKeyDown()"));
        assertFalse(mixin.contains("KEY_LCONTROL"));
        assertTrue(mixin.contains("ModBlocks.TRAIN_DEPOT"));
        assertTrue(mixin.contains("ModBlocks.STATION_MARKER"));
        assertTrue(mixin.contains("ModBlocks.SWITCH_RAIL"));
        assertTrue(mixin.contains("DeviceSettingsNetworking.RequestOpenPayload"));
        assertTrue(networking.contains("distanceToSqr"));
        assertFalse(station.contains("player.isShiftKeyDown()"));
        assertFalse(railwaySwitch.contains("player.isShiftKeyDown()"));
        assertFalse(globalInteraction.contains("DeviceSettingsNetworking.open"));
        assertFalse(globalInteraction.contains("ModBlocks.STATION_MARKER"));
        assertFalse(globalInteraction.contains("ModBlocks.SWITCH_RAIL"));
    }

    @Test void joiningAWorldShowsTheTranslatedShortcutHint() throws Exception {
        String initializer = read("src/main/java/cn/quit5700/fabrictrain/FabricTrainMod.java");
        assertTrue(initializer.contains("ServerPlayConnectionEvents.JOIN"));
        assertTrue(initializer.contains("message.fabric_train_simulator.settings_shortcut_hint"));
        for (String language : new String[]{"en_us", "zh_cn", "zh_tw", "ja_jp", "ko_kr", "de_de", "fr_fr", "es_es"}) {
            String json = read("src/main/resources/assets/fabric_train_simulator/lang/" + language + ".json");
            assertTrue(json.contains("message.fabric_train_simulator.settings_shortcut_hint"), language);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
