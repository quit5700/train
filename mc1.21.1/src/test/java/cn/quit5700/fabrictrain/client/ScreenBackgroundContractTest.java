package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenBackgroundContractTest {
    private static final List<String> SCREENS = List.of("CargoSettingsScreen", "DepotSettingsScreen",
            "DeviceSettingsScreen", "FormationScreen", "LineSettingsScreen", "RailwayManagerScreen",
            "TrainClassScreen", "TrainListScreen");

    @Test void customScreensDimWithoutBlurringTheirOwnContents() throws Exception {
        for (String screen : SCREENS) {
            String source = Files.readString(Path.of("src", "client", "java", "cn", "quit5700",
                    "fabrictrain", "client", screen + ".java"), StandardCharsets.UTF_8);
            assertTrue(source.contains("renderTransparentBackground(graphics)"), screen);
        }
    }
}
