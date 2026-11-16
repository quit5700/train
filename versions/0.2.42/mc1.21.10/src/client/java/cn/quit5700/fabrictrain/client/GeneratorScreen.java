package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.power.GeneratorFuelSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Keeps the vanilla generator chest layout and adds fuel guidance only. */
public final class GeneratorScreen extends ContainerScreen {
    private long rejectedUntil;
    public GeneratorScreen(ChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        GeneratorFuelSlots.configure(menu);
    }
    public static boolean isGenerator(Component title) {
        return title.getContents() instanceof TranslatableContents contents
            && contents.getKey().equals("container.fabric_train_simulator.power_generator");
    }
    @Override protected void slotClicked(Slot slot, int id, int button, ClickType action) {
        ItemStack incoming = menu.getCarried();
        if (action == ClickType.SWAP && button >= 0 && button < minecraft.player.getInventory().getContainerSize())
            incoming = minecraft.player.getInventory().getItem(button);
        boolean reject = id >= 0 && id < 9 && !incoming.isEmpty() && !GeneratorFuelSlots.accepts(incoming)
            && (action == ClickType.PICKUP || action == ClickType.SWAP || action == ClickType.QUICK_CRAFT);
        if (action == ClickType.QUICK_MOVE && id >= 9 && slot != null && slot.hasItem())
            reject = !GeneratorFuelSlots.accepts(slot.getItem());
        if (reject) { rejectedUntil = System.currentTimeMillis() + 2200; return; }
        super.slotClicked(slot, id, button, action);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Component hint = Component.translatable("container.fabric_train_simulator.power_generator.fuel_hint");
        int hintY = Math.max(1, topPos - 13);
        graphics.fill(leftPos - 2, hintY - 2, leftPos + Math.max(imageWidth, font.width(hint)) + 2, hintY + 11, 0xD020232A);
        graphics.drawString(font, hint, leftPos, hintY, 0xFFE6E6E6, false);
        for (int i=0; i<9; i++) {
            Slot slot = menu.getSlot(i);
            if (slot.hasItem()) continue;
            int x = leftPos + slot.x, y = topPos + slot.y;
            graphics.renderFakeItem(new ItemStack(Items.REDSTONE), x, y);
            graphics.fill(x, y, x+16, y+16, 0xA0C6C6C6);
            if (mouseX >= x && mouseX < x+16 && mouseY >= y && mouseY < y+16)
                graphics.setTooltipForNextFrame(font, Component.translatable("container.fabric_train_simulator.power_generator.fuel_tooltip"), mouseX, mouseY);
        }
        if (System.currentTimeMillis() < rejectedUntil) {
            Component error = Component.translatable("container.fabric_train_simulator.power_generator.fuel_rejected");
            int y = topPos + imageHeight + 5;
            graphics.fill(leftPos-2, y-2, leftPos+Math.max(imageWidth,font.width(error))+2, y+11, 0xE020232A);
            graphics.drawString(font, error, leftPos, y, 0xFFFFBB66, false);
        }
    }
}
