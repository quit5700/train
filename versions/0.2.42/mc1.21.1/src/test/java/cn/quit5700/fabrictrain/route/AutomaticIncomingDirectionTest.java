package cn.quit5700.fabrictrain.route;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticIncomingDirectionTest {
 @Test void actualArrivalOverridesOppositePlacementDirection(){
  TrackPoint junction=new TrackPoint(64,-60,57);
  Direction entry=SwitchRailRouting.entryForIncoming(new TrackPoint(64,-60,56),junction,Direction.SOUTH);
  assertEquals(Direction.NORTH,entry);
  assertEquals(Direction.SOUTH,SwitchRailRouting.exitFor(entry,1));
 }
 @Test void rightTurnUsesTravelDirectionAtNextJunction(){
  TrackPoint junction=new TrackPoint(64,-60,74);
  Direction entry=SwitchRailRouting.entryForIncoming(new TrackPoint(64,-60,73),junction,Direction.SOUTH);
  assertEquals(Direction.WEST,SwitchRailRouting.exitFor(entry,2));
 }
 @Test void arrivalAfterSlopeUsesHorizontalSide(){
  TrackPoint junction=new TrackPoint(64,-59,57);
  assertEquals(Direction.WEST,SwitchRailRouting.entryForIncoming(new TrackPoint(63,-60,57),junction,Direction.NORTH));
 }
 @Test void onlyMissingArrivalFallsBackToPlacement(){
  assertEquals(Direction.SOUTH,SwitchRailRouting.entryForIncoming(null,new TrackPoint(64,-60,57),Direction.SOUTH));
 }
}
