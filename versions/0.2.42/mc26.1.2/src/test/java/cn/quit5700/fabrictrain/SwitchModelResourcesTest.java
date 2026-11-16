package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SwitchModelResourcesTest {
    private static final Path MODELS = Path.of("src", "main", "resources", "assets",
            "fabric_train_simulator", "models", "block");

    @Test
    void switchModelsNeverUseStationLikeDetectorOrActivatorRailTextures() throws IOException {
        for (String name : new String[]{"switch_rail.json", "switch_rail_curved.json",
                "switch_rail_raised_ne.json", "switch_rail_raised_sw.json"}) {
            String json = Files.readString(MODELS.resolve(name), StandardCharsets.UTF_8);
            assertFalse(json.contains("detector_rail"), name);
            assertFalse(json.contains("activator_rail"), name);
        }
    }

    @Test
    void flatSwitchModelLooksLikeFourWayCrossroads() throws IOException {
        String json = Files.readString(MODELS.resolve("switch_rail.json"), StandardCharsets.UTF_8);
        assertTrue(json.contains("fabric_train_simulator:block/switch_rail_base"),
                "missing confirmed crossroads texture");
        assertTrue(json.contains("\"elements\""), "switch must have dedicated geometry");
    }

    @Test
    void selectedRouteUsesConfirmedDirectionalTexturesForEveryEntry() throws IOException {
        for (String name : new String[]{"switch_rail_left.json", "switch_rail_front.json", "switch_rail_right.json"}) {
            String json = Files.readString(MODELS.resolve(name), StandardCharsets.UTF_8);
            assertTrue(json.contains("fabric_train_simulator:block/" + name.substring(0, name.length() - 5)), name);
        }
        String blockState = Files.readString(Path.of("src", "main", "resources", "assets",
                "fabric_train_simulator", "blockstates", "switch_rail.json"), StandardCharsets.UTF_8);
        for (String direction : new String[]{"north", "east", "south", "west"}) {
            for (int choice = 0; choice < 3; choice++) {
                assertTrue(blockState.contains("entry_direction=" + direction + ",switch_choice=" + choice),
                        direction + ":" + choice);
            }
        }
    }
}
