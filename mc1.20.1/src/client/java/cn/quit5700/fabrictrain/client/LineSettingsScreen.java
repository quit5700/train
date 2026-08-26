package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.data.LineStatus;
import cn.quit5700.fabrictrain.network.LineSettingsNetworking;
import cn.quit5700.fabrictrain.network.LegacyClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

public final class LineSettingsScreen extends Screen {
    private static final int DESIRED_HEIGHT = 300;
    private final long lineId;
    private final ArrayList<Long> nodeIds;
    private int selectedNode;
    private int status;
    private int interval;
    private double speed;
    private int maximumTrains;
    private int scrollOffset;
    private EditBox lineName;
    private final String initialLineName;

    public LineSettingsScreen(LineSettingsNetworking.OpenPayload payload) {
        super(Component.translatable("screen.fabric_train_simulator.line.title", payload.lineId()));
        lineId = payload.lineId();
        nodeIds = new ArrayList<>(payload.nodeIds());
        selectedNode = Math.min(1, Math.max(0, nodeIds.size() - 1));
        status = payload.statusOrdinal();
        interval = payload.intervalSeconds();
        speed = payload.speedKmh();
        maximumTrains = payload.maximumTrains();
        initialLineName = payload.name();
    }

    private ResponsiveScreenLayout layout() {
        return ResponsiveScreenLayout.of(width, height, 250, DESIRED_HEIGHT, 20);
    }

    @Override
    protected void init() {
        ResponsiveScreenLayout layout = layout();
        int left = layout.left() + 10;
        int contentWidth = layout.panelWidth() - 20;
        int top = layout.top();
        if (lineName == null) {
            lineName = new EditBox(font, 0, 0, 210, 20, Component.translatable("screen.fabric_train_simulator.line.name"));
            lineName.setMaxLength(64);
            lineName.setValue(initialLineName);
        }
        int inputY = top + 28 - scrollOffset;
        lineName.setPosition(left, inputY);
        lineName.setWidth(contentWidth);
        addContent(lineName, layout);
        pair(layout, top + 62 - scrollOffset, "screen.fabric_train_simulator.line.minus_thirty", "screen.fabric_train_simulator.line.plus_thirty",
                () -> interval = Math.max(0, interval - 30), () -> interval = Math.min(3600, interval + 30));
        pair(layout, top + 88 - scrollOffset, "screen.fabric_train_simulator.line.minus_ten", "screen.fabric_train_simulator.line.plus_ten",
                () -> speed = Math.max(0, speed - 10), () -> speed += 10);
        pair(layout, top + 114 - scrollOffset, "screen.fabric_train_simulator.line.minus_one", "screen.fabric_train_simulator.line.plus_one",
                () -> maximumTrains = Math.max(1, maximumTrains - 1), () -> maximumTrains = Math.min(1000, maximumTrains + 1));
        int actionY = top + 148 - scrollOffset;
        int third = (contentWidth - 12) / 3;
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.status"),
                button -> status = (status + 1) % LineStatus.values().length).bounds(left, actionY, third, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.refresh"),
                button -> send(true, false)).bounds(left + 6 + third, actionY, third, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.delete"),
                button -> send(false, true)).bounds(left + 12 + third * 2, actionY, third, 20).build(), layout);
        controls(layout, top + 182 - scrollOffset, true);
        controls(layout, top + 208 - scrollOffset, false);
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.line.save"),
                button -> send(false, false)).bounds(layout.left() + (layout.panelWidth() - 78) / 2,
                layout.footerY(), 78, 20).build());
    }

    private void pair(ResponsiveScreenLayout layout, int y, String leftText, String rightText,
                      Runnable leftAction, Runnable rightAction) {
        int left = layout.left() + 10;
        int buttonWidth = Math.max(44, (layout.panelWidth() - 20) / 4);
        addContent(Button.builder(Component.translatable(leftText), button -> leftAction.run())
                .bounds(left, y, buttonWidth, 20).build(), layout);
        addContent(Button.builder(Component.translatable(rightText), button -> rightAction.run())
                .bounds(layout.right() - 10 - buttonWidth, y, buttonWidth, 20).build(), layout);
    }

    private void controls(ResponsiveScreenLayout layout, int y, boolean firstRow) {
        int left = layout.left() + 10;
        int contentWidth = layout.panelWidth() - 20;
        int third = (contentWidth - 12) / 3;
        if (firstRow) {
            addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.previous_node"),
                    button -> selectNode(-1)).bounds(left, y, third, 20).build(), layout);
            addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.move_up"),
                    button -> modifyNode(0)).bounds(left + 6 + third, y, third, 20).build(), layout);
            addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.next_node"),
                    button -> selectNode(1)).bounds(left + 12 + third * 2, y, third, 20).build(), layout);
        } else {
            addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.move_down"),
                    button -> modifyNode(1)).bounds(left, y, third, 20).build(), layout);
            addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.delete_node"),
                    button -> modifyNode(2)).bounds(left + 6 + third, y, third, 20).build(), layout);
            addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.line.add_nodes"),
                    button -> beginEditing()).bounds(left + 12 + third * 2, y, third, 20).build(), layout);
        }
    }

    private <T extends AbstractWidget> T addContent(T widget, ResponsiveScreenLayout layout) {
        widget.visible = widget.getY() >= layout.scrollTop() && widget.getY() + 20 <= layout.scrollBottom();
        return addRenderableWidget(widget);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        ResponsiveScreenLayout layout = layout();
        if (!layout.scrollRequired()) return super.mouseScrolled(mouseX, mouseY, vertical);
        int maximum = Math.max(0, DESIRED_HEIGHT - layout.panelHeight());
        int next = cn.quit5700.fabrictrain.compat.MathCompat.clamp(scrollOffset - (int) Math.signum(vertical) * 20, 0, maximum);
        if (next == scrollOffset) return false;
        scrollOffset = next;
        rebuildWidgets();
        return true;
    }

    private void send(boolean refresh, boolean delete) {
        LegacyClientPlayNetworking.send(new LineSettingsNetworking.UpdatePayload(lineId, lineName.getValue(), status,
                interval, speed, maximumTrains, refresh, delete, -1, -1, false));
        onClose();
    }

    private void selectNode(int amount) {
        if (nodeIds.size() > 1) selectedNode = 1 + Math.floorMod(selectedNode - 1 + amount, nodeIds.size() - 1);
    }

    private void modifyNode(int action) {
        if (selectedNode <= 0 || selectedNode >= nodeIds.size()) return;
        LegacyClientPlayNetworking.send(new LineSettingsNetworking.UpdatePayload(lineId, lineName.getValue(), status,
                interval, speed, maximumTrains, false, false, selectedNode, action, false));
        if (action == 0 && selectedNode > 1) {
            java.util.Collections.swap(nodeIds, selectedNode, selectedNode - 1);
            selectedNode--;
        } else if (action == 1 && selectedNode < nodeIds.size() - 1) {
            java.util.Collections.swap(nodeIds, selectedNode, selectedNode + 1);
            selectedNode++;
        } else if (action == 2) {
            nodeIds.remove(selectedNode);
            selectedNode = Math.min(selectedNode, nodeIds.size() - 1);
        }
    }

    private void beginEditing() {
        LegacyClientPlayNetworking.send(new LineSettingsNetworking.UpdatePayload(lineId, lineName.getValue(), status,
                interval, speed, maximumTrains, false, false, -1, -1, true));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        ResponsiveScreenLayout layout = layout();
        int top = layout.top();
        graphics.fill(layout.left(), top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.renderOutline(layout.left(), top, layout.panelWidth(), layout.panelHeight(), 0xFF66CCFF);
        graphics.drawCenteredString(font, title, width / 2, top + 9, 0xFFFFFFFF);
        label(graphics, layout, Component.translatable("screen.fabric_train_simulator.line.interval", interval), top + 68 - scrollOffset);
        label(graphics, layout, Component.translatable("screen.fabric_train_simulator.line.speed", speed), top + 94 - scrollOffset);
        label(graphics, layout, Component.translatable("screen.fabric_train_simulator.line.maximum", maximumTrains), top + 120 - scrollOffset);
        label(graphics, layout, Component.translatable("screen.fabric_train_simulator.line.current_status",
                Component.translatable("status.fabric_train_simulator." + LineStatus.values()[status].name().toLowerCase())), top + 136 - scrollOffset);
        if (!nodeIds.isEmpty()) label(graphics, layout, Component.translatable(
                "screen.fabric_train_simulator.line.selected_node", selectedNode,
                nodeIds.get(Math.max(0, selectedNode))), top + 172 - scrollOffset);
        super.render(graphics, mouseX, mouseY, tickProgress);
    }

    private void label(GuiGraphics graphics, ResponsiveScreenLayout layout, Component text, int y) {
        if (y >= layout.scrollTop() && y + font.lineHeight <= layout.scrollBottom())
            graphics.drawCenteredString(font, text, width / 2, y, 0xFFE6E6E6);
    }
}




