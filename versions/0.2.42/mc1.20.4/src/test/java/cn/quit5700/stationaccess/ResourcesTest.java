package cn.quit5700.stationaccess;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class ResourcesTest {
 static final List<String> LANG=List.of("en_us","zh_cn","zh_tw","ja_jp","ko_kr","de_de","fr_fr","es_es");
 static JsonObject json(String path)throws IOException {try(var in=ResourcesTest.class.getClassLoader().getResourceAsStream(path)){assertNotNull(in,path);return JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();}}
 @Test void allEightLanguagesHaveMatchingKeysAndPlaceholders() throws Exception {JsonObject base=json("assets/station_access/lang/en_us.json");for(String lang:LANG){var data=json("assets/station_access/lang/"+lang+".json");assertEquals(base.keySet(),data.keySet());for(String key:base.keySet()){assertEquals(base.get(key).getAsString().split("%s",-1).length,data.get(key).getAsString().split("%s",-1).length,key);assertFalse(data.get(key).getAsString().isBlank());}}}
 @Test void helpHasAllThreeObjectsAndInvisibleBoundaryInEveryLanguage() throws Exception {for(String lang:LANG){var data=json("assets/station_access/lang/"+lang+".json");for(String item:List.of("setter","ticket","terminal","boundary")){assertTrue(data.has("help.station_access."+item+".title"));for(int line=1;line<=3;line++)assertTrue(data.has("help.station_access."+item+".line"+line));}}}
 @Test void survivalCraftableObjectsHaveRecipes() throws Exception {for(String item:List.of("setter","terminal")){var data=json("data/station_access/recipes/"+item+".json");assertEquals("station_access:"+item,data.getAsJsonObject("result").get("item").getAsString());}}
 @Test void allModernItemDefinitionsAndModelsExist() throws Exception {for(String item:List.of("setter","ticket","terminal")){assertEquals("station_access:item/"+item,json("assets/station_access/items/"+item+".json").getAsJsonObject("model").get("model").getAsString());json("assets/station_access/models/item/"+item+".json");}}
 @Test void rechargeBlockHasAllFourFacingVariants() throws Exception {assertEquals(4,json("assets/station_access/blockstates/terminal.json").getAsJsonObject("variants").size());}
 @Test void terminalDropsItself() throws Exception {assertTrue(json("data/station_access/loot_tables/blocks/terminal.json").toString().contains("station_access:terminal"));}
 @Test void integratedJarHasOneModIdentityAndNoSeparateAccessDependency() throws Exception {var deps=json("fabric.mod.json").getAsJsonObject("depends");assertEquals("fabric_train_simulator",json("fabric.mod.json").get("id").getAsString());assertFalse(deps.has("station_access"));assertFalse(deps.has("quick_build"));assertFalse(deps.has("fabric_train_simulator"));assertTrue(deps.has("fabric-lifecycle-events-v1"));}
 @Test void warningDoesNotSayMaximumSize() throws Exception {var cn=json("assets/station_access/lang/zh_cn.json");assertTrue(cn.get("ui.station_access.large").getAsString().contains("可能造成卡顿"));assertFalse(cn.get("message.station_access.size").getAsString().contains("最大"));}
 @Test void ticketsCannotBeCrafted(){assertNull(getClass().getClassLoader().getResource("data/station_access/recipe/ticket.json"));assertNull(getClass().getClassLoader().getResource("data/station_access/recipes/ticket.json"));}
}
