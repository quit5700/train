package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class UiResourcePathsTest {
    @Test void gameAssetPathsUseValidLowercaseIdentifiers() throws Exception {
        Path assets=Path.of("src/main/resources/assets");
        try(var paths=Files.walk(assets)) {
            for(Path path:paths.filter(Files::isRegularFile).toList())
                assertTrue(assets.relativize(path).toString().replace('\\','/').matches("[a-z0-9/_.-]+"),path.toString());
        }
        assertTrue(Files.size(Path.of("src/main/resources/META-INF/licenses/noto-sans-sc-ofl.txt"))>1000);
    }
}
