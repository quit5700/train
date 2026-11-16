package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryPresentationResourcesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/fabric_train_simulator");

    @Test
    void everyRegisteredBlockItemHasAnItemTranslation() throws Exception {
        for (String locale : List.of("en_us", "zh_cn", "zh_tw", "ja_jp", "ko_kr", "de_de", "fr_fr", "es_es")) {
            String json = Files.readString(ASSETS.resolve("lang/" + locale + ".json"), StandardCharsets.UTF_8);
            for (String id : List.of("signal_rail", "switch_rail", "train_depot", "station_marker", "power_generator")) {
                assertTrue(json.contains("\"item.fabric_train_simulator." + id + "\""), locale + ":" + id);
            }
        }
    }

    @Test
    void thinRailsUseConfirmedFlatInventoryTextures() throws Exception {
        String signalRail = Files.readString(ASSETS.resolve("models/item/signal_rail.json"), StandardCharsets.UTF_8);
        assertTrue(signalRail.contains("minecraft:item/generated"));
        assertTrue(signalRail.contains("fabric_train_simulator:block/signal_rail"));

        String switchRail = Files.readString(ASSETS.resolve("models/item/switch_rail.json"), StandardCharsets.UTF_8);
        assertTrue(switchRail.contains("minecraft:item/generated"));
        assertTrue(switchRail.contains("fabric_train_simulator:item/switch_rail_all_lit"));
        assertTrue(Files.exists(ASSETS.resolve("textures/item/switch_rail_all_lit.png")));
    }
}
