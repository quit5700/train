package cn.quit5700.fabrictrain.train;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record OfflineCarState(CarType type, long destinationStationId, List<ItemStack> items) {
    public static final Codec<OfflineCarState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(CarType::valueOf, CarType::name).fieldOf("type").forGetter(OfflineCarState::type),
            Codec.LONG.optionalFieldOf("destination_station_id", 0L).forGetter(OfflineCarState::destinationStationId),
            ItemStack.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(OfflineCarState::items)
    ).apply(instance, OfflineCarState::new));

    public OfflineCarState {
        destinationStationId = Math.max(0L, destinationStationId);
        items = items.stream().map(ItemStack::copy).toList();
    }
}
