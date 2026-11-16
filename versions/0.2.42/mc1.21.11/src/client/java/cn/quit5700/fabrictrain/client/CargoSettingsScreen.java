package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.network.CargoSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CargoSettingsScreen extends Screen {
    // 设置页打开时保持单人世界及自动列车继续运行。
    @Override public boolean isPauseScreen() { return false; }

    private final int entityId;
    private List<CargoSettingsNetworking.Station> stations;
    /** Zero is the explicit empty/inherit choice; station entries start at one. */
    private int selectedIndex;
    private int dropdownOffset;
    private boolean dropdownOpen;

    public CargoSettingsScreen(CargoSettingsNetworking.OpenPayload payload) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.fabric_train_simulator.cargo.title"));
        entityId = payload.entityId();
        stations = payload.stations();
        selectedIndex = 0;
        for (int i = 0; i < stations.size(); i++)
            if (stations.get(i).id() == payload.selectedStationId()) selectedIndex = i + 1;
    }


    private int refreshTicks;
    @Override public void tick(){super.tick();if(++refreshTicks%10==0)ClientPlayNetworking.send(new cn.quit5700.fabrictrain.network.CargoSettingsNetworking.RefreshPayload(entityId,false));}
    public void refreshOptions(cn.quit5700.fabrictrain.network.CargoSettingsNetworking.OpenPayload payload){
        if(payload.entityId()!=entityId)return;
        long old=selectedIndex==0?0L:stations.get(selectedIndex-1).id();
        if(stations.equals(payload.stations()))return;
        stations=payload.stations();selectedIndex=0;
        for(int i=0;i<stations.size();i++)if(stations.get(i).id()==old)selectedIndex=i+1;
        if(old!=0L&&selectedIndex==0&&Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(Component.translatable("message.fabric_train_simulator.cargo.destination_cleared"),true);
        rebuildWidgets();
    }

    @Override protected void init() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 240, 220, 20);
        int left = layout.left() + 10;
        int top = layout.top();
        int contentWidth = layout.panelWidth() - 20;
        addRenderableWidget(Button.builder(selectedLabel(), button -> {
            dropdownOpen = !dropdownOpen;
            rebuildWidgets();
        }).bounds(left, top + 43, contentWidth, 20).build());

        if (dropdownOpen) addDropdownOptions(layout, left, top + 65, contentWidth);

        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.cargo.unload_all"), button -> {
            ClientPlayNetworking.send(new CargoSettingsNetworking.UnloadAllPayload(entityId));
            onClose();
        }).bounds(left, layout.footerY() - 24, contentWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.cargo.save"), button -> save())
                .bounds(layout.left() + (layout.panelWidth() - 70) / 2, layout.footerY(), 70, 20).build());
    }

    private void addDropdownOptions(ResponsiveScreenLayout layout, int left, int firstY, int width) {
        int availableRows = Math.max(1, Math.min(5, (layout.footerY() - 30 - firstY) / 20));
        int optionCount = stations.size() + 1;
        int maximumOffset = Math.max(0, optionCount - availableRows);
        dropdownOffset = Math.clamp(dropdownOffset, 0, maximumOffset);
        for (int row = 0; row < availableRows && dropdownOffset + row < optionCount; row++) {
            int option = dropdownOffset + row;
            Component label = option == 0
                    ? Component.translatable("screen.fabric_train_simulator.cargo.inherit")
                    : Component.literal(stations.get(option - 1).name());
            addRenderableWidget(Button.builder(label, button -> {
                selectedIndex = option;
                dropdownOpen = false;
                rebuildWidgets();
            }).bounds(left, firstY + row * 20, width, 20).build());
        }
    }

    private Component selectedLabel() {
        Component value = selectedIndex == 0
                ? Component.translatable("screen.fabric_train_simulator.cargo.inherit")
                : Component.literal(stations.get(selectedIndex - 1).name());
        return value.copy().append(" ▼");
    }

    private void save() {
        long stationId = selectedIndex == 0 ? 0L : stations.get(selectedIndex - 1).id();
        ClientPlayNetworking.send(new CargoSettingsNetworking.UpdatePayload(entityId, stationId));
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
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 240, 220, 20);
        int top = layout.top();
        graphics.fill(layout.left(), top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.renderOutline(layout.left(), top, layout.panelWidth(), layout.panelHeight(), 0xFFFFA64D);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("screen.fabric_train_simulator.cargo.destination_label"),
                layout.left() + 10, top + 30, 0xFFCCCCCC);
        super.render(graphics, mouseX, mouseY, tickProgress);
    }
}
