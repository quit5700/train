package cn.quit5700.fabrictrain.data;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
/** Passive observation must not create or renew Minecraft chunk tickets. */
public final class LoadedRailWorld {
 private LoadedRailWorld(){}
 public static BlockState state(ServerLevel level,BlockPos pos){var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(pos);}
 public static BlockEntity entity(ServerLevel level,BlockPos pos){var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);return chunk==null?null:chunk.getBlockEntity(pos);}
}
