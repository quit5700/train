package cn.quit5700.fabrictrain.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TrainConnectorItem extends Item {
    private static final Map<UUID, UUID> SELECTED = new HashMap<>();

    public TrainConnectorItem(Properties properties) {
        super(properties);
    }

    public static UUID selected(ServerPlayer player) {
        return SELECTED.get(player.getUUID());
    }

    public static void select(ServerPlayer player, UUID entityId, ItemStack stack) {
        SELECTED.put(player.getUUID(), entityId);
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        player.inventoryMenu.broadcastChanges();
    }

    public static void clear(ServerPlayer player, ItemStack stack) {
        SELECTED.remove(player.getUUID());
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        player.inventoryMenu.broadcastChanges();
    }

    public static void clearSessions() {
        SELECTED.clear();
    }
}
