package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.data.LineStatus;
import cn.quit5700.fabrictrain.depot.DepotSettingsAction;
import cn.quit5700.fabrictrain.depot.DepotTemplate;
import cn.quit5700.fabrictrain.network.DepotNetworking;
import cn.quit5700.fabrictrain.train.CarType;
import cn.quit5700.fabrictrain.train.SpeedRules;
import cn.quit5700.fabrictrain.train.TrainClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
    // 设置页打开时保持单人世界及自动列车继续运行。
    @Override public boolean isPauseScreen() { return false; }

    private static final int PANEL_WIDTH = 280;
    private final BlockPos depotPos;
    private final ArrayList<CarType> cars;
    private final ArrayList<Long> cargoDestinations = new ArrayList<>();
    private List<DepotStation> cargoStations = List.of();
    private int cargoRefresh;
    private int destinationRow = -1;
    private int destinationOffset;
    private record DepotStation(long id, String name) {}

    public void acceptFreightStations(com.google.gson.JsonObject snapshot) {
        if (snapshot.get("line").getAsLong() != selectedCargoLine()) return;
        var options = new ArrayList<DepotStation>();
        for (var entry : snapshot.getAsJsonArray("stations")) {
            var row = entry.getAsJsonObject();
            options.add(new DepotStation(row.get("id").getAsLong(), row.get("name").getAsString()));
        }
        if (!options.equals(cargoStations)) {
            cargoStations = List.copyOf(options);
            rebuildWidgets();
        }
    }

    private long selectedCargoLine() {
        return lineIndex < 0 ? 0 : lines.get(lineIndex).id();
    }

    private void requestCargoStations() {
        var request = new com.google.gson.JsonObject();
        request.addProperty("action", "stations");
        request.addProperty("x", depotPos.getX());
        request.addProperty("y", depotPos.getY());
        request.addProperty("z", depotPos.getZ());
        request.addProperty("line", selectedCargoLine());
        ClientPlayNetworking.send(new cn.quit5700.fabrictrain.network.FreightNetworking.Request(request.toString()));
    }

    private Component cargoDestinationLabel(int index) {
        long selected = index < cargoDestinations.size() ? cargoDestinations.get(index) : 0;
        return cargoStations.stream().filter(station -> station.id() == selected).findFirst()
                .<Component>map(station -> Component.literal(station.name()))
                .orElseGet(() -> Component.translatable("screen.fabric_train_simulator.cargo.inherit"));
    }

    private int formationRowHeight() { return trainClass == TrainClass.CARGO ? 22 : 11; }
    private int formationY(ResponsiveScreenLayout layout) { return layout.top() + 332 - scrollOffset; }

    private final List<DepotNetworking.LineOption> lines;
    private int lineIndex = -1;
    private int lineStatus = -1;
    private int pauseState = -1;
    private final boolean automaticDispatchEnabled;
    private final String operationMessageKey;
    private final List<String> operationArguments;
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
    private int manualChoice=-1;
    private Button generateButton,stationModeButton,freeModeButton;
    private EditBox nameInput;
    private final InlineTrainSpeedEditor speedEditor;
    private final net.minecraft.client.gui.components.EditBox[] speedFields = new net.minecraft.client.gui.components.EditBox[3];
    private final double[] speeds = new double[TrainClass.values().length];
    private int scrollOffset;

    public DepotSettingsScreen(DepotNetworking.OpenPayload payload) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.fabric_train_simulator.depot.title"));
        depotPos = payload.pos();
        DepotTemplate template = DepotTemplate.decode(payload.template());
        cars = new ArrayList<>(template.cars());
        cargoDestinations.addAll(template.destinations());
        lines = payload.lines();
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).id() == payload.lineId()) lineIndex = i;
        lineStatus = payload.lineStatus();
        pauseState = payload.pauseState();
        automaticDispatchEnabled = payload.automaticDispatchEnabled();
        operationMessageKey = payload.operationMessageKey();
        operationArguments = payload.operationArguments();
        trainClass = TrainClass.values()[Math.clamp(payload.trainClass(), 0, TrainClass.values().length - 1)];
        interval = payload.interval();
        maximum = payload.maximum();
        active = payload.active();
        speeds[TrainClass.CARGO.ordinal()] = payload.cargoSpeedKmh();
        speeds[TrainClass.PASSENGER.ordinal()] = payload.passengerSpeedKmh();
        speedEditor = new InlineTrainSpeedEditor(payload.cargoSpeedKmh(), payload.passengerSpeedKmh(), payload.expressSpeedKmh());
        speeds[TrainClass.EXPRESS.ordinal()] = payload.expressSpeedKmh();
        nameInput = new EditBox(font, 240, 20, Component.translatable("screen.fabric_train_simulator.device.name"));
        nameInput.setMaxLength(64);
        nameInput.setValue(payload.name());
    }

    private int desiredHeight() {
        int messageHeight = operationMessageKey.isBlank() ? 0 :
                font.split(operationMessage(),Math.max(1,Math.min(PANEL_WIDTH,width-8)-20)).size()*font.lineHeight+12;
        return Math.max(406, 398 + (cars.size() + 1) * formationRowHeight()) + messageHeight;
    }

    private Component operationMessage() {
        return Component.translatable("screen.fabric_train_simulator.depot.operation_result",
                Component.translatable(operationMessageKey, operationArguments.toArray()));
    }

    private ResponsiveScreenLayout layout() {
        return ResponsiveScreenLayout.of(width, height, PANEL_WIDTH, desiredHeight(), 20);
    }

    @Override
    protected void init() {
        ResponsiveScreenLayout layout = layout();
        if (destinationRow >= 0) {
            addCargoDropdowns(layout);
            return;
        }
        int top = layout.top();
        int contentWidth = layout.panelWidth() - 20;
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

        TrainClass[] speedTypes = TrainClass.values();
        for (int i = 0; i < speedTypes.length; i++) {
            speedFields[speedTypes[i].ordinal()] = addContent(speedEditor.field(font,
                    layout.left() + 10 + i * (classWidth + 6), top + 100 - scrollOffset,
                    classWidth, speedTypes[i]), layout);
        }

        int lineY = top + 126 - scrollOffset;
        lineButton = addContent(Button.builder(lineLabel(), button -> {
            lineIndex = lines.isEmpty() ? -1 : (lineIndex >= lines.size() - 1 ? -1 : lineIndex + 1);
            lineStatus = lineIndex < 0 ? -1 : lines.get(lineIndex).status();
            pauseState = lineStatus == LineStatus.PAUSED.ordinal() ? 1
                    : lineStatus == LineStatus.RUNNING.ordinal() ? 0 : -1;
            button.setMessage(lineLabel());
            updateButtons();
        }).bounds(layout.left() + 10, lineY, contentWidth - 82, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.edit_line"),
                button -> { if (lineIndex >= 0) send(DepotSettingsAction.OPEN_LINE); })
                .bounds(layout.left() + contentWidth - 68, lineY, 78, 20).build(), layout);

        int controlsY = top + 152 - scrollOffset;
        int quarter = Math.max(40, (contentWidth - 12) / 4);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.minus_five"),
                button -> interval = Math.max(1, interval - 5)).bounds(layout.left() + 10, controlsY, quarter, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.plus_five"),
                button -> interval = Math.min(3600, interval + 5)).bounds(layout.left() + 14 + quarter, controlsY, quarter, 20).build(), layout);
        addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.minus_one"),
                button -> maximum = Math.max(0, maximum - 1)).bounds(layout.left() + 18 + quarter * 2, controlsY, quarter, 20).build(), layout);
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
                button -> removeLast()).bounds(layout.left() + 10, actionY, contentWidth, 20).build(), layout);
        generateButton=addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.generate_now"),
                button -> send(DepotSettingsAction.GENERATE_MANUAL)).bounds(layout.left() + 10, actionY + 24, contentWidth, 20).build(), layout);
        stationModeButton=addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.station_mode"),b->{manualChoice=0;updateButtons();})
                .bounds(layout.left()+10,actionY+48,half,20).build(),layout);
        freeModeButton=addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.free_mode"),b->{manualChoice=1;updateButtons();})
                .bounds(layout.left()+16+half,actionY+48,half,20).build(),layout);
        dispatchButton = addContent(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.dispatch_now"),
                button -> send(DepotSettingsAction.DISPATCH_AUTOMATIC)).bounds(layout.left() + 10, actionY + 72, contentWidth, 20).build(), layout);

        int footerThird = (contentWidth - 12) / 3;
        pauseButton = addRenderableWidget(Button.builder(pauseLabel(),
                button -> send(DepotSettingsAction.TOGGLE_PAUSE))
                .bounds(layout.left() + 10, layout.footerY(), footerThird, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.save"),
                button -> send(DepotSettingsAction.SAVE))
                .bounds(layout.left() + 16 + footerThird, layout.footerY(), footerThird, 20).build());
        trainListButton = addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.depot.train_list"),
                button -> ClientPlayNetworking.send(new DepotNetworking.RequestTrainListPayload(depotPos)))
                .bounds(layout.left() + 22 + footerThird * 2, layout.footerY(), footerThird, 20).build());
        if (trainClass == TrainClass.CARGO) addCargoDropdowns(layout);
        updateButtons();
    }

    private void addCargoDropdowns(ResponsiveScreenLayout layout) {
        int x = layout.left() + layout.panelWidth() / 2, width = layout.right() - x - 10;
        if (destinationRow >= 0) {
            int firstY = layout.top() + 29;
            int rows = Math.max(1, Math.min(6, (layout.footerY() - firstY - 21) / 20));
            destinationOffset = Math.max(0, Math.min(destinationOffset, cargoStations.size() + 1 - rows));
            addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.cargo.destination_label"),
                    button -> { destinationRow = -1; rebuildWidgets(); })
                    .bounds(x - 60, firstY - 20, width + 60, 19).build());
            for (int row = 0; row < rows && destinationOffset + row <= cargoStations.size(); row++) {
                int option = destinationOffset + row;
                Component label = option == 0 ? Component.translatable("screen.fabric_train_simulator.cargo.inherit")
                        : Component.literal(cargoStations.get(option - 1).name());
                addRenderableWidget(Button.builder(label, button -> {
                    while (cargoDestinations.size() <= destinationRow) cargoDestinations.add(0L);
                    cargoDestinations.set(destinationRow, option == 0 ? 0L : cargoStations.get(option - 1).id());
                    destinationRow = -1;
                    rebuildWidgets();
                }).bounds(x - 60, firstY + row * 20, width + 60, 19).build());
            }
            return;
        }
        for (int i = 0; i <= cars.size(); i++) {
            int index = i;
            if (i != 0 && cars.get(i - 1) != CarType.CARGO) continue;
            addContent(Button.builder(cargoDestinationLabel(i).copy().append(" ▼"), button -> {
                destinationRow = index;
                destinationOffset = 0;
                rebuildWidgets();
            }).bounds(x, formationY(layout) + 13 + i * formationRowHeight(), width, 18).build(), layout);
        }
    }

    @Override public void tick() {
        super.tick();
        if (trainClass == TrainClass.CARGO && cargoRefresh++ % 12 == 0) requestCargoStations();
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
        if (destinationRow >= 0) {
            destinationOffset += vertical > 0 ? -1 : 1;
            rebuildWidgets();
            return true;
        }
        ResponsiveScreenLayout layout = layout();
        if (!layout.scrollRequired()) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        int maximumOffset = Math.max(0, desiredHeight() - layout.panelHeight());
        int next = Math.clamp(scrollOffset - (int) Math.signum(vertical) * 20, 0, maximumOffset);
        if (next == scrollOffset) return false;
        scrollOffset = next;
        rebuildWidgets();
        return true;
    }

    private boolean hasCargo() { return cars.contains(CarType.CARGO); }

    private void choose(TrainClass value) {
        trainClass = hasCargo() ? TrainClass.CARGO : value;
        rebuildWidgets();
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
            cars.removeLast();
            while (cargoDestinations.size() > cars.size() + 1) cargoDestinations.remove(cargoDestinations.size() - 1);
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


    private void updateButtons() {
        var spawnTip=net.minecraft.client.gui.components.Tooltip.create(Component.translatable("screen.fabric_train_simulator.depot.spawn_tail_hint",cars.size(),(int)Math.ceil(cars.size()*1.25D)));
        generateButton.setTooltip(spawnTip);dispatchButton.setTooltip(spawnTip);
        if (passengerButton != null) passengerButton.active = !hasCargo();
        if (expressButton != null) expressButton.active = !hasCargo();
        if (removeButton != null) removeButton.active = !cars.isEmpty();
        if (pauseButton != null) {
            pauseButton.active = lineIndex >= 0 && (lineStatus == LineStatus.RUNNING.ordinal()
                    || lineStatus == LineStatus.PAUSED.ordinal());
            pauseButton.setMessage(pauseLabel());
        }
        if(generateButton!=null)generateButton.active=manualChoice>=0;
        if(stationModeButton!=null)stationModeButton.active=manualChoice!=0;
        if(freeModeButton!=null)freeModeButton.active=manualChoice!=1;
        if (dispatchButton != null) dispatchButton.active = lineIndex >= 0;
        if (trainListButton != null) trainListButton.active = true;
    }

    public void refreshSpeeds(cn.quit5700.fabrictrain.network.TrainClassNetworking.OpenPayload payload) {
        speedEditor.refresh(payload);
        rebuildWidgets();
    }

    private void send(DepotSettingsAction action) {
        if (action != DepotSettingsAction.OPEN_LINE && !speedEditor.save()) {
            scrollOffset = 0;
            rebuildWidgets();
            setFocused(speedFields[speedEditor.errorClass().ordinal()]);
            return;
        }
        long line = lineIndex < 0 ? 0 : lines.get(lineIndex).id();
        ClientPlayNetworking.send(new DepotNetworking.UpdatePayload(depotPos, nameInput.getValue(),
                new DepotTemplate(cars, cargoDestinations).encode(), line, trainClass.ordinal(), interval, maximum,
                speeds[TrainClass.CARGO.ordinal()], speeds[TrainClass.PASSENGER.ordinal()],
                speeds[TrainClass.EXPRESS.ordinal()], action.ordinal(),manualChoice));
    }

    @Override
    public void onClose() {
        ClientPlayNetworking.send(new DepotNetworking.ClosePayload(depotPos));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        ResponsiveScreenLayout layout = layout();
        int top = layout.top();
        graphics.fill(layout.left(), top, layout.right(), layout.bottom(), 0xE020232A);
        graphics.renderOutline(layout.left(), top, layout.panelWidth(), layout.panelHeight(), 0xFF66CCFF);
        graphics.drawCenteredString(font, title, width / 2, top + 9, 0xFFFFFFFF);
        if (destinationRow >= 0) {
            super.render(graphics, mouseX, mouseY, tickProgress);
            return;
        }
        if (speedEditor.hasError()) renderText(graphics, speedEditor.label(speedEditor.errorClass()),
                layout.left() + 10, top + 87 - scrollOffset, layout, true);
        renderText(graphics, Component.translatable("screen.fabric_train_simulator.device.name"),
                layout.left() + 10, top + 24 - scrollOffset, layout, false);
        int statusY = top + 178 - scrollOffset;
        if (insideContent(statusY, layout)) {
            graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.depot.interval", interval),
                    width / 2 - layout.panelWidth() / 4, statusY, 0xFFE6E6E6);
            graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.depot.active", active, maximum),
                    width / 2 + layout.panelWidth() / 4, statusY, 0xFFE6E6E6);
        }
        int lineStateY = top + 181 - scrollOffset;
        if (insideContent(lineStateY, layout) && pauseState >= 0) {
            String stateKey = pauseState == 2 ? "paused" : pauseState == 1 ? "waiting" : "running";
            graphics.drawCenteredString(font, Component.translatable(
                    "screen.fabric_train_simulator.depot.operation_" + stateKey), width / 2,
                    lineStateY + 11, 0xFFBFE7FF);
        }
        int sequenceY = formationY(layout);
        renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.carriage_sequence"),
                layout.left() + 10, sequenceY, layout, false);
        renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.carriage_item", 1,
                Component.translatable("item.fabric_train_simulator.locomotive")),
                layout.left() + 18, sequenceY + 16, layout, false);
        if (cars.isEmpty()) {
            renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.empty_hint"),
                    layout.left() + 18, sequenceY + 13 + formationRowHeight(), layout, false);
        } else {
            for (int i = 0; i < cars.size(); i++) {
                Component type = Component.translatable(cars.get(i) == CarType.CARGO
                        ? "item.fabric_train_simulator.cargo_car" : "item.fabric_train_simulator.passenger_car");
                renderText(graphics, Component.translatable("screen.fabric_train_simulator.depot.carriage_item", i + 2, type),
                        layout.left() + 18, sequenceY + 16 + (i + 1) * formationRowHeight(), layout, false);
            }
        }
        if (!operationMessageKey.isBlank()) {
            int operationY=sequenceY + 24 + (cars.size() + 1) * formationRowHeight();
            for(var line:font.split(operationMessage(),layout.panelWidth()-20)) {
                if(insideContent(operationY,layout))graphics.drawString(font,line,layout.left()+10,operationY,0xFFFFCC66,false);
                operationY+=font.lineHeight;
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
