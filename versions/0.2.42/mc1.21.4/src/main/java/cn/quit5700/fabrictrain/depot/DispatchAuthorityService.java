package cn.quit5700.fabrictrain.depot;

import cn.quit5700.fabrictrain.offline.InfrastructureData;
import cn.quit5700.fabrictrain.offline.InfrastructureService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;

/** Creative dispatch is authorized by the operator's current/last observed mode, not a stale flag. */
public final class DispatchAuthorityService {
    private DispatchAuthorityService() {}

    public static void authorize(ServerLevel level, long depotId, ServerPlayer player) {
        var data = InfrastructureService.data(level.getServer());
        var next = new InfrastructureData.DispatchAuthority(player.getUUID(), player.isCreative());
        if (!next.equals(data.dispatchAuthorities.put(Long.toString(depotId), next))) data.setDirty();
    }

    public static void refreshPlayer(ServerPlayer player) {
        var data = InfrastructureService.data(((ServerLevel) player.level()).getServer());
        boolean changed = false;
        for (var entry : data.dispatchAuthorities.entrySet()) {
            var old = entry.getValue();
            if (old.player().equals(player.getUUID()) && old.creative() != player.isCreative()) {
                entry.setValue(new InfrastructureData.DispatchAuthority(old.player(), player.isCreative()));
                changed = true;
            }
        }
        if (changed) data.setDirty();
    }

    public static void refresh(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) refreshPlayer(player);
    }

    public static boolean isCreative(ServerLevel level, long depotId) {
        var server = level.getServer();
        var data = InfrastructureService.data(server);
        var auth = data.dispatchAuthorities.get(Long.toString(depotId));
        if (auth == null) {
            // Old single-player worlds have one unambiguous operator. Unknown multiplayer grants no free stock.
            var players = server.getPlayerList().getPlayers();
            if (!server.isSingleplayer() || players.size() != 1) return false;
            authorize(level, depotId, players.get(0));
            return players.get(0).isCreative();
        }
        var owner = server.getPlayerList().getPlayer(auth.player());
        if (owner == null) return auth.creative();
        refreshPlayer(owner);
        return owner.isCreative();
    }
}
