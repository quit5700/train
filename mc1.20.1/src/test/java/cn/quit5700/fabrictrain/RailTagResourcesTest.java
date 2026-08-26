package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

final class RailTagResourcesTest {
    @Test void everyPlaceableModRailIsInMinecraftRailsTag() throws Exception {
        Path tag=Path.of("src","main","resources","data","minecraft","tags","blocks","rails.json");
        assertTrue(Files.isRegularFile(tag),"missing minecraft:rails block tag");
        assertFalse(Files.exists(Path.of("src","main","resources","data","minecraft","tags","block","rails.json")),
                "1.20.x must use the legacy plural block tag directory");
        String json=Files.readString(tag,StandardCharsets.UTF_8);
        assertTrue(json.contains("fabric_train_simulator:signal_rail"));
        assertTrue(json.contains("fabric_train_simulator:switch_rail"));
        assertFalse(json.contains("station_marker"),"standalone station must not be a rail");
    }
}
