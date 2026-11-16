package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.network.FreightNetworking;
import cn.quit5700.fabrictrain.freight.FreightBrowseRules;
import net.minecraft.world.item.BlockItem;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Consumer;

public final class FreightYardScreen extends Screen {
    // 设置页打开时保持单人世界及自动列车继续运行。
    @Override public boolean isPauseScreen() { return false; }

    private final long yard;
    private JsonObject state;
    private boolean settings;
    private int page;
    private int car = 1;
    private int tabStart;
    private int itemStart;
    private int ticks;
    private boolean selectedOnly;
    private String category = "all";
    private Dropdown dropdown = Dropdown.NONE;
    private enum Dropdown { NONE, CATEGORY, RANGE }
    private EditBox name;
    private String query = "";
    private List<Item> matching = List.of();
    public FreightYardScreen(JsonObject state) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, tr("title"));
        this.state = state;
        yard = state.get("yard").getAsLong();
        page = state.get("page").getAsInt();
    }

    public static Component tr(String key, Object... args) {
        return Component.translatable("screen.fabric_train_simulator.freight." + key, args);
    }

    public void accept(JsonObject next) {
        if (next.get("yard").getAsLong() != yard) return;
        boolean layoutChanged = !Objects.equals(state.get("cars"), next.get("cars"))
                || !Objects.equals(state.get("returns"), next.get("returns"));
        boolean changedFilters = !Objects.equals(state.get("filters"), next.get("filters"));
        state = next;
        if (page == next.get("page").getAsInt() || layoutChanged) page = next.get("page").getAsInt();
        if (layoutChanged) { itemStart = 0; rebuildWidgets(); }
        else if (changedFilters && selectedOnly) refreshItems();
    }

    private int count() { return state.getAsJsonArray("cars").size(); }
    private boolean split() { return count() > 0; }
    private int panelWidth() { return Math.min(338, width - 8); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return Math.max(4, (height - 204) / 2); }
    private int shownTabs() { return Math.max(1, (panelWidth() - 20) / 61); }
    private int columns() { return settings ? Math.max(1, (panelWidth() - 24) / 18) : 9; }
    private int gridX() { return left() + (panelWidth() - 18 * columns()) / 2; }

    @Override protected void init() {
        int x = left() + 10, y = top(), w = panelWidth() - 20;
        String label = name == null ? state.get("name").getAsString() : name.getValue();
        name = addRenderableWidget(new EditBox(font, x, y + 19, Math.max(36, w - 113), 20, tr("name")));
        name.setValue(label);
        name.setMaxLength(64);
        button(tr("save_name"), x + w - 110, y + 19, 52, () ->
                request("name", o -> o.addProperty("name", name.getValue())));
        button(tr(settings ? "back" : "settings"), x + w - 55, y + 19, 55, () -> {
            settings = !settings;
            tabStart = 0;
            dropdown = Dropdown.NONE;
            rebuildWidgets();
        });
        int total = settings || split() ? count() : 1;
        tabStart = Math.max(0, Math.min(tabStart, total - shownTabs()));
        car = Math.max(1, Math.min(car, count()));
        if (total > shownTabs()) {
            int max = total - shownTabs();
            addRenderableWidget(new AbstractSliderButton(x, y + 78, w, 10,
                    Component.empty(), (double) tabStart / max) {
                @Override protected void updateMessage() {}
                @Override protected void applyValue() { tabStart = (int) Math.round(value * max); }
            });
        }
        if (settings) {
            int control = (w - 12) / 3;
            int rangeX = x + control + 6;
            int searchX = rangeX + control + 6;
            button(tr("category." + category).copy().append(" ▼"), x, y + 95, control,
                    () -> toggleDropdown(Dropdown.CATEGORY));
            button(tr(selectedOnly ? "selected" : "all_items").copy().append(" ▼"), rangeX, y + 95, control,
                    () -> toggleDropdown(Dropdown.RANGE));
            var search = addRenderableWidget(new EditBox(font, searchX, y + 95, x + w - searchX, 18, tr("search")));
            search.setHint(tr("search"));
            search.setMaxLength(128);
            search.setValue(query);
            search.setResponder(value -> {
                query = value;
                itemStart = 0;
                refreshItems();
            });
            if (dropdown == Dropdown.CATEGORY) {
                int column = (w - 8) / 3;
                for (int i = 0; i < FreightBrowseRules.CATEGORIES.size(); i++) {
                    String option = FreightBrowseRules.CATEGORIES.get(i);
                    button(tr("category." + option), x + (i % 3) * (column + 4), y + 115 + (i / 3) * 17, column,
                            () -> { category = option; itemStart = 0; closeDropdown(); });
                }
            } else if (dropdown == Dropdown.RANGE) {
                button(tr("selected"), rangeX, y + 115, control,
                        () -> { selectedOnly = true; itemStart = 0; closeDropdown(); });
                button(tr("all_items"), rangeX, y + 133, control,
                        () -> { selectedOnly = false; itemStart = 0; closeDropdown(); });
            } else {
                button(Component.literal("<"), x, y + 176, 24, () -> slideItems(-1));
                button(Component.literal(">"), x + w - 24, y + 176, 24, () -> slideItems(1));
            }
            refreshItems();
        } else if (state.get("returns").getAsBoolean()) {
            button(tr("returns"), x, y + 84, 78, () -> {
                page = -1;
                request("refresh", o -> {});
            });
        }
    }

    private void toggleDropdown(Dropdown value) {
        dropdown = dropdown == value ? Dropdown.NONE : value;
        rebuildWidgets();
    }

    private void closeDropdown() {
        dropdown = Dropdown.NONE;
        rebuildWidgets();
    }

    private void slideItems(int direction) {
        int step = columns() * 3;
        itemStart = Math.max(0, Math.min(itemStart + direction * step,
                (Math.max(0, matching.size() - 1) / step) * step));
    }

    private void button(Component text, int x, int y, int w, Runnable action) {
        addRenderableWidget(Button.builder(text, b -> action.run()).bounds(x, y, w, 18).build());
    }

    private Component carName(int index) {
        return tr("car", state.getAsJsonArray("cars").get(index - 1).getAsInt());
    }

    private List<Integer> colors(String id) {
        var colors = new ArrayList<Integer>();
        JsonArray rules = state.getAsJsonArray("filters");
        for (int i = 0; i < rules.size(); i++)
            for (JsonElement item : rules.get(i).getAsJsonArray())
                if (item.getAsString().equals(id)) { colors.add(color(i + 1)); break; }
        return colors;
    }

    private int color(int index) {
        return java.awt.Color.HSBtoRGB(((index - 1) * 0.61803399f + 0.12f) % 1f, 0.69f, 0.94f);
    }

    private void refreshItems() {
        Set<String> active = new HashSet<>();
        if (selectedOnly && car <= count())
            for (JsonElement item : state.getAsJsonArray("filters").get(car - 1).getAsJsonArray()) active.add(item.getAsString());
        String needle = query.strip().toLowerCase(Locale.ROOT);
        List<Item> result = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (item == Items.AIR || !FreightBrowseRules.visible(id, item instanceof BlockItem, category, selectedOnly, active)) continue;
            if (needle.isEmpty() || id.contains(needle) || Component.translatable(item.getDescriptionId()).getString()
                    .toLowerCase(Locale.ROOT).contains(needle)) result.add(item);
        }
        matching = result;
        int step = columns() * 3;
        itemStart = Math.min(itemStart, (Math.max(0, matching.size() - 1) / step) * step);
    }

    @Override public void tick() {
        super.tick();
        if (++ticks % 20 == 0) request("refresh", o -> {});
    }

    private void request(String action, Consumer<JsonObject> additional) {
        JsonObject out = new JsonObject();
        out.addProperty("action", action);
        out.addProperty("yard", yard);
        out.addProperty("page", page);
        additional.accept(out);
        ClientPlayNetworking.send(new FreightNetworking.Request(out.toString()));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = left(), y = top();
        g.fill(x, y, x + panelWidth(), y + 204, 0xF020232A);
        outline(g, x, y, panelWidth(), 204, 0xFF66CCFF);
        g.text(font, tr("title"), x + 10, y + 6, 0xFFFFFFFF);
        g.text(font, tr("line", state.get("line").getAsString()), x + 10, y + 42, 0xFFCCCCCC);
        int total = settings || split() ? count() : 1;
        int visible = Math.min(total, tabStart + shownTabs());
        for (int index = tabStart; index < visible; index++) {
            int slot = index + 1;
            int tabX = x + 10 + 61 * (index - tabStart);
            boolean current = settings ? car == slot : page == (split() ? slot : 0);
            g.fill(tabX, y + 55, tabX + 59, y + 77, current ? 0xFF50677C : 0xFF363D48);
            outline(g, tabX, y + 55, 59, 22, current ? 0xFF83D8FF : 0xFF677584);
            g.text(font, settings || split() ? carName(slot) : tr("default"), tabX + 3, y + 60, 0xFFFFFFFF);
            if (settings || split()) g.fill(tabX + 3, y + 73, tabX + 55, y + 76, color(slot));
        }
        if (dropdown == Dropdown.NONE && (!settings || count() > 0)) drawItems(g, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawItems(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int columns = columns();
        int startY = top() + (settings ? 117 : 105);
        JsonArray inventory = state.getAsJsonArray("items");
        if (!settings && state.get("page").getAsInt() != page) return;
        for (int i = 0; i < columns * (settings ? 3 : 4); i++) {
            int x = gridX() + 18 * (i % columns), y = startY + 18 * (i / columns);
            ItemStack item = ItemStack.EMPTY;
            String id = "";
            if (settings && itemStart + i < matching.size()) {
                Item chosen = matching.get(itemStart + i);
                id = BuiltInRegistries.ITEM.getKey(chosen).toString();
                item = chosen.getDefaultInstance();
            } else if (!settings && !inventory.get(i).isJsonNull() && Minecraft.getInstance().level != null) {
                item = ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess()),
                        inventory.get(i)).result().orElse(ItemStack.EMPTY);
            }
            g.fill(x, y, x + 17, y + 17, 0xFF3D424B);
            if (settings && !id.isEmpty()) {
                List<Integer> colors = colors(id);
                for (int j = 0; j < colors.size(); j++)
                    g.fill(x + j * 17 / colors.size(), y, x + (j + 1) * 17 / colors.size(), y + 17, colors.get(j));
            }
            if (!item.isEmpty()) {
                g.item(item, x, y);
                if (!settings) g.itemDecorations(font, item, x, y);
                if (mouseX >= x && mouseX < x + 17 && mouseY >= y && mouseY < y + 17)
                    g.setTooltipForNextFrame(font, item, mouseX, mouseY);
            }
        }
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return activate(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    private boolean activate(double mouseX, double mouseY, int button) {
        if (button != 0 || dropdown != Dropdown.NONE) return false;
        int total = settings || split() ? count() : 1;
        int visible = Math.min(shownTabs(), total - tabStart);
        if (mouseY >= top() + 55 && mouseY < top() + 77) {
            int index = FreightBrowseRules.cell(mouseX - left() - 10, 61, 59, visible);
            if (index < 0) return false;
            int slot = tabStart + index + 1;
            if (settings) {
                car = slot;
                itemStart = 0;
                refreshItems();
            } else {
                page = split() ? slot : 0;
                request("refresh", o -> {});
            }
            return true;
        }
        int columns = columns();
        int x = FreightBrowseRules.cell(mouseX - gridX(), 18, 17, columns);
        int y = FreightBrowseRules.cell(mouseY - top() - (settings ? 117 : 105), 18, 17, settings ? 3 : 4);
        if (x < 0 || y < 0) return false;
        int slot = y * columns + x;
        if (settings && car <= count() && itemStart + slot < matching.size()) {
            String item = BuiltInRegistries.ITEM.getKey(matching.get(itemStart + slot)).toString();
            request("filter", o -> {
                o.addProperty("car", car);
                o.addProperty("item", item);
            });
            return true;
        }
        if (!settings && state.get("page").getAsInt() == page && !state.getAsJsonArray("items").get(slot).isJsonNull()) {
            request("take", o -> o.addProperty("slot", slot));
            return true;
        }
        return false;
    }
}
