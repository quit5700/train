package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.block.entity.PowerGeneratorBlockEntity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class PowerGeneratorFuelTest {
    @Test void redstoneDustBurnsForOneRealHourAtTwentyTicksPerSecond() {
        assertEquals(60L * 60L * 20L, PowerGeneratorBlockEntity.REDSTONE_DUST_TICKS);
    }
    @Test void redstoneBlockBurnsForSevenRealHoursAtTwentyTicksPerSecond() {
        assertEquals(7L * 60L * 60L * 20L, PowerGeneratorBlockEntity.REDSTONE_BLOCK_TICKS);
    }
}
