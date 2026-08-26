package cn.quit5700.fabrictrain.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.List;
import com.mojang.datafixers.util.Pair;

/** Minecraft 1.21.1 minecart helpers whose newer equivalents are not public in this version. */
public final class MinecartCompat {
    private MinecartCompat() {
    }

    public static BlockPos currentRailPos(AbstractMinecart cart) {
        BlockPos current = cart.blockPosition();
        if (BaseRailBlock.isRail(cart.level(), current)) return current;
        BlockPos below = current.below();
        if (BaseRailBlock.isRail(cart.level(), below)) return below;
        BlockPos above = current.above();
        if (BaseRailBlock.isRail(cart.level(), above)) return above;
        return current;
    }

    public static Pair<Vec3i, Vec3i> exits(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> Pair.of(new Vec3i(0, 0, -1), new Vec3i(0, 0, 1));
            case EAST_WEST -> Pair.of(new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0));
            case ASCENDING_EAST -> Pair.of(new Vec3i(-1, 0, 0), new Vec3i(1, 1, 0));
            case ASCENDING_WEST -> Pair.of(new Vec3i(-1, 1, 0), new Vec3i(1, 0, 0));
            case ASCENDING_NORTH -> Pair.of(new Vec3i(0, 1, -1), new Vec3i(0, 0, 1));
            case ASCENDING_SOUTH -> Pair.of(new Vec3i(0, 0, -1), new Vec3i(0, 1, 1));
            case SOUTH_EAST -> Pair.of(new Vec3i(0, 0, 1), new Vec3i(1, 0, 0));
            case SOUTH_WEST -> Pair.of(new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0));
            case NORTH_WEST -> Pair.of(new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0));
            case NORTH_EAST -> Pair.of(new Vec3i(0, 0, -1), new Vec3i(1, 0, 0));
        };
    }
}
