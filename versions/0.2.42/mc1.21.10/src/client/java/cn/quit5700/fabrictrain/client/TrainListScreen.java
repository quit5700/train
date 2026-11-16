package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.network.DepotNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.List;
import java.util.ArrayList;

public final class TrainListScreen extends Screen {
    private static TrainListScreen active;
    private static final int PANEL_WIDTH = 300;
    private static final int ROW_HEIGHT = 66;
    private List<DepotNetworking.TrainListEntry> trains;
    private final BlockPos depotPos;
    private int scrollOffset;
    private int refreshTicks;
    private final List<Button> deleteButtons = new ArrayList<>();

    public TrainListScreen(DepotNetworking.TrainListPayload payload) {
        super(
                Component.translatable("screen.fabric_train_simulator.train_list.title"));
        trains = payload.trains();
        depotPos = payload.depotPos();
        active = this;
    }

    public static boolean refreshActive(DepotNetworking.TrainListPayload payload) {
        if (active == null || !active.accepts(payload)) return false;
        active.applyPayload(payload);
        return true;
    }

    public boolean accepts(DepotNetworking.TrainListPayload payload) {
        return depotPos.equals(payload.depotPos());
    }

    public void applyPayload(DepotNetworking.TrainListPayload payload) {
        if (!accepts(payload)) return;
        if (trains.equals(payload.trains())) return;
        trains = payload.trains();
        scrollOffset = Math.min(scrollOffset, Math.max(0, trains.size() * ROW_HEIGHT));
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            ClientPlayNetworking.send(new DepotNetworking.RequestTrainListPayload(depotPos));
        }
    }

    private ResponsiveScreenLayout layout() {
        int desiredHeight = Math.max(110, 74 + trains.size() * ROW_HEIGHT);
        return ResponsiveScreenLayout.of(width, height, PANEL_WIDTH, desiredHeight, 20);
    }

    @Override
    protected void init() {
        ResponsiveScreenLayout layout = layout();
        deleteButtons.clear();
        for (DepotNetworking.TrainListEntry train : trains) {
            Button delete = Button.builder(Component.translatable(
                            "screen.fabric_train_simulator.train_list.delete"),
                    button -> ClientPlayNetworking.send(new DepotNetworking.DeleteTrainPayload(
                            depotPos, train.trainId())))
                    .bounds(0, 0, 52, 18).build();
            deleteButtons.add(addRenderableWidget(delete));
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.fabric_train_simulator.train_list.back"),
                button -> ClientPlayNetworking.send(new DepotNetworking.RequestOpenPayload(depotPos)))
                .bounds(layout.left() + 10, layout.footerY(), layout.panelWidth() - 20, 20).build());
        positionDeleteButtons(layout);
    }

    @Override
    public void onClose() {
        if (active == this) active = null;
        ClientPlayNetworking.send(new DepotNetworking.ClosePayload(depotPos));
        super.onClose();
    }

    @Override
    public void removed() {
        if (active == this) active = null;
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        ResponsiveScreenLayout layout = layout();
        int visibleHeight = Math.max(ROW_HEIGHT, layout.scrollBottom() - layout.scrollTop());
        int contentHeight = Math.max(ROW_HEIGHT, trains.size() * ROW_HEIGHT);
        int maximumOffset = Math.max(0, contentHeight - visibleHeight);
        int next = Math.clamp(scrollOffset - (int) Math.signum(vertical) * ROW_HEIGHT, 0, maximumOffset);
        if (next == scrollOffset) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        scrollOffset = next;
        positionDeleteButtons(layout);
        return true;
    }

    private void positionDeleteButtons(ResponsiveScreenLayout layout) {
        for (int index = 0; index < deleteButtons.size(); index++) {
            int y = layout.scrollTop() + index * ROW_HEIGHT - scrollOffset;
            Button button = deleteButtons.get(index);
            button.setPosition(layout.right() - 66, y + 1);
            button.visible = y >= layout.scrollTop() && y + ROW_HEIGHT <= layout.scrollBottom();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickProgress) {
        ResponsiveScreenLayout layout = layout();
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), 0xE020232A);
        graphics.submitOutline(layout.left(), layout.top(), layout.panelWidth(), layout.panelHeight(), 0xFF66CCFF);
        graphics.drawCenteredString(font, title, width / 2, layout.top() + 9, 0xFFFFFFFF);
        if (trains.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.fabric_train_simulator.train_list.empty"),
                    width / 2, layout.scrollTop() + 12, 0xFFB8B8B8);
        } else {
            int y = layout.scrollTop() - scrollOffset;
            for (DepotNetworking.TrainListEntry train : trains) {
                if (y >= layout.scrollTop() && y + ROW_HEIGHT <= layout.scrollBottom()) {
                    Component mode = Component.translatable(train.automatic()
                            ? "screen.fabric_train_simulator.train_list.automatic"
                            : "screen.fabric_train_simulator.train_list.manual");
                    graphics.drawString(font, Component.translatable("screen.fabric_train_simulator.train_list.item",
                            train.number(), mode), layout.left() + 16, y + 6, 0xFFE6E6E6);
                    Component lineInfo=Component.translatable(train.lineId()==0?"screen.fabric_train_simulator.train_list.no_line":train.lineName().isEmpty()?"screen.fabric_train_simulator.train_list.deleted_line":"screen.fabric_train_simulator.train_list.line",train.lineName().isEmpty()?Long.toString(train.lineId()):train.lineName());
                    int textY=y+19;for(var wrapped:font.split(lineInfo,layout.panelWidth()-85)){
                        if(textY+font.lineHeight<=y+42)graphics.drawString(font,wrapped,layout.left()+16,textY,0xFFBBBBBB);textY+=font.lineHeight;
                    }

                    Component history = Component.translatable("screen.fabric_train_simulator.train_list.passed_stations",
                            train.stationHistory().isEmpty()
                                    ? Component.translatable("screen.fabric_train_simulator.train_list.no_passed_stations")
                                    : Component.literal(train.stationHistory()));
                    int historyY = y + 42;
                    for (var wrapped : font.split(history, layout.panelWidth() - 32)) {
                        if (historyY + font.lineHeight > y + ROW_HEIGHT) break;
                        graphics.drawString(font, wrapped, layout.left() + 16, historyY, 0xFFBBBBBB);
                        historyY += font.lineHeight;
                    }
                    if (!train.stationHistory().isEmpty() && mouseX >= layout.left() + 16
                            && mouseX < layout.right() - 16 && mouseY >= y + 42 && mouseY < y + ROW_HEIGHT) {
                        graphics.setTooltipForNextFrame(font, history, mouseX, mouseY);
                    }

                }
                y += ROW_HEIGHT;
            }
        }
        super.render(graphics, mouseX, mouseY, tickProgress);
    }
}
