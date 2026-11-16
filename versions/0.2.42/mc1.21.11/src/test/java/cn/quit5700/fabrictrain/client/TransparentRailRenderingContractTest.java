package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TransparentRailRenderingContractTest {
    @Test void transparentRailsUseCutoutLayer() throws Exception {
        String source = Files.readString(Path.of("src", "client", "java", "cn", "quit5700",
                "fabrictrain", "FabricTrainClient.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT"));
        assertTrue(source.contains("ModBlocks.SIGNAL_RAIL"));
        assertTrue(source.contains("ModBlocks.SWITCH_RAIL"));
    }
}
