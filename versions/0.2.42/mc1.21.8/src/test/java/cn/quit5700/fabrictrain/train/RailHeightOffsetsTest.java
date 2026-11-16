package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailHeightOffsetsTest {
    @Test
    void connectedRailLookupChecksSameBelowAndAboveHeight() {
        BlockPos origin = new BlockPos(4, 20, -7);
        assertEquals(java.util.List.of(origin, origin.below(), origin.above()),
                RailHeightOffsets.candidates(origin));
    }
}
