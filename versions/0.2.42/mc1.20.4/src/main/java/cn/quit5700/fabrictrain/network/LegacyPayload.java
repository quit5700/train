package cn.quit5700.fabrictrain.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface LegacyPayload {
    Type<? extends LegacyPayload> type();

    record Type<T extends LegacyPayload>(ResourceLocation id) {}

    static <T extends LegacyPayload> LegacyPayloadCodec<FriendlyByteBuf, T> codec(
            BiConsumer<T, FriendlyByteBuf> writer, Function<FriendlyByteBuf, T> reader) {
        return new LegacyPayloadCodec<>(writer, reader);
    }
}
