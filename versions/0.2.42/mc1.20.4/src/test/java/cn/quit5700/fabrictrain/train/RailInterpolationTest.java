package cn.quit5700.fabrictrain.train;
import org.junit.jupiter.api.Test;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class RailInterpolationTest {
 @Test void interpolationFollowsTheCornerRatherThanTheEndpointChord(){
  Vec3 start=new Vec3(0,0,0);var path=List.of(new Vec3(1,0,0),new Vec3(1,0,1));
  assertEquals(new Vec3(1,0,0),RailInterpolation.sample(start,path,1));
  assertEquals(new Vec3(1,0,.5),RailInterpolation.sample(start,path,1.5));
  assertEquals(2,RailInterpolation.length(start,path));
 }
 @Test void repeatedPointsAndReverseMovementRemainFinite(){
  Vec3 start=new Vec3(1,0,1);var path=List.of(start,new Vec3(1,0,0),new Vec3(0,0,0));
  assertEquals(new Vec3(.5,0,0),RailInterpolation.sample(start,path,1.5));
  assertEquals(Vec3.ZERO,RailInterpolation.sample(start,path,100));
 }
}
