package cn.quit5700.fabrictrain.compat;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

/** Legacy equivalent of OPTIONAL_CODEC: only an actually empty map is an empty slot. */
public final class OptionalItemStackCodec {
    private OptionalItemStackCodec() {}
    public static final Codec<ItemStack> CODEC=new Codec<>() {
        @Override public <T> DataResult<Pair<ItemStack,T>> decode(DynamicOps<T> ops,T input) {
            boolean empty=ops.getMap(input).result().filter(map->map.entries().findAny().isEmpty()).isPresent();
            return empty?DataResult.success(Pair.of(ItemStack.EMPTY,ops.empty())):ItemStack.CODEC.decode(ops,input);
        }
        @Override public <T> DataResult<T> encode(ItemStack stack,DynamicOps<T> ops,T prefix) {
            return stack.isEmpty()?ops.mergeToMap(prefix,Map.of()):ItemStack.CODEC.encode(stack,ops,prefix);
        }
    };
}
