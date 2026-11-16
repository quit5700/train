package cn.quit5700.stationaccess.client;
import cn.quit5700.stationaccess.*;
import cn.quit5700.stationaccess.core.Box;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
public final class RangeRenderer {
 public static final int[] COLORS={0xf9fffe,0xf9801d,0xc74ebd,0x3ab3da,0xfed83d,0x80c71f,0xf38baa,0x474f52,0x9d9d97,0x169c9c,0x8932b8,0x3c44aa,0x835432,0x5e7c16,0xb02e26,0x1d1d21};
 public static void register(){WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context,outline)->!RangeController.holding(Minecraft.getInstance()));WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx->{var mc=Minecraft.getInstance();if(mc.player==null||ctx.matrices()==null||mc.level==null)return;
  if(!RangeController.holding(mc))return;Vec3 camera=ctx.worldState().cameraRenderState.pos;
  ctx.commandQueue().submitCustomGeometry(ctx.matrices(),RenderTypes.debugQuads(),(pose,buffer)->{
   for(var area:ClientAreas.areas)if(area.visible()&&area.dimension.equals(mc.level.dimension().identifier().toString())){Box b=area.box;box(buffer,pose.pose(),new BlockPos(b.x0(),b.y0(),b.z0()),new BlockPos(b.x1(),b.y1(),b.z1()),camera,COLORS[Math.floorMod(area.color,16)]);}
   var target=mc.player.isCreative()&&mc.player.isShiftKeyDown()?RangeController.breachTarget(mc):null;if(target==null)target=RangeController.target(mc);box(buffer,pose.pose(),target,target,camera,0xffffff);
   if(RangeController.first!=null)box(buffer,pose.pose(),RangeController.first,target,camera,0xffffff);
  });
 });}

 private static void box(VertexConsumer buffer,Matrix4f matrix,BlockPos first,BlockPos second,Vec3 camera,int color){
  double[] min={Math.min(first.getX(),second.getX())-.005,Math.min(first.getY(),second.getY())-.005,Math.min(first.getZ(),second.getZ())-.005};
  double[] max={Math.max(first.getX(),second.getX())+1.005,Math.max(first.getY(),second.getY())+1.005,Math.max(first.getZ(),second.getZ())+1.005};
  double line=first.equals(second)?.006:.025;
  for(int axis=0;axis<3;axis++)for(int i=0;i<2;i++)for(int j=0;j<2;j++){int u=(axis+1)%3,v=(axis+2)%3;double[] a=min.clone(),b=min.clone();a[u]=b[u]=i==0?min[u]:max[u];a[v]=b[v]=j==0?min[v]:max[v];b[axis]=max[axis];Vec3 start=new Vec3(a[0],a[1],a[2]).subtract(camera),end=new Vec3(b[0],b[1],b[2]).subtract(camera);Vec3 side=(u==0?new Vec3(line,0,0):u==1?new Vec3(0,line,0):new Vec3(0,0,line));quad(buffer,matrix,start,end,end.add(side),start.add(side),color,240);Vec3 other=(v==0?new Vec3(line,0,0):v==1?new Vec3(0,line,0):new Vec3(0,0,line));quad(buffer,matrix,start,end,end.add(other),start.add(other),color,240);}
 }

 private static void quad(VertexConsumer b,Matrix4f m,Vec3 a,Vec3 c,Vec3 e,Vec3 f,int color,int alpha){for(var p:new Vec3[]{a,c,e,f})b.addVertex(m,(float)p.x,(float)p.y,(float)p.z).setColor(color>>16&255,color>>8&255,color&255,alpha);}
}
