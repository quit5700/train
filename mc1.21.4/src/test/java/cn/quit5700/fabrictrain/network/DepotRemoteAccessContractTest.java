package cn.quit5700.fabrictrain.network;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DepotRemoteAccessContractTest {
    @Test
    void managerAuthorizesBeforeOpeningAndEveryDepotActionUsesTheSameGuard() throws Exception {
        String manager = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/RailwayManagerNetworking.java"),
                StandardCharsets.UTF_8);
        String depot = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/network/DepotNetworking.java"),
                StandardCharsets.UTF_8);
        String item = Files.readString(Path.of(
                "src/main/java/cn/quit5700/fabrictrain/item/LineSetterItem.java"),
                StandardCharsets.UTF_8);

        assertTrue(item.contains("DepotRemoteAccessSessions.authorizeManager"));
        assertTrue(manager.contains("DepotRemoteAccessSessions.hasManagerAccess"));
        assertTrue(manager.contains("DepotRemoteAccessSessions.revokeAll"));
        assertTrue(manager.indexOf("DepotRemoteAccessSessions.authorize")
                < manager.indexOf("DepotNetworking.open(player,pos,depot)"));
        assertTrue(count(depot, "if (!canAccess(player,") >= 3);
        assertTrue(depot.contains("private static void denyAccess(ServerPlayer player)"));
        assertTrue(depot.contains("DepotRemoteAccessSessions.revokeAll"));
    }

    private static int count(String text, String token) {
        int result = 0;
        for (int at = 0; (at = text.indexOf(token, at)) >= 0; at += token.length()) result++;
        return result;
    }
}
