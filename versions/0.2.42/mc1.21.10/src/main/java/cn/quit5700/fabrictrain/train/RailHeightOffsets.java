package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;

import java.util.List;

public final class RailHeightOffsets {
    private RailHeightOffsets() {}

    public static List<BlockPos> candidates(BlockPos position) {
        return List.of(position, position.below(), position.above());
    }
}
