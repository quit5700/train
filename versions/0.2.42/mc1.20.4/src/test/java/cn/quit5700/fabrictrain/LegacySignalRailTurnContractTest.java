package cn.quit5700.fabrictrain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LegacySignalRailTurnContractTest {
    @Test void signalRailUsesVanillaFlexibleRailAndCurvedModel() throws Exception {
        String block = Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/block/SignalRailBlock.java"), StandardCharsets.UTF_8);
        String states = Files.readString(Path.of("src/main/resources/assets/fabric_train_simulator/blockstates/signal_rail.json"), StandardCharsets.UTF_8);
        String curve = Files.readString(Path.of("src/main/resources/assets/fabric_train_simulator/models/block/signal_rail_curved.json"), StandardCharsets.UTF_8);
        assertTrue(block.contains("extends RailBlock"));
        for (String shape : new String[]{"south_east", "south_west", "north_west", "north_east"}) assertTrue(states.contains("shape=" + shape), shape);
        assertTrue(curve.contains("minecraft:block/rail_curved"));
    }
}
