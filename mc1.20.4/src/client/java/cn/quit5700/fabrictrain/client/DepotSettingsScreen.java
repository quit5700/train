package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.data.LineStatus;
import cn.quit5700.fabrictrain.depot.DepotSettingsAction;
import cn.quit5700.fabrictrain.depot.DepotDispatchResult;
import cn.quit5700.fabrictrain.depot.DepotTemplate;
import cn.quit5700.fabrictrain.network.DepotNetworking;
import cn.quit5700.fabrictrain.train.CarType;
import cn.quit5700.fabrictrain.train.SpeedRules;
import cn.quit5700.fabrictrain.train.TrainClass;
import cn.quit5700.fabrictrain.network.LegacyClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DepotSettingsScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private final BlockPos depotPos;
    private final ArrayList<CarType> cars;
    private final List<DepotNetworking.LineOption> lines;
    private int lineIndex = -1;
    private int lineStatus = -1;
    private int pauseState = -1;
    private final boolean automaticDispatchEnabled;
    private final int lastAutomaticDispatchResult;
    private final String operationMessageKey;
    private TrainClass trainClass;
    private int interval;
    private int maximum;
    private final long active;
    private Button lineButton;
    private Button dispatchButton;
    private Button passengerButton;
    private Button expressButton;
    private Button removeButton;
    private Button pauseButton;
    private Button trainListButton;
    private EditBox speedInput;
    private EditBox nameInput;
    private final String initialName;
    private final double[] speeds = new double[TrainClass.values().length];
    private int scrollOffset;

    public DepotSettingsScreen(DepotNetworking.OpenPayload payload) {
        super(Component.translatable("screen.fabric_train_simulator.depot.title"));
        depotPos = payload.pos();
        cars = new ArrayList<>(DepotTemplate.decode(payload.template()).cars());
        lines = payload.lines();
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).id() == payload.lineId()) lineIndex = i;
        lineStatus = payload.lineStatus();
        pauseState = payload.pauseState();
        automaticDispatchEnabled = payload.automaticDispatchEnabled();
        lastAutomaticDispatchResult = payload.lastAutomaticDispatchResult();
        operationMessageKey = payload.operationMessageKey();
        trainClass = TrainClass.values()[cn.quit5700.fabrictrain.compat.MathCompat.clamp(payload.trainClass(), 0, TrainClass.values().length - 1)];
        interval = payload.interval();
        maximum = payload.maximum();
        active = payload.active();
        speeds[TrainClass.CARGO.ordinal()] = payload.cargoSpeedKmh();
        speeds[TrainClass.PASSENGER.ordinal()] = payload.passengerSpeedKmh();
        speeds[TrainClass.EXPRESS.ordinal()] = payload.expressSpeedKmh();
        initialName = payload.name();
    }

    private int desiredHeight() {
        return Math.max(334, 314 + cars.size() * 11);
    }

    private ResponsiveScreenLayout layout() {
        return ResponsiveScreenLayout.of(width, height, PANEL_WIDTH, desiredHeight(), 20);
    }

    @Override
    protected void init() {
        ResponsiveScreenLayout layout = layout();
        int top = layout.top();
        int contentWidth = layout.panelWidth() - 20;
        if (nameInput == null) {
            nameInput = new EditBox(font, 240, 20, Component.translatable("screen.fabric_train_simulator.device.name"));
            nameInput.setMaxLength(64);
            nameInput.setValue(initialName);
        }
        int nameY = top + 36 - scrollOffset;
        nameInput.setPosition(layout.left() + 10, nameY);
        nameInput.setWidth(contentWidth);
        addContent(nameInput, layout);

        int classY = top + 62 - scrollOffset;
        int classWidth = Math.max(48, (contentWidth - 12) / 3);
        passengerButton = addContent(Button.builder(Component.translatable("class.fabric_train_simulator.passenger"),
                button -> choose(TrainClass.PASSENGER)).bounds(layout.left() + 10, classY, classWidth, 20).build(), layout);
        addContent(Button.builder(Component.translatable("class.fabric_train_simulator.cargo"),
                button -> choose(TrainClass.CARGO)).bounds(layout.left() + 16 + classWidth, classY, classWidth, 20).build(), layout);
        expressButton = addContent(Button.builder(Component.translatable("class.fabric_train_simulator.express"),
                button -> choose(TrainClass.EXPRESS)).bounds(layout.left() + 22 + classWidth * 2, classY, classWidth, 20).build(), layout);

        if (speedInput == null) {
            speedInput = new EditBox(font, contentWidth, 20, Component.translatable("screen.fabric_train_simulator.depot.speed"));
            speedInput.setMaxLength(12);
            speedInput.setValue(speedText(trainClass));
        }
        int speedY = top + 100 - scrollOffset;
        speedInput.setPosition(layout.left() + 10, speedY);
        speedInput.setWidth(contentWidth);
        addContent(speedInput, layout);

        int lineY = top + 126 - scrollOffset;
        lineButton = addContent(Button.builder(lineLabel(), button -> {
            lineIndex = lines.isEmpty() ? -1 : (lineIndex >= lines.size() - 1 ? -1 : lineIndex + 1);
            lineStatus = lineIndex < 0 ? -1 : lines.get(lineIndex).status();
            pauseState = lineStatus == LineStatus.PAUSED.ordinal() ? 1
                    : lineStatus == LineStatus.RUNNING.ordinal() ? 0 : -1;
            button.setMessage(lineLabel());
            updateButtons();
        }).bounds(layout.left() + 10, lineY, contentWidth, 20).build(), layout);

        int controlsY = top + 152 - scrollOffset;
        int quarter = Math.max(40, (contentWidth - 12) / 4);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.minus_five"),
                button -> interval = Math.max(1, interval - 5)).bounds(layout.left() + 10, controlsY, quarter, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.plus_five"),
                button -> interval = Math.min(3600, interval + 5)).bounds(layout.left() + 14 + quarter, controlsY, quarter, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.minus_one"),
                button -> maximum = Math.max(1, maximum - 1)).bounds(layout.left() + 18 + quarter * 2, controlsY, quarter, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.plus_one"),
                button -> maximum = Math.min(1000, maximum + 1)).bounds(layout.left() + 22 + quarter * 3, controlsY, quarter, 20).build(), layout);

        int addY = top + 208 - scrollOffset;
        int half = (contentWidth - 6) / 2;
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.add_passenger"),
                button -> add(CarType.PASSENGER)).bounds(layout.left() + 10, addY, half, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.add_cargo"),
                button -> add(CarType.CARGO)).bounds(layout.left() + 16 + half, addY, half, 20).build(), layout);

        int actionY = top + 234 - scrollOffset;
        int third = (contentWidth - 12) / 3;
        removeButton = addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.remove"),
                button -> removeLast()).bounds(layout.left() + 10, actionY, third, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.generate_now"),
                button -> send(DepotSettingsAction.GENERATE_MANUAL)).bounds(layout.left() + 16 + third, actionY, third, 20).build(), layout);
        dispatchButton = addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.dispatch_now"),
                button -> send(DepotSettingsAction.DISPATCH_AUTOMATIC)).bounds(layout.left() + 22 + third * 2, actionY, third, 20).build(), layout);

        int footerThird = (contentWidth - 12) / 3;
        pauseButton = addRenderableWidget(Button.builder(pauseLabel(),
                button -> send(DepotSettingsAction.TOGGLE_PAUSE))
                .bounds(layout.left() + 10, layout.footerY(), footerThird, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.save"),
                button -> send(DepotSettingsAction.SAVE))
                .bounds(layout.left() + 16 + footerThird, layout.footerY(), footerThird, 20).build());
        trainListButton = addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.train_list"),
                button -> LegacyClientPlayNetworking.send(new DepotNetworking.RequestTrainListPayload(depotPos)))
                .bounds(layout.left() + 22 + footerThird * 2, layout.footerY(), footerThird, 20).build());
        updateButtons();
    }

    private <T extends AbstractWidget> T addContent(T widget, ResponsiveScreenLayout layout) {
        widget.visible = insideContent(widget.getY(), layout);
        return addRenderableWidget(widget);
    }

    private static boolean insideContent(int y, ResponsiveScreenLayout layout) {
        return y >= layout.scrollTop() && y + 20 <= layout.scrollBottom();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        ResponsiveScreenLayout layout = layout();
        if (!layout.scrollRequired()) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        commitSpeed();
        int maximumOffset = Math.max(0, desiredHeight() - layout.panelHeight());
        int next = cn.quit5700.fabrictrain.compat.MathCompat.clamp(scrollOffset - (int) Math.signum(vertical) * 20, 0, maximumOffset);
        if (next == scrollOffset) return false;
        scrollOffset = next;
        rebuildWidgets();
        return true;
    }

    private boolean hasCargo() { return cars.contains(CarType.CARGO); }

    private void choose(TrainClass value) {
        commitSpeed();
        trainClass = hasCargo() ? TrainClass.CARGO : value;
        speedInput.setValue(speedText(trainClass));
        updateButtons();
    }

    private void add(CarType type) {
        if (cars.size() < 100) {
            cars.add(type);
            if (type == CarType.CARGO) choose(TrainClass.CARGO);
            rebuildWidgets();
        }
    }

    private void removeLast() {
        if (!cars.isEmpty()) {
            cars.remove(cars.size() - 1);
            if (!hasCargo() && trainClass == TrainClass.CARGO) choose(TrainClass.PASSENGER);
            scrollOffset = Math.min(scrollOffset, Math.max(0, desiredHeight() - layout().panelHeight()));
            rebuildWidgets();
        }
    }

    private Component lineLabel() {
        return lineIndex < 0 ? Component.translatable("screen.fabric_train_simulator.depot.no_line")
                : Component.literal(lines.get(lineIndex).name() + " ▼");
    }

    private Component pauseLabel() {
        return Component.translatable(lineStatus == LineStatus.PAUSED.ordinal()
                ? "screen.fabric_train_simulator.depot.continue"
                : "screen.fabric_train_simulator.depot.pause");
    }

    private String speedText(TrainClass value) {
        return String.format(Locale.ROOT, "%.1f", speeds[value.ordinal()]);
    }

    private void commitSpeed() {
        if (speedInput == null) return;
        try {
            speeds[trainClass.ordinal()] = SpeedRules.sanitizeConfiguredLimit(
                    Double.parseDouble(speedInput.getValue()), speeds[trainClass.ordinal()]);
        } catch (NumberFormatException ignored) {}
        speedInput.setValue(speedText(trainClass));
    }

    private void updateButtons() {
        if (passengerButton != null) passengerButton.active = !hasCargo();
        if (expressButton != null) expressButton.active = !hasCargo();
        if (removeButton != null) removeButton.active = !cars.isEmpty();
        if (pauseButton != null) {
            pauseButton.active = lineIndex >= 0 && (lineStatus == LineStatus.RUNNING.ordinal()
                    || lineStatus == LineStatus.PAUSED.ordinal());
            pauseButton.setMessage(pauseLabel());
        }
        if (dispatchButton != null) dispatchButton.active = lineIndex >= 0;
        if (trainListButton != null) trainListButton.active = lineIndex >= 0;
    }

    private void send(DepotSettingsAction action) {
        commitSpeed();
        long line = lineIndex < 0 ? 0 : lines.get(lineIndex).id();
        LegacyClientPlayNetworking.send(new DepotNetworking.UpdatePayload(depotPos, nameInput.getValue(),
                new DepotTemplate(cars).encode(), line, trainClass.ordinal(), interval, maximum,
                speeds[TrainClass.CARGO.ordinal()], speeds[TrainClass.PASSENGER.ordinal()],
                speeds[TrainClass.EXPRESS.ordinal()], action.ordinal()));
    }

    @Override
    public void onClose() {
        LegacyClientPlayNetworking.send(new DepotNetworking.ClosePayload(depotPos));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        ResponsiveScreenLayout layout = layout();
        int top = layout.top();
        graphics.fill(layout.left(), top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.renderOutline(layout.left(), top, layout.panelWidth(), layout.panelHeight(), 0xFF66CCFF);
        graphics.drawCenteredString(font, title, width / 2, top + 9, 0xFFFFFFFF);
        renderText(graphics, Component.translatable("screen.fabric_train_simulator.device.name"),
                layout.left() + 10, top + 24 - scrollOffset, layout, false);
        renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.speed",
                        Component.translatable("class.fabric_train_simulator." + trainClass.name().toLowerCase(Locale.ROOT))),
                layout.left() + 10, top + 88 - scrollOffset, layout, false);
        int statusY = top + 178 - scrollOffset;
        if (insideContent(statusY, layout)) {
            graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.depot.interval", interval),
                    width / 2 - layout.panelWidth() / 4, statusY, 0xFFE6E6E6);
            graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.depot.active", active, maximum),
                    width / 2 + layout.panelWidth() / 4, statusY, 0xFFE6E6E6);
        }
        int lineStateY = top + 181 - scrollOffset;
        if (insideContent(lineStateY, layout) && automaticDispatchEnabled && lastAutomaticDispatchResult >= 0
                && lastAutomaticDispatchResult < DepotDispatchResult.values().length) {
            DepotDispatchResult result = DepotDispatchResult.values()[lastAutomaticDispatchResult];
            graphics.drawCenteredString(font, Component.translatable(
                    "screen.fabric_train_simulator.depot.automatic_status",
                    Component.translatable(result.translationKey())), width / 2,
                    lineStateY + 11, result.succeeded() ? 0xFFBFE7FF : 0xFFFFCC66);
        } else if (insideContent(lineStateY, layout) && pauseState >= 0) {
            String stateKey = pauseState == 2 ? "paused" : pauseState == 1 ? "waiting" : "running";
            graphics.drawCenteredString(font, Component.translatable(
                    "screen.fabric_train_simulator.depot.operation_" + stateKey), width / 2,
                    lineStateY + 11, 0xFFBFE7FF);
        }
        int operationY = layout.footerY() - 12;
        if (!operationMessageKey.isBlank()) {
            graphics.drawCenteredString(font, Component.translatable(
                    "screen.fabric_train_simulator.depot.operation_result",
                    Component.translatable(operationMessageKey)), width / 2, operationY, 0xFFFFCC66);
        }
        int sequenceY = top + 260 - scrollOffset;
        renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.carriage_sequence"),
                layout.left() + 10, sequenceY, layout, false);
        if (cars.isEmpty()) {
            renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.empty_hint"),
                    layout.left() + 18, sequenceY + 13, layout, false);
        } else {
            for (int i = 0; i < cars.size(); i++) {
                Component type = Component.translatable(cars.get(i) == CarType.CARGO
                        ? "item.fabric_train_simulator.cargo_car" : "item.fabric_train_simulator.passenger_car");
                renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.carriage_item", i + 1, type),
                        layout.left() + 18, sequenceY + 13 + i * 11, layout, false);
            }
        }
        super.render(graphics, mouseX, mouseY, tickProgress);
    }

    private void renderText(GuiGraphics graphics, Component text, int x, int y,
                            ResponsiveScreenLayout layout, boolean centered) {
        if (!insideContent(y, layout)) return;
        if (centered) graphics.drawCenteredString(font, text, width / 2, y, 0xFFE6E6E6);
        else graphics.drawString(font, text, x, y, 0xFFE6E6E6);
    }
}



