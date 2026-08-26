package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.network.RailwayManagerNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class RailwayManagerScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private final List<RailwayManagerNetworking.DeviceSummary> devices;
    private final List<RailwayManagerNetworking.LineSummary> lines;
    private RailwayManagerNetworking.LineSummary selected;
    private final boolean savedConfirmation;
    private EditBox lineName;
    private Button leftDirection;
    private Button straightDirection;
    private Button rightDirection;
    private long draggedDevice;
    private int draggedNode = -1;
    private int selectedSwitch = -1;
    private int leftScroll;
    private int middleScroll;
    private int rightScroll;
    private int draggedScrollbar = -1;
    private double pressX;
    private double pressY;

    public RailwayManagerScreen(RailwayManagerNetworking.OpenPayload payload) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.fabric_train_simulator.manager.title"));
        devices = payload.devices();
        lines = payload.lines();
        selected = lines.stream().filter(line -> line.id() == payload.selectedLineId()).findFirst().orElse(null);
        savedConfirmation = "screen.fabric_train_simulator.manager.saved".equals(payload.statusMessageKey());
    }

    private Layout layout() {
        return Layout.of(width, height, selected != null || savedConfirmation);
    }

    @Override
    protected void init() {
        Layout l = layout();
        lineName = new EditBox(font, Math.max(40, l.columnWidth() - 44), 20,
                Component.translatable("screen.fabric_train_simulator.line.name"));
        lineName.setMaxLength(64);
        lineName.setX(l.middleX() + 4);
        lineName.setY(l.footerY());
        lineName.setValue(selected == null ? "" : selected.name());
        addRenderableWidget(lineName);

        if (selected == null) {
            addRenderableWidget(Button.builder(Component.literal("＋"),
                    button -> send(RailwayManagerNetworking.CREATE, 0, 0, 0, 0, lineName.getValue()))
                    .bounds(l.middleX() + l.columnWidth() - 36, l.footerY(), 32, 20).build());
            return;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.device.save"),
                button -> send(RailwayManagerNetworking.RENAME, selected.id(), 0, 0, 0, lineName.getValue()))
                .bounds(l.middleX() + l.columnWidth() - 36, l.footerY(), 32, 20).build());
        int operationY = l.footerY();
        int operationWidth = Math.max(36, (l.columnWidth() - 12) / 4);
        leftDirection = addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.switch.left"),
                button -> setDirection(0)).bounds(l.rightX() + 4, operationY, operationWidth, 20).build());
        straightDirection = addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.switch.straight"),
                button -> setDirection(1)).bounds(l.rightX() + 6 + operationWidth, operationY, operationWidth, 20).build());
        rightDirection = addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.switch.right"),
                button -> setDirection(2)).bounds(l.rightX() + 8 + operationWidth * 2, operationY, operationWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.manager.close"),
                button -> ClientPlayNetworking.send(new RailwayManagerNetworking.SelectPayload(false, 0)))
                .bounds(l.rightX() + 10 + operationWidth * 3, operationY, operationWidth, 20).build());
        updateDirectionButtonVisibility();
    }

    private void updateDirectionButtonVisibility() {
        boolean visible = selectedSwitch >= 0;
        if (leftDirection != null) leftDirection.visible = visible;
        if (straightDirection != null) straightDirection.visible = visible;
        if (rightDirection != null) rightDirection.visible = visible;
    }

    private void setDirection(int value) {
        if (selectedSwitch >= 0) send(RailwayManagerNetworking.DIRECTION, selected.id(), 0, selectedSwitch, value, "");
    }

    private static void send(int operation, long line, long device, int index, int value, String text) {
        ClientPlayNetworking.send(new RailwayManagerNetworking.EditPayload(operation, line, device, index, value, text));
    }

    private List<RailwayManagerNetworking.DeviceSummary> leftDevices() {
        return devices.stream().filter(device -> device.type() == DeviceType.STATION.ordinal()
                || device.type() == DeviceType.SWITCH.ordinal()).toList();
    }

    private List<RailwayManagerNetworking.DeviceSummary> depots() {
        return devices.stream().filter(device -> device.type() == DeviceType.DEPOT.ordinal()).toList();
    }

    private RailwayManagerNetworking.DeviceSummary device(long id) {
        return devices.stream().filter(device -> device.id() == id).findFirst().orElse(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        clearDragCapture();
        if (super.mouseClicked(event, doubleClick)) return true;
        Layout l = layout();
        double x = event.x();
        double y = event.y();
        int row = (int) ((y - l.rowsY()) / ROW_HEIGHT);
        pressX = x;
        pressY = y;

        if (event.button() == 0 && beginScrollbarDrag(l, x, y)) return true;

        if (selected == null) {
            if (event.button() == 0 && l.insideMiddle(x) && validRow(row, lines.size(), l.rows())) {
                ClientPlayNetworking.send(new RailwayManagerNetworking.SelectPayload(false,
                        lines.get(middleScroll + row).id()));
                return true;
            }
            if (event.button() == 1 && l.insideMiddle(x) && validRow(row, lines.size(), l.rows())) {
                RailwayManagerNetworking.LineSummary line = lines.get(middleScroll + row);
                minecraft.setScreenAndShow(new ConfirmScreen(confirmed -> {
                    if (confirmed) send(RailwayManagerNetworking.DELETE, line.id(), 0, 0, 0, "");
                    else minecraft.setScreenAndShow(this);
                }, Component.translatable("screen.fabric_train_simulator.manager.delete_title"),
                        Component.translatable("screen.fabric_train_simulator.manager.delete_message", line.name())));
                return true;
            }
        }

        if (l.insideLeft(x) && validRow(row, leftDevices().size(), l.rows())) {
            if (event.button() == 0) draggedDevice = leftDevices().get(leftScroll + row).id();
            return true;
        }
        if (l.insideRight(x) && validRow(row, depots().size(), l.rows())) {
            RailwayManagerNetworking.DeviceSummary depot = depots().get(rightScroll + row);
            if (event.button() == 0) draggedDevice = depot.id();
            return true;
        }
        if (selected != null && l.insideMiddle(x)) {
            int nodeIndex = middleScroll + row;
            if (row >= 0 && nodeIndex < selected.nodes().size()) {
                if (event.button() == 1) {
                    send(RailwayManagerNetworking.REMOVE, selected.id(), 0, nodeIndex, 0, "");
                    return true;
                }
                if (event.button() == 0) {
                    draggedNode = nodeIndex;
                    RailwayManagerNetworking.DeviceSummary device = device(selected.nodes().get(nodeIndex));
                    selectedSwitch = device != null && device.type() == DeviceType.SWITCH.ordinal() ? nodeIndex : -1;
                    updateDirectionButtonVisibility();
                    return true;
                }
            }
        }
        selectedSwitch = -1;
        updateDirectionButtonVisibility();
        return false;
    }

    private void clearDragCapture() {
        draggedDevice = 0;
        draggedNode = -1;
    }

    private static boolean validRow(int row, int size, int rows) {
        return row >= 0 && row < Math.min(size, rows);
    }

    @Override
    public void onClose() {
        ClientPlayNetworking.send(new RailwayManagerNetworking.ClosePayload());
        super.onClose();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggedScrollbar >= 0) {
            draggedScrollbar = -1;
            return true;
        }
        Layout l = layout();
        double x = event.x();
        double y = event.y();
        boolean moved = Math.abs(x - pressX) + Math.abs(y - pressY) > 5;
        if (draggedDevice != 0) {
            long id = draggedDevice;
            draggedDevice = 0;
            if (selected != null && l.insideMiddle(x) && y >= l.rowsY() && y < l.rowsBottom()) {
                int at = Math.clamp(middleScroll + (int) ((y - l.rowsY()) / ROW_HEIGHT), 0, selected.nodes().size());
                send(RailwayManagerNetworking.ADD, selected.id(), id, at, 0, "");
            } else if (!moved) {
                ClientPlayNetworking.send(new RailwayManagerNetworking.SelectPayload(true, id));
            }
            return true;
        }
        if (draggedNode >= 0) {
            int from = draggedNode;
            draggedNode = -1;
            if (moved && l.insideMiddle(x) && y >= l.rowsY() && y < l.rowsBottom()) {
                int to = Math.clamp(middleScroll + (int) ((y - l.rowsY()) / ROW_HEIGHT),
                        0, Math.max(0, selected.nodes().size() - 1));
                send(RailwayManagerNetworking.MOVE, selected.id(), 0, from, to, "");
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggedScrollbar >= 0) {
            updateScrollbarFromMouse(draggedScrollbar, event.y(), layout());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        Layout l = layout();
        int column = l.insideLeft(mouseX) ? 0 : l.insideMiddle(mouseX) ? 1 : l.insideRight(mouseX) ? 2 : -1;
        if (column < 0 || mouseY < l.rowsY() || mouseY >= l.rowsBottom() || vertical == 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        }
        int before = scroll(column);
        setScroll(column, before + (vertical > 0 ? -1 : 1), l);
        return scroll(column) != before || super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickProgress) {
        Layout l = layout();
        clampScrolls(l);
        graphics.fill(l.left(), l.top(), l.left() + l.panelWidth(), l.top() + l.panelHeight(), 0xED20232A);
        graphics.outline(l.left(), l.top(), l.panelWidth(), l.panelHeight(), 0xFF66CCFF);
        graphics.fill(l.middleX() - 3, l.top() + 28, l.middleX() - 2, l.footerY() - 4, 0xFF666B73);
        graphics.fill(l.rightX() - 3, l.top() + 28, l.rightX() - 2, l.footerY() - 4, 0xFF666B73);
        graphics.centeredText(font, title, width / 2, l.top() + 7, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("screen.fabric_train_simulator.manager.devices"),
                l.leftX() + l.columnWidth() / 2, l.top() + 25, 0xFF66CCFF);
        graphics.centeredText(font, Component.translatable("screen.fabric_train_simulator.manager.lines"),
                l.middleX() + l.columnWidth() / 2, l.top() + 25, 0xFF66CCFF);
        if (selected != null) graphics.centeredText(font,
                Component.translatable("screen.fabric_train_simulator.manager.editing", selected.name()),
                l.middleX() + l.columnWidth() / 2, l.top() + 39, 0xFFEEEEEE);
        else if (savedConfirmation) graphics.centeredText(font,
                Component.translatable("screen.fabric_train_simulator.manager.saved"),
                l.middleX() + l.columnWidth() / 2, l.top() + 39, 0xFF66FF88);
        graphics.centeredText(font, Component.translatable("device_type.fabric_train_simulator.depot"),
                l.rightX() + l.columnWidth() / 2, l.top() + 25, 0xFF66CCFF);

        renderDevices(graphics, l.leftX(), l.rowsY(), leftDevices(), leftScroll, l);
        renderDevices(graphics, l.rightX(), l.rowsY(), depots(), rightScroll, l);
        if (selected == null) {
            for (int i = 0; i < l.rows() && middleScroll + i < lines.size(); i++)
                row(graphics, l.middleX(), l.rowsY() + i * ROW_HEIGHT,
                        lines.get(middleScroll + i).name(), 0xFFEEEEEE, l);
        } else {
            int start = middleScroll;
            for (int i = 0; i < l.rows() && start + i < selected.nodes().size(); i++) {
                int index = start + i;
                var device = device(selected.nodes().get(index));
                int direction = selected.directions().get(index);
                String suffix = direction < 0 ? "" : "  [" + (direction == 0 ? "←" : direction == 1 ? "↑" : "→") + "]";
                if (device != null && device.type() == DeviceType.STATION.ordinal()) {
                    RailwayManagerNetworking.StopSummary stop = selected.stops().get(index);
                    suffix = "  " + (stop.bound()
                            ? Component.translatable("screen.fabric_train_simulator.stop_track.bound_short", stop.x(), stop.y(), stop.z()).getString()
                            : Component.translatable("screen.fabric_train_simulator.stop_track.unbound").getString());
                }
                row(graphics, l.middleX(), l.rowsY() + i * ROW_HEIGHT,
                        (index + 1) + ". " + (device == null ? "?" : device.name()) + suffix,
                        index == selectedSwitch ? 0xFF66CCFF : 0xFFEEEEEE, l);
            }
            if (selectedSwitch >= 0)
                graphics.outline(l.rightX() + 1, l.footerY() - 3, l.columnWidth() - 2, 26, 0xFF777777);
        }
        renderScrollbar(graphics, 0, l.leftX(), leftDevices().size(), l);
        renderScrollbar(graphics, 1, l.middleX(), middleSize(), l);
        renderScrollbar(graphics, 2, l.rightX(), depots().size(), l);
        super.extractRenderState(graphics, mouseX, mouseY, tickProgress);
    }

    private void renderDevices(GuiGraphicsExtractor graphics, int x, int y,
                               List<RailwayManagerNetworking.DeviceSummary> list, int offset, Layout layout) {
        for (int i = 0; i < layout.rows() && offset + i < list.size(); i++) {
            RailwayManagerNetworking.DeviceSummary device = list.get(offset + i);
            row(graphics, x, y + i * ROW_HEIGHT, device.name() + "  #" + device.id(), 0xFFEEEEEE, layout);
        }
    }

    private void row(GuiGraphicsExtractor graphics, int x, int y, String text, int color, Layout layout) {
        graphics.fill(x, y, x + layout.columnWidth(), y + ROW_HEIGHT - 2, 0xAA343941);
        String fitted = font.plainSubstrByWidth(text, Math.max(12, layout.columnWidth() - 14));
        graphics.text(font, Component.literal(fitted), x + 4, y + 5, color);
    }

    private boolean beginScrollbarDrag(Layout layout, double x, double y) {
        if (y < layout.rowsY() || y >= layout.rowsBottom()) return false;
        int column = scrollbarColumnAt(layout, x);
        if (column < 0 || listSize(column) <= layout.rows()) return false;
        draggedScrollbar = column;
        updateScrollbarFromMouse(column, y, layout);
        return true;
    }

    private int scrollbarColumnAt(Layout layout, double x) {
        if (x >= layout.leftX() + layout.columnWidth() - 8 && x < layout.leftX() + layout.columnWidth()) return 0;
        if (x >= layout.middleX() + layout.columnWidth() - 8 && x < layout.middleX() + layout.columnWidth()) return 1;
        if (x >= layout.rightX() + layout.columnWidth() - 8 && x < layout.rightX() + layout.columnWidth()) return 2;
        return -1;
    }

    private void updateScrollbarFromMouse(int column, double mouseY, Layout layout) {
        int total = listSize(column);
        int maximum = Math.max(0, total - layout.rows());
        int trackHeight = layout.rowsBottom() - layout.rowsY();
        int thumbHeight = scrollbarThumbHeight(total, layout.rows(), trackHeight);
        double usable = Math.max(1, trackHeight - thumbHeight);
        int value = (int) Math.round((mouseY - layout.rowsY() - thumbHeight / 2.0D) * maximum / usable);
        setScroll(column, value, layout);
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int column, int x, int total, Layout layout) {
        if (total <= layout.rows()) return;
        int trackX = x + layout.columnWidth() - 6;
        int trackHeight = layout.rowsBottom() - layout.rowsY();
        int thumbHeight = scrollbarThumbHeight(total, layout.rows(), trackHeight);
        int maximum = total - layout.rows();
        int thumbY = layout.rowsY() + (trackHeight - thumbHeight) * scroll(column) / maximum;
        graphics.fill(trackX, layout.rowsY(), trackX + 4, layout.rowsBottom(), 0xFF17191D);
        graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight,
                draggedScrollbar == column ? 0xFF88EFFF : 0xFF66CCFF);
    }

    private static int scrollbarThumbHeight(int total, int visible, int trackHeight) {
        return Math.max(12, trackHeight * visible / Math.max(visible, total));
    }

    private int middleSize() { return selected == null ? lines.size() : selected.nodes().size(); }
    private int listSize(int column) {
        return switch (column) {
            case 0 -> leftDevices().size();
            case 1 -> middleSize();
            case 2 -> depots().size();
            default -> 0;
        };
    }
    private int scroll(int column) {
        return switch (column) { case 0 -> leftScroll; case 1 -> middleScroll; case 2 -> rightScroll; default -> 0; };
    }
    private void setScroll(int column, int value, Layout layout) {
        int clamped = Math.clamp(value, 0, Math.max(0, listSize(column) - layout.rows()));
        if (column == 0) leftScroll = clamped;
        else if (column == 1) middleScroll = clamped;
        else if (column == 2) rightScroll = clamped;
    }
    private void clampScrolls(Layout layout) {
        setScroll(0, leftScroll, layout);
        setScroll(1, middleScroll, layout);
        setScroll(2, rightScroll, layout);
    }

    private record Layout(int left, int top, int panelWidth, int panelHeight, int columnWidth,
                          int leftX, int middleX, int rightX, int rowsY, int rowsBottom,
                          int footerY, int rows) {
        static Layout of(int screenWidth, int screenHeight, boolean showStatus) {
            ResponsiveScreenLayout responsive = ResponsiveScreenLayout.of(
                    screenWidth, screenHeight, 900, 500, 20);
            int panelWidth = responsive.panelWidth();
            int panelHeight = responsive.panelHeight();
            int left = responsive.left();
            int top = responsive.top();
            int gap = 8;
            int columnWidth = (panelWidth - gap * 4) / 3;
            int leftX = left + gap;
            int middleX = leftX + columnWidth + gap;
            int rightX = middleX + columnWidth + gap;
            int rowsY = top + (showStatus ? 52 : 40);
            int footerY = top + panelHeight - 27;
            int rowsBottom = footerY - 4;
            int rows = Math.max(1, (rowsBottom - rowsY) / ROW_HEIGHT);
            return new Layout(left, top, panelWidth, panelHeight, columnWidth, leftX, middleX, rightX,
                    rowsY, rowsBottom, footerY, rows);
        }

        boolean insideLeft(double x) { return x >= leftX && x < leftX + columnWidth; }
        boolean insideMiddle(double x) { return x >= middleX && x < middleX + columnWidth; }
        boolean insideRight(double x) { return x >= rightX && x < rightX + columnWidth; }
    }
}
