package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionRemapContractTest {
    @Test
    void distributionCopiesRuntimeRemappedArtifacts() throws Exception {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);
        String distribution = build.substring(build.indexOf("tasks.register('copyDistribution'"));

        assertTrue(distribution.contains("dependsOn tasks.named('remapJar'), tasks.named('remapSourcesJar')"));
        assertTrue(distribution.contains("from(tasks.named('remapJar'))"));
        assertTrue(distribution.contains("from(tasks.named('remapSourcesJar'))"));
        assertFalse(distribution.contains("from(tasks.named('jar'))"));
    }
}
