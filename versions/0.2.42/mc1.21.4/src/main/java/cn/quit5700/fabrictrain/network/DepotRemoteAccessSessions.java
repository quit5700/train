package cn.quit5700.fabrictrain.network;

import net.minecraft.core.BlockPos;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived server-side authorization for opening one depot through the line setter. */
public final class DepotRemoteAccessSessions {
    private static final long TIMEOUT_NANOS = Duration.ofMinutes(5).toNanos();
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> MANAGER_SESSIONS = new HashMap<>();

    private DepotRemoteAccessSessions() {}

    public static synchronized void authorize(UUID playerId, String dimension, BlockPos depotPos, long nowNanos) {
        SESSIONS.put(playerId, new Session(dimension, depotPos.immutable(), nowNanos));
    }

    public static synchronized void authorizeManager(UUID playerId, long nowNanos) {
        MANAGER_SESSIONS.put(playerId, nowNanos);
    }

    public static synchronized boolean hasManagerAccess(UUID playerId, long nowNanos) {
        Long lastAccess = MANAGER_SESSIONS.get(playerId);
        if (lastAccess == null) return false;
        if (nowNanos - lastAccess > TIMEOUT_NANOS) {
            MANAGER_SESSIONS.remove(playerId);
            return false;
        }
        MANAGER_SESSIONS.put(playerId, nowNanos);
        return true;
    }

    public static synchronized boolean hasAccess(UUID playerId, String dimension, BlockPos depotPos, long nowNanos) {
        Session session = SESSIONS.get(playerId);
        if (session == null) return false;
        if (nowNanos - session.lastAccessNanos() > TIMEOUT_NANOS) {
            SESSIONS.remove(playerId);
            return false;
        }
        if (!session.dimension().equals(dimension) || !session.depotPos().equals(depotPos)) return false;
        SESSIONS.put(playerId, new Session(dimension, depotPos.immutable(), nowNanos));
        return true;
    }

    public static synchronized void revoke(UUID playerId, BlockPos depotPos) {
        Session session = SESSIONS.get(playerId);
        if (session != null && session.depotPos().equals(depotPos)) SESSIONS.remove(playerId);
    }

    public static synchronized void revokeAll(UUID playerId) {
        SESSIONS.remove(playerId);
        MANAGER_SESSIONS.remove(playerId);
    }

    public static synchronized void clear() {
        SESSIONS.clear();
        MANAGER_SESSIONS.clear();
    }

    static long timeoutNanos() {
        return TIMEOUT_NANOS;
    }

    private record Session(String dimension, BlockPos depotPos, long lastAccessNanos) {}
}
