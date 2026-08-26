package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailPositionResolverTest {
    @Test
    void recoveryLookupChecksCurrentBelowAndAboveWithoutChangingOrder() {
        BlockPos origin = new BlockPos(10, 64, -3);
        assertEquals(java.util.List.of(origin, origin.below(), origin.above()),
                RailPositionResolver.candidates(origin));
    }
}
