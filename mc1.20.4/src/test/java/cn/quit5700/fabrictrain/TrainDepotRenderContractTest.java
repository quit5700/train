package cn.quit5700.fabrictrain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TrainDepotRenderContractTest {
    @Test void blockEntityExplicitlyUsesTheBakedBlockModel() throws Exception {
        String source = Files.readString(
                Path.of("src/main/java/cn/quit5700/fabrictrain/block/TrainDepotBlock.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("RenderShape.MODEL"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/fabric_train_simulator/models/block/train_depot.json")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/assets/fabric_train_simulator/blockstates/train_depot.json")));
    }
}
