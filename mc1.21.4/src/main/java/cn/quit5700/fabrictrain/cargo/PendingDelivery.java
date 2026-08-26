package cn.quit5700.fabrictrain.cargo;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public record PendingDelivery(UUID shipmentId, UUID sourceCarId, long stationId, long dueGameTime,
                              List<ItemStack> stacks) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final Codec<PendingDelivery> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("shipment_id").forGetter(PendingDelivery::shipmentId),
            UUID_CODEC.fieldOf("source_car_id").forGetter(PendingDelivery::sourceCarId),
            Codec.LONG.fieldOf("station_id").forGetter(PendingDelivery::stationId),
            Codec.LONG.fieldOf("due_game_time").forGetter(PendingDelivery::dueGameTime),
            ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(PendingDelivery::stacks)
    ).apply(instance, PendingDelivery::new));

    public PendingDelivery {
        stacks = stacks.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }
}
