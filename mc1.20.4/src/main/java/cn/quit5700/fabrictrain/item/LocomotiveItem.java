package cn.quit5700.fabrictrain.item;

import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public final class LocomotiveItem extends Item {
    private final EntityType<? extends AbstractMinecart> type;

    public LocomotiveItem(EntityType<? extends AbstractMinecart> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!BaseRailBlock.isRail(state)) return InteractionResult.FAIL;
        if (!context.getLevel().isClientSide()) {
            AbstractMinecart minecart = type.create(context.getLevel());
            if (minecart == null) return InteractionResult.FAIL;
            RailShape shape = state.getBlock() instanceof BaseRailBlock rail
                    ? state.getValue(rail.getShapeProperty()) : RailShape.NORTH_SOUTH;
            double yOffset = shape.isAscending() ? 0.5D : 0.0625D;
            minecart.setPos(context.getClickedPos().getX() + 0.5D,
                    context.getClickedPos().getY() + yOffset,
                    context.getClickedPos().getZ() + 0.5D);
            ItemStack stack = context.getItemInHand();
            context.getLevel().addFreshEntity(minecart);
            stack.shrink(1);
            if (minecart instanceof LocomotiveEntity locomotive && context.getPlayer() != null) {
                locomotive.setYRot(context.getPlayer().getYRot());
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
}
