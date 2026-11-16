package cn.quit5700.fabrictrain.network;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotRemoteAccessSessionsTest {
    private final UUID player = UUID.randomUUID();
    private final BlockPos depot = new BlockPos(12, 70, -8);

    @AfterEach
    void clearSessions() {
        DepotRemoteAccessSessions.clear();
    }

    @Test
    void onlyTheAuthorizedDepotAndDimensionAreAccessible() {
        DepotRemoteAccessSessions.authorize(player, "minecraft:overworld", depot, 1_000L);

        assertTrue(DepotRemoteAccessSessions.hasAccess(
                player, "minecraft:overworld", depot, 1_001L));
        assertFalse(DepotRemoteAccessSessions.hasAccess(
                player, "minecraft:the_nether", depot, 1_001L));
        assertFalse(DepotRemoteAccessSessions.hasAccess(
                player, "minecraft:overworld", depot.east(), 1_001L));
    }

    @Test
    void sessionExpiresAndCanBeRevoked() {
        DepotRemoteAccessSessions.authorize(player, "minecraft:overworld", depot, 10L);
        assertFalse(DepotRemoteAccessSessions.hasAccess(
                player, "minecraft:overworld", depot,
                10L + DepotRemoteAccessSessions.timeoutNanos() + 1L));

        DepotRemoteAccessSessions.authorize(player, "minecraft:overworld", depot, 20L);
        DepotRemoteAccessSessions.revoke(player, depot);
        assertFalse(DepotRemoteAccessSessions.hasAccess(
                player, "minecraft:overworld", depot, 21L));
    }

    @Test
    void managerSessionAlsoExpires() {
        DepotRemoteAccessSessions.authorizeManager(player, 100L);
        assertTrue(DepotRemoteAccessSessions.hasManagerAccess(player, 101L));
        assertFalse(DepotRemoteAccessSessions.hasManagerAccess(
                player, 101L + DepotRemoteAccessSessions.timeoutNanos() + 1L));
    }

    @Test
    void heldLineSetterRecoversAnExpiredManagerSession() {
        DepotRemoteAccessSessions.authorizeManager(player, 100L);
        long expired = 100L + DepotRemoteAccessSessions.timeoutNanos() + 1L;
        assertFalse(RailwayManagerNetworking.restoreManagerAccess(player, expired, false));
        assertTrue(RailwayManagerNetworking.restoreManagerAccess(player, expired, true));
        assertTrue(DepotRemoteAccessSessions.hasManagerAccess(player, expired + 1L));
    }
}
