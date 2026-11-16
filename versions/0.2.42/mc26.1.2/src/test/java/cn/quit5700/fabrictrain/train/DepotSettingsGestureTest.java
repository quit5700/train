package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class DepotSettingsGestureTest {
    @AfterEach
    void resetClientOpener() {
        DepotSettingsGesture.installClientOpener(ignored -> false);
    }

    @Test
    void nativeInteractionDelegatesOnlyToInstalledClientOpener() {
        AtomicReference<BlockPos> opened = new AtomicReference<>();
        BlockPos depot = new BlockPos(4, -61, 7);
        DepotSettingsGesture.installClientOpener(pos -> {
            opened.set(pos);
            return true;
        });
        assertTrue(DepotSettingsGesture.tryOpen(depot));
        assertEquals(depot, opened.get());
    }

    @Test
    void nonControlGestureDoesNotConsumeNormalUse() {
        DepotSettingsGesture.installClientOpener(ignored -> false);
        assertFalse(DepotSettingsGesture.tryOpen(BlockPos.ZERO));
    }
}
