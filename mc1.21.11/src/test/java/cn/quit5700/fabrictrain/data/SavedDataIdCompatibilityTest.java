package cn.quit5700.fabrictrain.data;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SavedDataIdCompatibilityTest {
    @Test void savedDataUsesLegacyWindowsSafeNames() throws Exception {
        for (String file : List.of("data/RailwaySavedData.java", "train/OfflineTrainSavedData.java",
                "cargo/CargoLedgerSavedData.java")) {
            String source = Files.readString(Path.of("src", "main", "java", "cn", "quit5700",
                    "fabrictrain").resolve(file), StandardCharsets.UTF_8);
            assertTrue(source.contains("FabricTrainMod.MOD_ID + \"_"), file);
            assertFalse(source.contains("FabricTrainMod.id("), file);
        }
    }
}
