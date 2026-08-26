package cn.quit5700.fabrictrain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TrainDepotFacingContractTest {
    @Test void depotFrontRotatesTowardThePlacingPlayer() throws Exception {
        String block = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/block/TrainDepotBlock.java"), StandardCharsets.UTF_8);
        String states = Files.readString(Path.of("src/main/resources/assets/fabric_train_simulator/blockstates/train_depot.json"), StandardCharsets.UTF_8);
        assertTrue(block.contains("HORIZONTAL_FACING"));
        assertTrue(block.contains("getStateForPlacement"));
        assertTrue(block.contains("createBlockStateDefinition"));
        for (String direction : new String[]{"north", "east", "south", "west"}) assertTrue(states.contains("facing=" + direction), direction);
    }
}
