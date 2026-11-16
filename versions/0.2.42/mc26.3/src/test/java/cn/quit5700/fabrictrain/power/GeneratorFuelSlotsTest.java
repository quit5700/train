package cn.quit5700.fabrictrain.power;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeneratorFuelSlotsTest {
    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var registries=net.minecraft.data.registries.VanillaRegistries.createWorldLookup();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries).forEach(pending -> pending.apply());
    }
    @Test void bothFuelItemsAreAcceptedByTheActualMenuSlot() {
        Slot slot=GeneratorFuelSlots.restrict(new Slot(new SimpleContainer(9),0,8,18));
        assertTrue(slot.mayPlace(new ItemStack(Items.REDSTONE)));
        assertTrue(slot.mayPlace(new ItemStack(Items.REDSTONE_BLOCK)));
    }
    @Test void ordinaryItemsAndOtherRedstoneComponentsAreRejected() {
        Slot slot=GeneratorFuelSlots.restrict(new Slot(new SimpleContainer(9),0,8,18));
        for (var item:java.util.List.of(Items.DIAMOND,Items.COAL,Items.REDSTONE_TORCH,Items.REPEATER,Items.DIRT))
            assertFalse(slot.mayPlace(new ItemStack(item)));
        assertFalse(slot.mayPlace(ItemStack.EMPTY));
    }
    @Test void legacyNonFuelCanStillBeTakenWithoutBeingDeleted() {
        SimpleContainer inventory=new SimpleContainer(9);
        inventory.setItem(2,new ItemStack(Items.DIAMOND,7));
        Slot slot=GeneratorFuelSlots.restrict(new Slot(inventory,2,44,18));
        assertEquals(7,slot.getItem().getCount());
        ItemStack removed=slot.remove(3);
        assertTrue(removed.is(Items.DIAMOND));
        assertEquals(3,removed.getCount());
        assertEquals(4,inventory.getItem(2).getCount());
    }
    @Test void slotIdentityAndPositionArePreserved() {
        Slot original=new Slot(new SimpleContainer(9),4,80,18);
        original.index=4;
        Slot slot=GeneratorFuelSlots.restrict(original);
        assertEquals(original.index,slot.index);
        assertEquals(original.getContainerSlot(),slot.getContainerSlot());
        assertSame(original.container,slot.container);
        assertEquals(original.x,slot.x);
        assertEquals(original.y,slot.y);
    }
}
