package cn.quit5700.stationaccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
public final class SetterItem extends Item {
 public SetterItem(Properties p){super(p);}
 @Override public boolean canDestroyBlock(ItemStack stack,net.minecraft.world.level.block.state.BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.LivingEntity user){return false;}
 @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer() instanceof ServerPlayer p)AccessNetwork.openSetter(p,c.getHand());return InteractionResult.SUCCESS;}
 @Override public InteractionResult use(Level level,Player p,InteractionHand hand){if(p instanceof ServerPlayer sp)AccessNetwork.openSetter(sp,hand);return InteractionResult.SUCCESS;}
}
