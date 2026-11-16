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
                nonOverlayClientMessages += ordinaryClientMessages(source);
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

    // 新通行模块使用原版动作栏，不能将overlay=true误算为普通聊天。普通聊天上限仍为4。
    @Test void overlayMessagesDoNotCountAsOrdinaryChat() {
        assertEquals(0, ordinaryClientMessages("p.displayClientMessage(Component.translatable(\"key\"),true);"));
        assertEquals(1, ordinaryClientMessages("p.displayClientMessage(Component.translatable(\"key\"),false);"));
    }
    private static int ordinaryClientMessages(String source) {
        String token=".displayClientMessage(";int count=0,at=0;
        while((at=source.indexOf(token,at))>=0){
            int start=at+token.length(),depth=1,end=start;boolean quoted=false,escaped=false;
            for(;end<source.length()&&depth>0;end++){
                char c=source.charAt(end);
                if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='\"')quoted=false;}
                else if(c=='\"')quoted=true;else if(c=='(')depth++;else if(c==')')depth--;
            }
            String args=source.substring(start,Math.max(start,end-1)).replaceAll("\\s+","");
            if(!args.endsWith(",true"))count++;
            at=end;
        }
        return count;
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
