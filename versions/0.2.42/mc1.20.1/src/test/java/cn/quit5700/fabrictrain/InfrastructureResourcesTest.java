package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class InfrastructureResourcesTest {
    private static final Path ROOT=Path.of("src","main","resources");
    private static String read(String relative) throws Exception{return Files.readString(ROOT.resolve(relative),StandardCharsets.UTF_8);}

    @Test void stationIsAStandaloneBlockAndNotARailModel() throws Exception {
        String states=read("assets/fabric_train_simulator/blockstates/station_marker.json");
        String model=read("assets/fabric_train_simulator/models/block/station_marker.json");
        assertFalse(states.contains("shape="));
        assertFalse(model.contains("rail_flat"));
        assertTrue(model.contains("minecraft:block/cube"));
        for(String face:new String[]{"top","bottom","front","back","left","right"})
            assertTrue(model.contains("fabric_train_simulator:block/station_marker_"+face),face);
    }

    @Test void confirmedInfrastructureModelsIncludeAllSixFacesAndInventoryModels() throws Exception {
        for(String id:new String[]{"station_marker","power_generator","train_depot"}){
            String model=read("assets/fabric_train_simulator/models/block/"+id+".json");
            assertTrue(model.contains("minecraft:block/cube"),id);
            for(String face:new String[]{"top","bottom","front","back","left","right"}){
                assertTrue(model.contains("fabric_train_simulator:block/"+id+"_"+face),id+":"+face);
                assertTrue(Files.isRegularFile(ROOT.resolve(
                        "assets/fabric_train_simulator/textures/block/"+id+"_"+face+".png")),id+":"+face);
            }
            assertTrue(read("assets/fabric_train_simulator/models/item/"+id+".json")
                    .contains("fabric_train_simulator:block/"+id),id);
        }
    }

    @Test void toolsUseConfirmedFlatInventoryAndHandheldTextures() throws Exception {
        for(String id:new String[]{"line_setter","train_connector"}){
            String model=read("assets/fabric_train_simulator/models/item/"+id+".json");
            assertTrue(model.contains("minecraft:item/generated"),id);
            assertTrue(model.contains("fabric_train_simulator:item/"+id),id);
            assertTrue(Files.isRegularFile(ROOT.resolve(
                    "assets/fabric_train_simulator/textures/item/"+id+".png")),id);
        }
    }

    @Test void connectorRecipeUsesAvailableMinecraft262Ingredients() throws Exception {
        String recipe=read("data/fabric_train_simulator/recipes/train_connector.json");
        assertTrue(recipe.contains("minecraft:redstone_block"));
        assertTrue(recipe.contains("minecraft:iron_ingot"));
        assertFalse(recipe.contains("minecraft:chain"),"Minecraft 26.2 no longer registers the old chain item id");
    }

    @Test void generatorHasBlockItemRecipeAndLootResources() {
        for(String path:new String[]{"assets/fabric_train_simulator/blockstates/power_generator.json","assets/fabric_train_simulator/items/power_generator.json","assets/fabric_train_simulator/models/block/power_generator.json","assets/fabric_train_simulator/models/item/power_generator.json","data/fabric_train_simulator/recipes/power_generator.json","data/fabric_train_simulator/loot_table/blocks/power_generator.json"})
            assertTrue(Files.isRegularFile(ROOT.resolve(path)),path);
    }

    @Test void trainVehiclesUseCustomModelsWithoutVanillaShell() throws Exception {
        for(String id:new String[]{"locomotive","passenger_car","cargo_car"}){
            String display=id.equals("locomotive")?"locomotive_display":id.replace("_car","_display");
            assertTrue(read("assets/fabric_train_simulator/models/item/"+id+".json")
                    .contains("fabric_train_simulator:block/"+display),id);
            String model=read("assets/fabric_train_simulator/models/block/"+display+".json");
            assertTrue(model.contains("minecraft:block/gold_block"),id);
            assertTrue(model.contains("\"from\":[2,1,0.0]"),id+": model long axis follows minecart renderer coordinates");
        }
        assertTrue(read("assets/fabric_train_simulator/models/block/passenger_display.json")
                .contains("light_blue_stained_glass"));
        assertTrue(read("assets/fabric_train_simulator/models/block/locomotive_display.json")
                .contains("light_blue_stained_glass"));
        String cargo=read("assets/fabric_train_simulator/models/block/cargo_display.json");
        assertTrue(cargo.contains("minecraft:block/black_concrete"),"cargo body is black");
        String renderer=Files.readString(Path.of("src","client","java","cn","quit5700","fabrictrain","client","TrainVehicleRenderer.java"));
        assertTrue(renderer.contains("root().visible = false"),"vanilla minecart shell is hidden");
    }

    @Test void modRailsNeverUseRedPoweredDetectorOrActivatorTextures() throws Exception {
        for(String name:new String[]{"signal_rail.json","signal_rail_curved.json","signal_rail_raised_ne.json","signal_rail_raised_sw.json","switch_rail.json"}){
            String model=read("assets/fabric_train_simulator/models/block/"+name);
            assertFalse(model.contains("powered_rail"),name);
            assertFalse(model.contains("detector_rail"),name);
            assertFalse(model.contains("activator_rail"),name);
        }
    }
}
