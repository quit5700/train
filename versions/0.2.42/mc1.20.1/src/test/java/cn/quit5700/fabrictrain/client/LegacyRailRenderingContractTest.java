package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyRailRenderingContractTest {
    @Test
    void transparentRailsUseTheCutoutRenderLayer() throws Exception {
        String client = Files.readString(Path.of(
                "src/client/java/cn/quit5700/fabrictrain/FabricTrainClient.java"),
                StandardCharsets.UTF_8);

        assertTrue(client.contains("BlockRenderLayerMap.INSTANCE.putBlocks"));
        assertTrue(client.contains("RenderType.cutout()"));
        assertTrue(client.contains("ModBlocks.SIGNAL_RAIL"));
        assertTrue(client.contains("ModBlocks.SWITCH_RAIL"));
    }
}
