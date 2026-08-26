package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationChatSilenceContractTest {
    @Test
    void onlyTheJoinInstructionAndConnectorFeedbackMayUseSystemChat() throws Exception {
        Path sourceRoot = Path.of("src", "main", "java");
        int systemChatCalls = 0;
        int nonOverlayClientMessages = 0;

        try (var paths = Files.walk(sourceRoot)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path, StandardCharsets.UTF_8);
                systemChatCalls += occurrences(source, "sendSystemMessage(");
                nonOverlayClientMessages += occurrences(source, "displayClientMessage(Component.translatable(");
            }
        }

        String initializer = Files.readString(sourceRoot.resolve(
                "cn/quit5700/fabrictrain/FabricTrainMod.java"), StandardCharsets.UTF_8);
        String connector = Files.readString(sourceRoot.resolve(
                "cn/quit5700/fabrictrain/train/ConnectorInteractionService.java"), StandardCharsets.UTF_8);
        int connectorFeedbackCalls = occurrences(connector, "sendSystemMessage(")
                + occurrences(connector, "displayClientMessage(Component.translatable(");
        assertEquals(4, systemChatCalls + nonOverlayClientMessages,
                "只允许登录说明和连接器的三类反馈使用普通消息通道");
        assertEquals(3, connectorFeedbackCalls);
        assertTrue(initializer.contains("message.fabric_train_simulator.settings_shortcut_hint"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
