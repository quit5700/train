package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;

import java.util.Objects;
import java.util.function.Predicate;

public final class DepotSettingsGesture {
    private static volatile Predicate<BlockPos> clientOpener = ignored -> false;

    private DepotSettingsGesture() {}

    public static void installClientOpener(Predicate<BlockPos> opener) {
        clientOpener = Objects.requireNonNull(opener, "opener");
    }

    public static boolean tryOpen(BlockPos pos) {
        return clientOpener.test(pos);
    }
}
