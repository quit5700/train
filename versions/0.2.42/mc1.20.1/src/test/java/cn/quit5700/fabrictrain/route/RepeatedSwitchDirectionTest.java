package cn.quit5700.fabrictrain.route;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import com.google.gson.*;
import static org.junit.jupiter.api.Assertions.*;
class RepeatedSwitchDirectionTest {
 @Test void renderingAndRouteUseTheSameVisibleDirectionProperty() throws Exception {
  String source=Files.readString(Path.of("src/main/java/cn/quit5700/fabrictrain/route/WorldLineValidator.java"),java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(source.contains("sourceState.getValue(DeviceRailBlock.ENTRY_DIRECTION)"));
  assertFalse(source.contains("switchReferences.entry("));
  var variants=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/fabric_train_simulator/blockstates/switch_rail.json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("variants");
  for(String entry:new String[]{"north","east","south","west"})for(int choice=0;choice<3;choice++)
   assertNotNull(variants.get("entry_direction="+entry+",switch_choice="+choice));
 }
 @Test void selectedExitsMatchVisibleArrowUnderAllRotations(){
  Direction[][] expected={{Direction.EAST,Direction.SOUTH,Direction.WEST},{Direction.SOUTH,Direction.WEST,Direction.NORTH},{Direction.WEST,Direction.NORTH,Direction.EAST},{Direction.NORTH,Direction.EAST,Direction.SOUTH}};
  Direction[] entries={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
  for(int i=0;i<4;i++)for(int choice=0;choice<3;choice++)assertEquals(expected[i][choice],SwitchRailRouting.exitFor(entries[i],choice));
 }
 @Test void repeatedNodeDoesNotReuseAnotherNodesChoice(){
  var line=new cn.quit5700.fabrictrain.data.RailwayLine(1,"test",6,java.util.List.of(8L,4L,7L,8L,1L),30,80,1,cn.quit5700.fabrictrain.data.LineStatus.NEEDS_VALIDATION,java.util.Map.of(0,1,2,2,3,0));
  assertEquals(1,line.switchDirection(0));assertEquals(0,line.switchDirection(3));
 }
}
