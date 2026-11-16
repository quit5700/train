package cn.quit5700.fabrictrain.train;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RailSegmentGeometryTest {
 @Test void eachFlatCornerPassesThroughTheNativeEdgeMidpoints(){
  for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
   var g=RailSegmentGeometry.between(new BlockPos(3,81,4),new Vec3i(x,0,0),new Vec3i(0,0,z));
   assertEquals(Math.sqrt(.5),g.length(),1e-10);
   assertEquals(new Vec3(3.5+x*.5,81.1,4.5),g.at(0));
   assertEquals(new Vec3(3.5,81.1,4.5+z*.5),g.at(g.length()));
   assertEquals(3.5+x*.25,g.at(g.length()/2).x,1e-10);
   assertEquals(4.5+z*.25,g.at(g.length()/2).z,1e-10);
  }
 }
 @Test void projectionDoesNotPutACornerCarAtTheBlockCenter(){
  var g=RailSegmentGeometry.between(BlockPos.ZERO,new Vec3i(-1,0,0),new Vec3i(0,0,1));
  var p=g.at(g.project(new Vec3(.5,.1,.5)));
  assertEquals(.25,p.x,1e-10);assertEquals(.75,p.z,1e-10);
 }
 @Test void reverseTraversalUsesTheIdenticalTrack(){
  var a=RailSegmentGeometry.between(BlockPos.ZERO,new Vec3i(-1,0,0),new Vec3i(0,0,1));
  var b=RailSegmentGeometry.between(BlockPos.ZERO,new Vec3i(0,0,1),new Vec3i(-1,0,0));
  for(int i=0;i<=20;i++)assertTrue(a.at(a.length()*i/20).distanceTo(b.at(b.length()*(20-i)/20))<1e-10);
 }
 @Test void slopedEndpointsJoinLowAndHighNeighbourRails(){
  var g=RailSegmentGeometry.between(new BlockPos(0,81,0),new Vec3i(-1,-1,0),new Vec3i(1,0,0));
  assertEquals(81.1,g.entry().y,1e-10);assertEquals(82.1,g.exit().y,1e-10);
  assertEquals(Math.sqrt(2),g.length(),1e-10);
 }
}
