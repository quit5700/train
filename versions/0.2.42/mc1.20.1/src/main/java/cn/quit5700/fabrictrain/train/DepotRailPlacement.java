package cn.quit5700.fabrictrain.train;

import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseRailBlock;

/** The same top-face rail exception used by freight yards. */
public final class DepotRailPlacement {
    private DepotRailPlacement() {}
    public static boolean isTopRail(ItemStack stack, Direction face) {
        return face == Direction.UP && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof BaseRailBlock;
    }
}
