package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceSettingsFontLifecycleContractTest {
    @Test
    void editBoxesAreCreatedAfterScreenFontInitialization() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/client/DeviceSettingsScreen.java"),
                StandardCharsets.UTF_8);
        int constructor = source.indexOf("public DeviceSettingsScreen");
        int init = source.indexOf("@Override protected void init");
        String constructorBody = source.substring(constructor, init);
        String initBody = source.substring(init);

        assertFalse(constructorBody.contains("new EditBox"));
        assertTrue(initBody.contains("if (name == null)"));
        assertTrue(initBody.contains("new EditBox(font"));
    }
}
