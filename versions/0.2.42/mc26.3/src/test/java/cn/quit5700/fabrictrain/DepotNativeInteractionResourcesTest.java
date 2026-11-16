package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class DepotNativeInteractionResourcesTest {
    private static final Path ROOT = Path.of("src/main");

    @Test
    void clientMixinInterceptsNativeUseBeforeFabricInteractionCallbacks() throws Exception {
        String modJson = Files.readString(ROOT.resolve("resources/fabric.mod.json"), StandardCharsets.UTF_8);
        Path config = ROOT.resolve("resources/fabric_train_simulator.client.mixins.json");
        Path source = Path.of("src/client/java/cn/quit5700/fabrictrain/mixin/client/MinecraftUseItemMixin.java");

        assertTrue(modJson.contains("fabric_train_simulator.client.mixins.json"));
        assertTrue(Files.isRegularFile(config));
        assertTrue(Files.isRegularFile(source));
        String mixin = Files.readString(source, StandardCharsets.UTF_8);
        assertTrue(mixin.contains("method = \"startUseItem\""));
        assertTrue(mixin.contains("priority = 2000"));
    }
}
