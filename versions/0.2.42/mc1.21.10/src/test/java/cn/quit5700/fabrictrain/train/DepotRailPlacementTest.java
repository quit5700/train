package cn.quit5700.fabrictrain.train;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DepotRailPlacementTest {
 @BeforeAll static void bootstrap(){
  net.minecraft.SharedConstants.tryDetectVersion();
  net.minecraft.server.Bootstrap.bootStrap();
  
 }
 @Test void allVanillaRailsAllowTopPlacement(){
  for(var item:java.util.List.of(Items.RAIL,Items.POWERED_RAIL,Items.DETECTOR_RAIL,Items.ACTIVATOR_RAIL))
   assertTrue(DepotRailPlacement.isTopRail(new ItemStack(item),Direction.UP));
 }
 @Test void otherFacesKeepExistingInteraction(){
  for(var face:Direction.values())if(face!=Direction.UP)
   for(var item:java.util.List.of(Items.RAIL,Items.POWERED_RAIL,Items.DETECTOR_RAIL,Items.ACTIVATOR_RAIL))
    assertFalse(DepotRailPlacement.isTopRail(new ItemStack(item),face));
 }
 @Test void ordinaryItemsAreNotReclassifiedAsRails(){
  for(var item:java.util.List.of(Items.STONE,Items.DIAMOND,Items.REDSTONE,Items.HOPPER))
   assertFalse(DepotRailPlacement.isTopRail(new ItemStack(item),Direction.UP));
 }
 @Test void emptyHandKeepsExistingInteraction(){
  assertFalse(DepotRailPlacement.isTopRail(ItemStack.EMPTY,Direction.UP));
 }
}
