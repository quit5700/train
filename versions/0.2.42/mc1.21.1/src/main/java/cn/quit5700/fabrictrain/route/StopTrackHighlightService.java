package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.data.RailwayLine;
import cn.quit5700.fabrictrain.item.LineSetterItem;
import cn.quit5700.fabrictrain.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Visual-only marker. It never changes the rail state or train physics. */
public final class StopTrackHighlightService {
    private static long ticks;

    private StopTrackHighlightService() {}

    public static void tick(MinecraftServer server) {
        if (++ticks % 10L != 0L) return;
        for (var player : server.getPlayerList().getPlayers()) {
            if (!player.getMainHandItem().is(ModItems.LINE_SETTER)
                    && !player.getOffhandItem().is(ModItems.LINE_SETTER)) continue;
            ServerLevel level = (ServerLevel) player.serverLevel();
            long lineId = LineSetterItem.selectedLine(player.getUUID());
            RailwayLine line = RailwayDataManager.state(level).line(lineId);
            if (line == null) continue;
            var data = RailwayDataManager.state(level);
            for (var entry : line.stationStopTracks().entrySet()) {
                int index = entry.getKey();
                if (index < 0 || index >= line.nodeIds().size()) continue;
                RailwayDevice station = data.device(line.nodeIds().get(index));
                if (station == null || !station.dimension().equals(RailwayDataManager.dimension(level))) continue;
                var stop = entry.getValue();
                BlockPos pos = new BlockPos(stop.x(), stop.y(), stop.z());
                if (!level.hasChunkAt(pos) || player.distanceToSqr(stop.x() + 0.5D, stop.y() + 0.5D, stop.z() + 0.5D) > 4096.0D)
                    continue;
                level.sendParticles(player, ParticleTypes.END_ROD, false,
                        stop.x() + 0.5D, stop.y() + 0.2D, stop.z() + 0.5D,
                        4, 0.35D, 0.05D, 0.35D, 0.0D);
            }
        }
    }
}
