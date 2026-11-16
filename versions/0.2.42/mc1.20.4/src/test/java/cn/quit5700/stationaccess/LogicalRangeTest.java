package cn.quit5700.stationaccess;
import cn.quit5700.stationaccess.core.Box;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class LogicalRangeTest {
 @Test void openingOneCellDoesNotDisableOtherBoundaryCells(){var a=new Area("owner","dim",new Box(0,0,0,6,6,6));var gap=new Box.Cell(0,2,3);a.holes.add(BlockPos.asLong(gap.x(),gap.y(),gap.z()));assertFalse(a.closed(gap));assertTrue(a.closed(new Box.Cell(0,2,4)));}
 @Test void hidingRangeCannotBypassPermissionCrossing(){var a=new Area("owner","dim",new Box(0,0,0,6,6,6));a.shown=false;var hits=a.box.crossings(new Box.Point(-2,2.5,3.5),new Box.Point(2,2.5,3.5));assertFalse(a.visible());assertEquals(1,hits.size());assertTrue(a.closed(hits.get(0)));}
 @Test void savedGapSurvivesAndOnlyItsCellIsOpen(){var a=new Area("owner","dim",new Box(0,0,0,6,6,6));a.holes.add(BlockPos.asLong(0,2,3));var b=AccessData.GSON.fromJson(AccessData.GSON.toJson(a),Area.class);assertFalse(b.closed(new Box.Cell(0,2,3)));assertTrue(b.closed(new Box.Cell(0,2,4)));}
}
