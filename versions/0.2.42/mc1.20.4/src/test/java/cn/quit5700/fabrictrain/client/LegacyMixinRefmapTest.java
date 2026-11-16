package cn.quit5700.fabrictrain.client;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonParser;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class LegacyMixinRefmapTest {
 @Test void productionMixinExplicitlyReferencesGeneratedRemapping() throws Exception {
  var config=JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric_train_simulator.client.mixins.json"),StandardCharsets.UTF_8)).getAsJsonObject();
  assertEquals("client-fabric-train-simulator-refmap.json",config.get("refmap").getAsString());
 }
}
