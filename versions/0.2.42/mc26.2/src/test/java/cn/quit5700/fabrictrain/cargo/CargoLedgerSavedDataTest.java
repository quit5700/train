package cn.quit5700.fabrictrain.cargo;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoLedgerSavedDataTest {
    @Test
    void emptyLedgerSurvivesSerialization() {
        CargoLedgerSavedData original = new CargoLedgerSavedData();
        var encoded = CargoLedgerSavedData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        CargoLedgerSavedData restored = CargoLedgerSavedData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertTrue(restored.deliveries().isEmpty());
    }
}
