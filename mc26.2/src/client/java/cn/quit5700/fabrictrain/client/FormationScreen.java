package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.network.FormationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public final class FormationScreen extends Screen {
    private final int seedEntityId;
    private final ArrayList<FormationNetworking.Car> cars;
    private int selected;

    public FormationScreen(FormationNetworking.OpenPayload payload) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.fabric_train_simulator.formation.title"));
        seedEntityId = payload.seedEntityId();
        cars = new ArrayList<>(payload.cars());
    }

    @Override protected void init() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 280, 210, 20);
        int left = layout.left() + 10, bottom = layout.footerY();
        int contentWidth = layout.panelWidth() - 20;
        int quarter = (contentWidth - 15) / 4;
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.formation.previous"),
                b -> selected = Math.max(0, selected - 1)).bounds(left, bottom - 24, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.formation.up"),
                b -> move(-1)).bounds(left + 5 + quarter, bottom - 24, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.formation.down"),
                b -> move(1)).bounds(left + 10 + quarter * 2, bottom - 24, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.formation.next"),
                b -> selected = Math.min(cars.size() - 1, selected + 1)).bounds(left + 15 + quarter * 3, bottom - 24, quarter, 20).build());
        int half = (contentWidth - 10) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.formation.save"),
                b -> save(false)).bounds(left, bottom, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.formation.dissolve"),
                b -> save(true)).bounds(left + 10 + half, bottom, half, 20).build());
    }

    private void move(int amount) {
        int target = selected + amount;
        if (target < 0 || target >= cars.size()) return;
        var car = cars.remove(selected); cars.add(target, car); selected = target;
    }

    private void save(boolean dissolve) {
        ClientPlayNetworking.send(new FormationNetworking.UpdatePayload(seedEntityId, dissolve,
                cars.stream().map(FormationNetworking.Car::entityId).toList()));
        onClose();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 280, 210, 20);
        int left = layout.left(), top = layout.top();
        graphics.fill(left, top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.outline(left, top, layout.panelWidth(), layout.panelHeight(), 0xFF55DDEE);
        graphics.centeredText(font, title, width / 2, top + 9, 0xFFFFFFFF);
        int first = Math.max(0, Math.min(selected - 4, Math.max(0, cars.size() - 9)));
        for (int i = first; i < Math.min(cars.size(), first + 9); i++) {
            int y = top + 30 + (i - first) * 15;
            if (i == selected) graphics.fill(left + 15, y - 2, layout.right() - 15, y + 12, 0xAA2C6B78);
            graphics.text(font, Component.translatable("screen.fabric_train_simulator.formation.row",
                    i + 1, cars.get(i).name()), left + 22, y, 0xFFE6E6E6);
        }
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }
}
