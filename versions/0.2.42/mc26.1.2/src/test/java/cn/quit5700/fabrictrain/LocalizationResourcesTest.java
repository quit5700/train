package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationResourcesTest {
    private static final Pattern KEY = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:");

    @Test
    void allEightLanguagesHaveIdenticalKeys() throws IOException {
        Path lang = Path.of("src/main/resources/assets/fabric_train_simulator/lang");
        Set<String> expected = keys(lang.resolve("en_us.json"));
        for (String locale : Set.of("zh_cn", "zh_tw", "ja_jp", "ko_kr", "de_de", "fr_fr", "es_es")) {
            assertEquals(expected, keys(lang.resolve(locale + ".json")), locale);
        }
    }

    private static Set<String> keys(Path path) throws IOException {
        Matcher matcher = KEY.matcher(Files.readString(path, StandardCharsets.UTF_8));
        java.util.HashSet<String> result = new java.util.HashSet<>();
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
