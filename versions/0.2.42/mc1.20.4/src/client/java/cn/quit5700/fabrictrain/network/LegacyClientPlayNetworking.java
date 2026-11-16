package cn.quit5700.fabrictrain.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiConsumer;

public final class LegacyClientPlayNetworking {
    private LegacyClientPlayNetworking() {}
    public record Context(Minecraft client) {}

    public static <T extends LegacyPayload> void registerGlobalReceiver(
            LegacyPayload.Type<T> type, BiConsumer<T, Context> receiver) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(type.id(), (client, handler, buffer, responseSender) -> {
            T payload = LegacyPayloadTypeRegistry.codec(type).decode(buffer);
            receiver.accept(payload, new Context(client));
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void send(LegacyPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        LegacyPayload.Type type = payload.type();
        LegacyPayloadTypeRegistry.codec(type).encode(buffer, payload);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(type.id(), buffer);
    }
}


