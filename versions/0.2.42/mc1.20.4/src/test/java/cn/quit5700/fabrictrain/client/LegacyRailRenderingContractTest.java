package cn.quit5700.fabrictrain.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LegacyRailRenderingContractTest {
    @Test
    void transparentRailTexturesUseCutoutRenderLayer() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/FabricTrainClient.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("BlockRenderLayerMap.INSTANCE.putBlocks"));
        assertTrue(source.contains("RenderType.cutout()"));
        assertTrue(source.contains("ModBlocks.SIGNAL_RAIL"));
        assertTrue(source.contains("ModBlocks.SWITCH_RAIL"));
    }
}
