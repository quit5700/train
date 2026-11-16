package cn.quit5700.stationaccess;
import org.junit.jupiter.api.Test;
import cn.quit5700.fabrictrain.compat.TrainHelpCompat;
import com.google.gson.JsonParser;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class OperationHelpCoverageTest {
 @Test void everyTrainItemHasTranslatedOperatingSteps() throws Exception {
  for(String language:new String[]{"en_us","zh_cn","zh_tw","ja_jp","ko_kr","de_de","fr_fr","es_es"}){
   var data=JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/fabric_train_simulator/lang/"+language+".json"),StandardCharsets.UTF_8)).getAsJsonObject();
   for(String item:TrainHelpCompat.ITEMS){assertFalse(TrainHelpCompat.pages(item).isEmpty());for(var page:TrainHelpCompat.pages(item))for(String key:page){assertTrue(data.has(key),language+":"+key);assertFalse(data.get(key).getAsString().isBlank());}}
  }
 }
}
