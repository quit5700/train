package cn.quit5700.fabrictrain.network;

import net.minecraft.network.chat.Component;

/** Keeps operation results in their UI instead of sending chat messages. */
public final class OperationChat {
    private OperationChat() {}

    public static void suppress(Component ignored) {
    }
}
