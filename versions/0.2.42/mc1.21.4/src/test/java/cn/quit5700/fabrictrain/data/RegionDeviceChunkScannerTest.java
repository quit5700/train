package cn.quit5700.fabrictrain.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionDeviceChunkScannerTest {
    @TempDir Path temporaryDirectory;

    @Test void findsDeviceChunkInLegacyOverworldRegionWithoutLoadingMinecraftChunk() throws Exception {
        Path region = temporaryDirectory.resolve("region/r.-1.2.mca");
        Files.createDirectories(region.getParent());
        byte[] raw = "palette fabric_train_simulator:station_marker".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) { deflater.write(raw); }
        byte[] payload = compressed.toByteArray();
        ByteBuffer file = ByteBuffer.allocate(3 * 4096);
        int index = 3 + 5 * 32;
        file.putInt(index * 4, (2 << 8) | 1);
        file.position(2 * 4096); file.putInt(payload.length + 1); file.put((byte) 2); file.put(payload);
        Files.write(region, file.array());
        List<Long> found = new ArrayList<>();

        RegionDeviceChunkScanner.scanWorld(temporaryDirectory, found::add);

        assertEquals(List.of(RegionDeviceChunkScanner.pack(-29, 69)), found);
    }
}
