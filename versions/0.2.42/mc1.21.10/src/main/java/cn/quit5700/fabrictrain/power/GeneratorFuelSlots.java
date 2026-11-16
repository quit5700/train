package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.block.entity.PowerGeneratorBlockEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;

/** One insertion policy for the existing nine-slot generator menu. */
public final class GeneratorFuelSlots {
    private GeneratorFuelSlots() {}
    public static boolean accepts(ItemStack stack) {
        return stack.is(Items.REDSTONE) || stack.is(Items.REDSTONE_BLOCK);
    }
    public static Slot restrict(Slot original) {
        Slot restricted = new Slot(original.container, original.getContainerSlot(), original.x, original.y) {
            @Override public boolean mayPlace(ItemStack stack) { return accepts(stack); }
            @Override public boolean mayPickup(Player player) {
                return original.mayPickup(player) && (!(container instanceof PowerGeneratorBlockEntity generator)
                    || generator.canTakeItem(player.getInventory(), getContainerSlot(), getItem()));
            }
        };
        restricted.index = original.index;
        return restricted;
    }
    public static void configure(ChestMenu menu) {
        for (int i=0; i<PowerGeneratorBlockEntity.SIZE; i++) menu.slots.set(i, restrict(menu.getSlot(i)));
    }
}
