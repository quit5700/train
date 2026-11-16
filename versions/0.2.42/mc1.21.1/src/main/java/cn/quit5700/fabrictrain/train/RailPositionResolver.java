package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;

import java.util.List;

/**
 * Resolves the actual rail under a logically moved train.  A slope interpolation can place the
 * entity block position one block below the upper rail, so vanilla's current/below lookup is only
 * a recovery hint and must also consider the block above.
 */
public final class RailPositionResolver {
    private RailPositionResolver() {}

    public static List<BlockPos> candidates(BlockPos origin) {
        return List.of(origin, origin.below(), origin.above());
    }

    public static BlockPos resolve(ServerLevel level, AbstractMinecart minecart) {
        if (level == null || minecart == null) return null;
        return resolve(level, cn.quit5700.fabrictrain.compat.MinecartCompat.currentRailPos(minecart));
    }

    public static BlockPos resolve(ServerLevel level, BlockPos origin) {
        if (level == null || origin == null) return null;
        return candidates(origin).stream().filter(candidate -> BaseRailBlock.isRail(level, candidate))
                .findFirst().orElse(null);
    }
}

