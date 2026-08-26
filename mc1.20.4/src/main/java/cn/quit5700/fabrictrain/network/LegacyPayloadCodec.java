package cn.quit5700.fabrictrain.network;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record LegacyPayloadCodec<B, T>(BiConsumer<T, B> writer, Function<B, T> reader) {
    public void encode(B buffer, T payload) { writer.accept(payload, buffer); }
    public T decode(B buffer) { return reader.apply(buffer); }
}
