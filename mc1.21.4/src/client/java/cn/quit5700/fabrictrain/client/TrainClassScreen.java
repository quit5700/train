package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.network.TrainClassNetworking;
import cn.quit5700.fabrictrain.train.TrainClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TrainClassScreen extends Screen {
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
    }
    private final int entityId;
    private final boolean hasCargo;
    private TrainClass selected;
    private final double freightSpeed;
    private final double passengerSpeed;
    private final double expressSpeed;
    private final java.util.List<TrainClassNetworking.Station> stations;
    /** Zero is no locomotive destination; stations start at one. */
    private int stationIndex;
    private int dropdownOffset;
    private boolean dropdownOpen;

    public TrainClassScreen(TrainClassNetworking.OpenPayload payload) {
        super(Component.translatable("screen.fabric_train_simulator.train_class.title"));
        entityId = payload.entityId();
        hasCargo = payload.hasCargo();
        selected = TrainClass.values()[Math.clamp(payload.trainClassOrdinal(), 0, TrainClass.values().length - 1)];
        freightSpeed = payload.freightSpeedKmh();
        passengerSpeed = payload.passengerSpeedKmh();
        expressSpeed = payload.expressSpeedKmh();
        stations = payload.stations();
        stationIndex = 0;
        for (int i = 0; i < stations.size(); i++)
            if (stations.get(i).id() == payload.destinationStationId()) stationIndex = i + 1;
    }

    @Override protected void init() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 240, 250, 20);
        int left = layout.left() + 10;
        int top = layout.top();
        int contentWidth = layout.panelWidth() - 20;
        int third = (contentWidth - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("class.fabric_train_simulator.passenger"),
                button -> choose(TrainClass.PASSENGER)).bounds(left, top + 42, third, 20).build()).active = !hasCargo;
        addRenderableWidget(Button.builder(Component.translatable("class.fabric_train_simulator.cargo"),
                button -> choose(TrainClass.CARGO)).bounds(left + 6 + third, top + 42, third, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("class.fabric_train_simulator.express"),
                button -> choose(TrainClass.EXPRESS)).bounds(left + 12 + third * 2, top + 42, third, 20).build()).active = !hasCargo;
        if (selected == TrainClass.CARGO) {
            addRenderableWidget(Button.builder(destinationLabel(), button -> {
                dropdownOpen = !dropdownOpen;
                rebuildWidgets();
            }).bounds(left, top + 105, contentWidth, 20).build());
            if (dropdownOpen) addDropdownOptions(layout, left, top + 127, contentWidth);
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.train_class.save"),
                button -> save()).bounds(layout.left() + (layout.panelWidth() - 70) / 2, layout.footerY(), 70, 20).build());
    }

    private void addDropdownOptions(ResponsiveScreenLayout layout, int left, int firstY, int width) {
        int rows = Math.max(1, Math.min(4, (layout.footerY() - 20 - firstY) / 20));
        int count = stations.size() + 1;
        dropdownOffset = Math.clamp(dropdownOffset, 0, Math.max(0, count - rows));
        for (int row = 0; row < rows && dropdownOffset + row < count; row++) {
            int option = dropdownOffset + row;
            Component label = option == 0
                    ? Component.translatable("screen.fabric_train_simulator.cargo.no_destination")
                    : Component.literal(stations.get(option - 1).name());
            addRenderableWidget(Button.builder(label, button -> {
                stationIndex = option;
                dropdownOpen = false;
                rebuildWidgets();
            }).bounds(left, firstY + row * 20, width, 20).build());
        }
    }

    private void choose(TrainClass trainClass) {
        selected = hasCargo ? TrainClass.CARGO : trainClass;
        dropdownOpen = false;
        rebuildWidgets();
    }

    private Component destinationLabel() {
        Component value = stationIndex == 0
                ? Component.translatable("screen.fabric_train_simulator.cargo.no_destination")
                : Component.literal(stations.get(stationIndex - 1).name());
        return value.copy().append(" ▼");
    }

    private void save() {
        long destination = stationIndex == 0 ? 0L : stations.get(stationIndex - 1).id();
        ClientPlayNetworking.send(new TrainClassNetworking.UpdatePayload(entityId, selected.ordinal(),
                freightSpeed, passengerSpeed, expressSpeed, destination));
        onClose();
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!dropdownOpen) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        int next = Math.clamp(dropdownOffset - (int) Math.signum(vertical), 0, Math.max(0, stations.size()));
        if (next == dropdownOffset) return false;
        dropdownOffset = next;
        rebuildWidgets();
        return true;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        renderTransparentBackground(graphics);
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 240, 250, 20);
        int left = layout.left();
        int top = layout.top();
        graphics.fill(left, top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.renderOutline(left, top, layout.panelWidth(), layout.panelHeight(), 0xFF66CCFF);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.train_class.current",
                Component.translatable("class.fabric_train_simulator." + selected.name().toLowerCase())),
                width / 2, top + 72, 0xFFE6E6E6);
        if (selected == TrainClass.CARGO)
            graphics.drawString(font, Component.translatable("screen.fabric_train_simulator.cargo.destination_label"),
                    left + 10, top + 91, 0xFFCCCCCC);
        if (hasCargo && !dropdownOpen)
            graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.train_class.cargo_locked"),
                    width / 2, layout.footerY() - 14, 0xFFFFAA55);
        super.render(graphics, mouseX, mouseY, tickProgress);
    }
}
