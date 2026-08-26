package cn.quit5700.fabrictrain.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public final class LegacyServerPlayNetworking {
    private LegacyServerPlayNetworking() {}

    public record Context(MinecraftServer server, ServerPlayer player) {}

    public static <T extends LegacyPayload> void registerGlobalReceiver(
            LegacyPayload.Type<T> type, BiConsumer<T, Context> receiver) {
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(type.id(), (server, player, handler, buffer, responseSender) -> {
            T payload = LegacyPayloadTypeRegistry.codec(type).decode(buffer);
            receiver.accept(payload, new Context(server, player));
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void send(ServerPlayer player, LegacyPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        LegacyPayload.Type type = payload.type();
        LegacyPayloadTypeRegistry.codec(type).encode(buffer, payload);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, type.id(), buffer);
    }
}


