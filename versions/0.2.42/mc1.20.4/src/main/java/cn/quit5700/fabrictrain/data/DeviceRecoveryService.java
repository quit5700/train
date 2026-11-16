package cn.quit5700.fabrictrain.data;

import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Recovers missing device records from the authoritative blocks stored in region files. */
public final class DeviceRecoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceRecoveryService.class);
    private static final ConcurrentLinkedQueue<Long> PENDING = new ConcurrentLinkedQueue<>();
    private static final Set<Long> QUEUED = ConcurrentHashMap.newKeySet();
    private static volatile boolean scanning;

    private DeviceRecoveryService() { }

    public static void begin(MinecraftServer server) {
        clear();
        scanning = true;
        Thread scanner = new Thread(() -> {
            try {
                RegionDeviceChunkScanner.scanWorld(server.getWorldPath(LevelResource.ROOT), packed -> {
                    if (QUEUED.add(packed)) PENDING.add(packed);
                });
            } catch (IOException exception) {
                LOGGER.error("扫描旧铁路设备区块失败", exception);
            } finally {
                scanning = false;
            }
        }, "fabric-train-device-recovery");
        scanner.setDaemon(true);
        scanner.start();
    }

    public static void tick(MinecraftServer server) {
        recoverPending(server.overworld(), 1);
    }

    public static void recoverPending(ServerLevel level, int maximumChunks) {
        for (int i = 0; i < maximumChunks; i++) {
            Long packed = PENDING.poll();
            if (packed == null) return;
            QUEUED.remove(packed);
            recoverChunk(level, RegionDeviceChunkScanner.unpackX(packed), RegionDeviceChunkScanner.unpackZ(packed));
        }
    }

    private static void recoverChunk(ServerLevel level, int chunkX, int chunkZ) {
        int minimumX = chunkX << 4;
        int minimumZ = chunkZ << 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int recovered = 0;
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    cursor.set(minimumX + localX, y, minimumZ + localZ);
                    var block = level.getBlockState(cursor).getBlock();
                    DeviceType type = block == ModBlocks.STATION_MARKER ? DeviceType.STATION
                            : block == ModBlocks.SWITCH_RAIL ? DeviceType.SWITCH
                            : block == ModBlocks.TRAIN_DEPOT ? DeviceType.DEPOT
                            : block == ModBlocks.FREIGHT_YARD ? DeviceType.FREIGHT_YARD : null;
                    if (type != null && RailwayDataManager.state(level).find(RailwayDataManager.dimension(level),
                            cursor.getX(), cursor.getY(), cursor.getZ()) == null) {
                        RailwayDataManager.register(level, cursor.immutable(), type);
                        recovered++;
                    }
                }
            }
        }
        if (recovered > 0) LOGGER.info("已从区块 ({}, {}) 恢复 {} 个铁路设备记录", chunkX, chunkZ, recovered);
    }

    public static boolean isScanning() { return scanning; }

    public static void clear() {
        PENDING.clear();
        QUEUED.clear();
        scanning = false;
    }
}
