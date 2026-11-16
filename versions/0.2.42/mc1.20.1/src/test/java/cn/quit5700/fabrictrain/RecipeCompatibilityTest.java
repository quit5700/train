package cn.quit5700.fabrictrain;
import com.google.gson.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
class RecipeCompatibilityTest {
 static final String DIRECTORY="recipes",RESULT_FIELD="item";
 JsonArray baseline()throws Exception{return read("crafting-baseline.json").getAsJsonArray();}
 JsonElement read(String path)throws Exception{try(var in=getClass().getClassLoader().getResourceAsStream(path)){assertNotNull(in,"Missing resource "+path);return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8));}}
 String resource(JsonObject r,String folder){String[] id=r.get("id").getAsString().split(":");return "data/"+id[0]+"/"+folder+"/"+id[1]+".json";}
 @TestFactory Stream<DynamicTest> everyRecipeUsesVersionFormatAndPreservesMaterials()throws Exception{
  var tests=new ArrayList<DynamicTest>();for(var value:baseline()){var expected=value.getAsJsonObject();tests.add(DynamicTest.dynamicTest(expected.get("id").getAsString(),()->{
   var data=read(resource(expected,DIRECTORY)).getAsJsonObject();assertEquals("minecraft:crafting_shaped",data.get("type").getAsString());
   var grid=new ArrayList<String>(Collections.nCopies(9,""));var pattern=data.getAsJsonArray("pattern");assertTrue(pattern.size()<=3);for(int y=0;y<pattern.size();y++){String line=pattern.get(y).getAsString();assertTrue(line.length()<=3);for(int x=0;x<line.length();x++){char ch=line.charAt(x);if(ch==' ')continue;var ingredient=data.getAsJsonObject("key").get(String.valueOf(ch));assertTrue(ingredient.isJsonObject(),"Older recipe requires item object");var object=ingredient.getAsJsonObject();assertEquals(Set.of("item"),object.keySet());grid.set(y*3+x,object.get("item").getAsString());}}
   var wanted=new ArrayList<String>();expected.getAsJsonArray("grid").forEach(v->wanted.add(v.getAsString()));assertEquals(wanted,grid,"Crafting materials and positions must not change");
   var output=data.getAsJsonObject("result");assertEquals(expected.get("result").getAsString(),output.get(RESULT_FIELD).getAsString());assertEquals(expected.get("count").getAsInt(),output.get("count").getAsInt());
  }));}return tests.stream();
 }
 @TestFactory Stream<DynamicTest> noIgnoredDuplicateInWrongVersionDirectory()throws Exception{
  var tests=new ArrayList<DynamicTest>();for(var value:baseline()){var recipe=value.getAsJsonObject();tests.add(DynamicTest.dynamicTest(recipe.get("id").getAsString(),()->assertNull(getClass().getClassLoader().getResource(resource(recipe,DIRECTORY.equals("recipe")?"recipes":"recipe")))));}return tests.stream();
 }
 @Test void ticketStillHasNoRecipe(){for(String folder:List.of("recipe","recipes"))assertNull(getClass().getClassLoader().getResource("data/station_access/"+folder+"/ticket.json"));}
}
