package cn.quit5700.stationaccess;
import cn.quit5700.stationaccess.core.Box;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class RangeStateTest {
 @Test void legacyAreaUsesMinimumCornerAndStaysVisible(){var a=AccessData.GSON.fromJson("{\"box\":{\"x0\":-6,\"y0\":2,\"z0\":3,\"x1\":9,\"y1\":8,\"z1\":10}}",Area.class);assertTrue(a.visible());assertEquals(new Box.Cell(-6,2,3),a.anchor());}
 @Test void actualFirstPointSurvivesSaveEvenAtMaximumCorner(){var a=new Area("owner","dimension",new Box(-6,2,3,9,8,10));a.first=new Box.Cell(9,8,10);a.second=new Box.Cell(-6,2,3);a.shown=false;a.color=15;var restored=AccessData.GSON.fromJson(AccessData.GSON.toJson(a),Area.class);assertEquals(a.first,restored.anchor());assertFalse(restored.visible());assertEquals(15,restored.color);assertEquals(a.box,restored.box);}
 @Test void movingSecondPointAcrossAnchorDoesNotMoveFirst(){var a=new Area("owner","dimension",new Box(-6,2,3,9,8,10));a.first=new Box.Cell(9,8,10);var b=new Box.Cell(20,18,30);a.box=Box.between(a.anchor(),b);assertEquals(new Box.Cell(9,8,10),a.anchor());assertTrue(a.box.contains(b));assertTrue(a.box.contains(a.anchor()));}
 @Test void secondPointEditReleasesListSessionAndKeepsAnchor() throws Exception {var player=java.util.UUID.randomUUID();var sf=AccessNetwork.class.getDeclaredField("SESSIONS");sf.setAccessible(true);@SuppressWarnings("unchecked") var sessions=(java.util.Map<java.util.UUID,Object>)sf.get(null);sessions.put(player,new Object());var first=new net.minecraft.core.BlockPos(9,8,10);AccessNetwork.beginSecondPointEdit(player,first,"dimension","area-id");assertFalse(sessions.containsKey(player),"broadcast after saving must not reopen the list");var selections=AccessNetwork.class.getDeclaredField("SELECTIONS");selections.setAccessible(true);var pending=((java.util.Map<?,?>)selections.get(null)).get(player);var anchor=pending.getClass().getDeclaredField("a");anchor.setAccessible(true);assertEquals(first,anchor.get(pending));AccessNetwork.clear();}

}
