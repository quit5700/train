package cn.quit5700.fabrictrain.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LegacyPayloadTypeRegistry {
    private static final LegacyPayloadTypeRegistry INSTANCE = new LegacyPayloadTypeRegistry();
    private static final Map<ResourceLocation, LegacyPayloadCodec<FriendlyByteBuf, ? extends LegacyPayload>> CODECS =
            new ConcurrentHashMap<>();

    private LegacyPayloadTypeRegistry() {}
    public static LegacyPayloadTypeRegistry playC2S() { return INSTANCE; }
    public static LegacyPayloadTypeRegistry playS2C() { return INSTANCE; }

    public <T extends LegacyPayload> void register(LegacyPayload.Type<T> type,
                                                    LegacyPayloadCodec<FriendlyByteBuf, T> codec) {
        CODECS.put(type.id(), codec);
    }

    @SuppressWarnings("unchecked")
    static <T extends LegacyPayload> LegacyPayloadCodec<FriendlyByteBuf, T> codec(LegacyPayload.Type<T> type) {
        return (LegacyPayloadCodec<FriendlyByteBuf, T>) CODECS.get(type.id());
    }
}


