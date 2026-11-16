package cn.quit5700.stationaccess;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.material.PushReaction;
public final class BoundaryBlock extends Block {
 public BoundaryBlock(Properties p){super(p);}
 @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
 @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter level,BlockPos pos){return Shapes.empty();}
 @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
}
