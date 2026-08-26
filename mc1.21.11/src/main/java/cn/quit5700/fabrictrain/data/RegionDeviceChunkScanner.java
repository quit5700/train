package cn.quit5700.fabrictrain.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/** Finds chunks containing railway devices without depending on a version-specific NBT layout. */
final class RegionDeviceChunkScanner {
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    private static final byte[][] DEVICE_IDS = {
            "fabric_train_simulator:station_marker".getBytes(StandardCharsets.UTF_8),
            "fabric_train_simulator:switch_rail".getBytes(StandardCharsets.UTF_8),
            "fabric_train_simulator:train_depot".getBytes(StandardCharsets.UTF_8)
    };

    private RegionDeviceChunkScanner() { }

    static void scanWorld(Path worldRoot, LongConsumer candidateConsumer) throws IOException {
        Set<Path> regionDirectories = new LinkedHashSet<>();
        regionDirectories.add(worldRoot.resolve("region"));
        regionDirectories.add(worldRoot.resolve("dimensions").resolve("minecraft").resolve("overworld").resolve("region"));
        for (Path directory : regionDirectories) scanDirectory(directory, candidateConsumer);
    }

    private static void scanDirectory(Path directory, LongConsumer candidateConsumer) throws IOException {
        if (!Files.isDirectory(directory)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "r.*.*.mca")) {
            for (Path file : files) scanRegion(file, candidateConsumer);
        }
    }

    private static void scanRegion(Path file, LongConsumer candidateConsumer) throws IOException {
        Matcher matcher = REGION_NAME.matcher(file.getFileName().toString());
        if (!matcher.matches()) return;
        int regionX = Integer.parseInt(matcher.group(1));
        int regionZ = Integer.parseInt(matcher.group(2));
        try (var channel = Files.newByteChannel(file)) {
            java.nio.ByteBuffer header = java.nio.ByteBuffer.allocate(4096);
            if (channel.read(header) != 4096) return;
            header.flip();
            for (int index = 0; index < 1024; index++) {
                int location = header.getInt();
                int sectorOffset = location >>> 8;
                int sectorCount = location & 255;
                if (sectorOffset == 0 || sectorCount == 0) continue;
                int maximum = sectorCount * 4096 - 5;
                java.nio.ByteBuffer prefix = java.nio.ByteBuffer.allocate(5);
                channel.position((long) sectorOffset * 4096L);
                if (channel.read(prefix) != 5) continue;
                prefix.flip();
                int length = prefix.getInt();
                int compression = prefix.get() & 255;
                if ((compression & 128) != 0 || length <= 1 || length - 1 > maximum) continue;
                java.nio.ByteBuffer payload = java.nio.ByteBuffer.allocate(length - 1);
                while (payload.hasRemaining() && channel.read(payload) >= 0) { }
                if (payload.hasRemaining()) continue;
                byte[] unpacked = decompress(payload.array(), compression & 127);
                if (containsDeviceId(unpacked)) {
                    int chunkX = regionX * 32 + index % 32;
                    int chunkZ = regionZ * 32 + index / 32;
                    candidateConsumer.accept(pack(chunkX, chunkZ));
                }
            }
        }
    }

    private static byte[] decompress(byte[] payload, int compression) throws IOException {
        InputStream source = new ByteArrayInputStream(payload);
        InputStream decoded = switch (compression) {
            case 1 -> new GZIPInputStream(source);
            case 2 -> new InflaterInputStream(source);
            case 3 -> source;
            default -> throw new IOException("Unsupported region compression type: " + compression);
        };
        try (decoded) { return decoded.readAllBytes(); }
    }

    private static boolean containsDeviceId(byte[] data) {
        for (byte[] needle : DEVICE_IDS) if (indexOf(data, needle) >= 0) return true;
        return false;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        outer: for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) if (data[i + j] != needle[j]) continue outer;
            return i;
        }
        return -1;
    }

    static long pack(int chunkX, int chunkZ) { return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL); }
    static int unpackX(long packed) { return (int) (packed >> 32); }
    static int unpackZ(long packed) { return (int) packed; }
}
