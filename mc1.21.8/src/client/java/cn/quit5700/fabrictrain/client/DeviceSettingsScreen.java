package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.network.DeviceSettingsNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class DeviceSettingsScreen extends Screen {
    private final long deviceId;
    private final DeviceType type;
    private int dwellSeconds;
    private int cargoDwellSeconds;
    private int switchShape;
    private int switchEntryOrdinal;
    private EditBox name;
    private EditBox announcement;
    private final String initialName;
    private final String initialAnnouncement;
    private final boolean containerConnected;
    private int scrollOffset;

    public DeviceSettingsScreen(DeviceSettingsNetworking.OpenPayload payload) {
        super(titleFor(payload));
        deviceId = payload.deviceId();
        type = DeviceType.values()[Math.clamp(payload.typeOrdinal(), 0, DeviceType.values().length - 1)];
        dwellSeconds = payload.dwellSeconds();
        cargoDwellSeconds = payload.cargoDwellSeconds();
        switchShape = Math.clamp(payload.switchShapeOrdinal(), 0, 2);
        switchEntryOrdinal = payload.switchEntryOrdinal();
        initialName = payload.name();
        initialAnnouncement = payload.announcement();
        containerConnected = payload.containerConnected();
    }

    @Override protected void init() {
        boolean compact = height < 304;
        int desiredHeight = type == DeviceType.SWITCH ? 180 : compact ? 242 : 304;
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(width, height, 220, desiredHeight, 20);
        int left = layout.left();
        int top = layout.top();
        if (name == null) {
            name = new EditBox(font, 190, 20, Component.translatable("screen.fabric_train_simulator.device.name"));
            name.setMaxLength(64);
            name.setValue(initialName);
            announcement = new EditBox(font, 190, 20, Component.translatable("screen.fabric_train_simulator.station.announcement"));
            announcement.setMaxLength(128);
            announcement.setValue(initialAnnouncement);
        }
        int nameY = top + (compact ? 38 : 48) - scrollOffset;
        name.setX(left + 15); name.setY(nameY); addRenderableWidget(name);
        name.visible = insideContent(nameY, layout, type == DeviceType.STATION ? layout.footerY() - 28 : layout.footerY());
        if(type==DeviceType.STATION){
            int announcementY=top+(compact?142:192)-scrollOffset;
            announcement.setX(left+15);announcement.setY(announcementY);addRenderableWidget(announcement);
            announcement.visible=insideContent(announcementY,layout,layout.footerY()-28);
        }
        if (type == DeviceType.STATION) {
            int dwellY=top+(compact?76:102)-scrollOffset,cargoY=top+(compact?110:146)-scrollOffset;
            addContentButton(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.minus_five"), b -> dwellSeconds = Math.max(0, dwellSeconds - 5)).bounds(left + 15, dwellY, 70, 20).build(),layout,layout.footerY()-28);
            addContentButton(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.plus_five"), b -> dwellSeconds = Math.min(3600, dwellSeconds + 5)).bounds(left + 135, dwellY, 70, 20).build(),layout,layout.footerY()-28);
            addContentButton(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.minus_five"), b -> cargoDwellSeconds = Math.max(0, cargoDwellSeconds - 5)).bounds(left + 15, cargoY, 70, 20).build(),layout,layout.footerY()-28);
            addContentButton(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.plus_five"), b -> cargoDwellSeconds = Math.min(3600, cargoDwellSeconds + 5)).bounds(left + 135, cargoY, 70, 20).build(),layout,layout.footerY()-28);
        }
        if (type == DeviceType.SWITCH) {
            addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.switch.left"), b -> switchShape = 0).bounds(left + 10, top + 90, 62, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.switch.straight"), b -> switchShape = 1).bounds(left + 79, top + 90, 62, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.switch.right"), b -> switchShape = 2).bounds(left + 148, top + 90, 62, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.device.save"), b -> save()).bounds(left + 75, layout.footerY(), 70, 20).build());
    }

    private void addContentButton(Button button, ResponsiveScreenLayout layout, int bottom) {
        button.visible = insideContent(button.getY(), layout, bottom);
        addRenderableWidget(button);
    }

    private static boolean insideContent(int y, ResponsiveScreenLayout layout, int bottom) {
        return y >= layout.scrollTop() && y + 20 <= bottom;
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical) {
        boolean compact=height<304;int desiredHeight=type==DeviceType.SWITCH?180:compact?242:304;
        ResponsiveScreenLayout layout=ResponsiveScreenLayout.of(width,height,220,desiredHeight,20);
        if(!layout.scrollRequired())return super.mouseScrolled(mouseX,mouseY,horizontal,vertical);
        int maximum=Math.max(0,desiredHeight-layout.panelHeight());
        int next=Math.clamp(scrollOffset-(int)Math.signum(vertical)*20,0,maximum);
        if(next==scrollOffset)return false;scrollOffset=next;rebuildWidgets();return true;
    }

    private static Component titleFor(DeviceSettingsNetworking.OpenPayload payload) {
        DeviceType type = DeviceType.values()[Math.clamp(payload.typeOrdinal(), 0, DeviceType.values().length - 1)];
        return Component.translatable(type == DeviceType.STATION
                ? "screen.fabric_train_simulator.station.named_title"
                : "screen.fabric_train_simulator.switch.named_title", payload.name());
    }
    private void save() {
        ClientPlayNetworking.send(new DeviceSettingsNetworking.UpdatePayload(deviceId, name.getValue(), dwellSeconds,
                cargoDwellSeconds, switchShape, switchEntryOrdinal, announcement.getValue()));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        boolean compact=height<304;int desiredHeight=type==DeviceType.SWITCH?180:compact?242:304;
        ResponsiveScreenLayout layout=ResponsiveScreenLayout.of(width,height,220,desiredHeight,20);
        int left = layout.left();
        int top = layout.top();
        graphics.fill(left, top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.renderOutline(left, top, layout.panelWidth(), layout.panelHeight(), 0xFF66CCFF);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFFFF);
        int nameLabelY=top+(compact?26:35)-scrollOffset;
        if(insideContent(nameLabelY,layout,type==DeviceType.STATION?layout.footerY()-28:layout.footerY()))graphics.drawString(font, Component.translatable("screen.fabric_train_simulator.device.name"), left + 15, nameLabelY, 0xFFCCCCCC);
        int dwellLabelY=top+(compact?62:87)-scrollOffset;
        if (type == DeviceType.STATION&&insideContent(dwellLabelY,layout,layout.footerY()-28)) graphics.drawCenteredString(font,
                Component.translatable("screen.fabric_train_simulator.device.dwell", dwellSeconds), width / 2, dwellLabelY, 0xFFE6E6E6);
        if(type==DeviceType.STATION) {
            int cargoLabelY=top+(compact?96:131)-scrollOffset,announcementLabelY=top+(compact?130:179)-scrollOffset,containerY=top+(compact?166:218)-scrollOffset;
            if(insideContent(cargoLabelY,layout,layout.footerY()-28))graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.station.cargo_dwell", cargoDwellSeconds), width / 2, cargoLabelY, 0xFFE6E6E6);
            if(insideContent(announcementLabelY,layout,layout.footerY()-28))graphics.drawString(font,Component.translatable("screen.fabric_train_simulator.station.announcement"),left+15,announcementLabelY,0xFFCCCCCC);
            if(insideContent(containerY,layout,layout.footerY()-28))graphics.drawCenteredString(font, Component.translatable(containerConnected
                    ? "screen.fabric_train_simulator.station.container_connected"
                    : "screen.fabric_train_simulator.station.container_disconnected"), width / 2, containerY, 0xFFE6E6E6);
        }
        if (type == DeviceType.SWITCH) graphics.drawCenteredString(font,
                Component.translatable("screen.fabric_train_simulator.device.shape",
                        Component.translatable("screen.fabric_train_simulator.switch." + switchName())), width / 2, top + 120, 0xFFE6E6E6);
        super.render(graphics, mouseX, mouseY, tickProgress);
    }

    private String switchName() { return switch (switchShape) { case 0 -> "left"; case 2 -> "right"; default -> "straight"; }; }
}
