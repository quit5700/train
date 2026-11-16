package cn.quit5700.fabrictrain.item;

import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

public final class LocomotiveItem extends MinecartItem {
    public LocomotiveItem(EntityType<? extends AbstractMinecart> type,Properties properties){super(type,properties);}
    @Override public InteractionResult useOn(UseOnContext context){
        InteractionResult result=super.useOn(context);
        if(!context.getLevel().isClientSide()&&context.getPlayer()!=null){AABB area=new AABB(context.getClickedPos()).inflate(1.5);context.getLevel().getEntitiesOfClass(LocomotiveEntity.class,area).stream().filter(l->l.tickCount<3&&l.trainLink().parked()).min(java.util.Comparator.comparingDouble(l->l.distanceToSqr(context.getPlayer()))).ifPresent(l->l.setYRot(context.getPlayer().getYRot()));}
        return result;
    }
}
