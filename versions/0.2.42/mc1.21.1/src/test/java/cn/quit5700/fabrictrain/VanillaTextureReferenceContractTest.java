package cn.quit5700.fabrictrain;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;import java.nio.file.*;import org.junit.jupiter.api.Test;
class VanillaTextureReferenceContractTest {@Test void locomotiveUsesExistingFroglightTexture() throws Exception {String model=Files.readString(Path.of("src/main/resources/assets/fabric_train_simulator/models/block/locomotive_display.json"),StandardCharsets.UTF_8);assertTrue(model.contains("minecraft:block/ochre_froglight_side"));assertFalse(model.contains("\"minecraft:block/ochre_froglight\""));}}
