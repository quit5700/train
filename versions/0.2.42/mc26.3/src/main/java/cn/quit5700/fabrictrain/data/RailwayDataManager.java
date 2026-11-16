package cn.quit5700.fabrictrain.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class RailwayDataManager {
    private RailwayDataManager() {
    }

    public static RailwaySavedData state(ServerLevel level) {
        MinecraftServer server = level.getServer();
        return server.overworld().getDataStorage().computeIfAbsent(RailwaySavedData.TYPE);
    }

    public static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public static RailwayDevice register(ServerLevel level, BlockPos pos, DeviceType type) {
        return state(level).register(type, dimension(level), pos.getX(), pos.getY(), pos.getZ());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        state(level).remove(dimension(level), pos.getX(), pos.getY(), pos.getZ());
    }
}
