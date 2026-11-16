package cn.quit5700.stationaccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
public final class TerminalBlock extends HorizontalDirectionalBlock {
 public TerminalBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
 @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player p,BlockHitResult hit){if(p instanceof ServerPlayer sp)AccessNetwork.openTerminal(sp,pos);return InteractionResult.SUCCESS;}
 @Override protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(stack.is(StationAccess.SETTER))return InteractionResult.PASS;if(p instanceof ServerPlayer sp)AccessNetwork.openTerminal(sp,pos);return InteractionResult.SUCCESS;}
}
