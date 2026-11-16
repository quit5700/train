package cn.quit5700.fabrictrain.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ScreenFontLifecycleContractTest {
    @Test void legacyScreenConstructorsDoNotCreateEditBoxesBeforeFontInitialization() throws Exception {
        for (String screen : new String[]{"DeviceSettingsScreen", "DepotSettingsScreen", "LineSettingsScreen"}) {
            String source = Files.readString(Path.of("src/client/java/cn/quit5700/fabrictrain/client/" + screen + ".java"), StandardCharsets.UTF_8);
            int constructor = source.indexOf("public " + screen + "(");
            int init = source.indexOf("protected void init()", constructor);
            assertTrue(constructor >= 0 && init > constructor, screen);
            assertFalse(source.substring(constructor, init).contains("new EditBox"), screen);
            assertTrue(source.substring(init).contains("new EditBox"), screen);
        }
    }
}
