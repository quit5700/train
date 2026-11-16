package cn.quit5700.fabrictrain.freight;

import cn.quit5700.fabrictrain.offline.InfrastructureData.Pos;
import cn.quit5700.fabrictrain.offline.InfrastructureData.Slot;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Authoritative cargo-yard inventory. Page zero is the shared default page. */
public final class FreightYard {
    public static final int SLOTS = 36;
    public static final int MAX_CARS = 100;

    private record Page(int index, List<String> filters, List<Slot> stock) {
        static final Codec<Page> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("index").forGetter(Page::index),
                Codec.STRING.listOf().fieldOf("filters").forGetter(Page::filters),
                Slot.CODEC.listOf().fieldOf("stock").forGetter(Page::stock)).apply(i, Page::new));
    }
    private record ReturnHold(Pos source, String item, int count) {
        static final Codec<ReturnHold> CODEC = RecordCodecBuilder.create(i -> i.group(
                Pos.CODEC.fieldOf("source").forGetter(ReturnHold::source),
                Codec.STRING.fieldOf("item").forGetter(ReturnHold::item),
                Codec.INT.fieldOf("count").forGetter(ReturnHold::count)).apply(i, ReturnHold::new));
    }

    public static final Codec<FreightYard> CODEC = RecordCodecBuilder.create(i -> i.group(
            Pos.CODEC.fieldOf("position").forGetter(y -> y.pos),
            Codec.LONG.fieldOf("device").forGetter(y -> y.id),
            Codec.BOOL.optionalFieldOf("per_car", false).forGetter(y -> y.perCar),
            Codec.LONG.listOf().optionalFieldOf("line_order", List.of()).forGetter(y -> List.copyOf(y.lineOrder)),
            Page.CODEC.listOf().optionalFieldOf("pages", List.of()).forGetter(FreightYard::savePages),
            Slot.CODEC.listOf().optionalFieldOf("returns", List.of()).forGetter(y -> slots(y.returns)),
            Pos.CODEC.optionalFieldOf("source").forGetter(y -> Optional.ofNullable(y.lastSource)),
            ReturnHold.CODEC.listOf().optionalFieldOf("return_holds", List.of()).forGetter(y -> List.copyOf(y.heldReturns)),
            Codec.unboundedMap(Codec.STRING, Pos.CODEC).optionalFieldOf("origins", Map.of()).forGetter(y -> Map.copyOf(y.origins)))
            .apply(i, FreightYard::new));

    public final Pos pos;
    public final long id;
    public boolean perCar;
    public final List<Long> lineOrder = new ArrayList<>();
    public final SimpleContainer returns = new SimpleContainer(SLOTS);
    public Pos lastSource;
    private final Map<Integer, SimpleContainer> storage = new TreeMap<>();
    private final Map<Integer, Set<String>> rules = new TreeMap<>();
    private final Map<String, Integer> cursor = new HashMap<>();
    private final Map<String, Pos> origins = new HashMap<>();
    private final List<ReturnHold> heldReturns = new ArrayList<>();

    public FreightYard(Pos pos, long id) {
        this.pos = pos;
        this.id = id;
    }

    private FreightYard(Pos pos, long id, boolean perCar, List<Long> order, List<Page> pages,
                        List<Slot> pending, Optional<Pos> source, List<ReturnHold> holds, Map<String, Pos> origins) {
        this(pos, id);
        this.perCar = perCar;
        lineOrder.addAll(order);
        for (Page page : pages) {
            if (page.index < 0 || page.index > MAX_CARS) continue;
            restore(page(page.index), page.stock);
            filters(page.index).addAll(page.filters);
        }
        restore(returns, pending);
        lastSource = source.orElse(null);
        heldReturns.addAll(holds);
        this.origins.putAll(origins);
    }

    public SimpleContainer page(int index) {
        return storage.computeIfAbsent(index, k -> new SimpleContainer(SLOTS));
    }

    public Set<String> filters(int index) {
        return rules.computeIfAbsent(index, k -> new LinkedHashSet<>());
    }

    public Set<Integer> storedPages() {
        return Set.copyOf(storage.keySet());
    }

    public int nextPage(String item, int size) {
        return Math.floorMod(cursor.getOrDefault(item, 0), size);
    }

    public void advance(String item, int slot) {
        cursor.put(item, slot + 1);
    }

    public void rememberOrigin(int page, String item, Pos origin) {
        if (origin != null) origins.putIfAbsent(page + "|" + item, origin);
    }

    public Pos origin(int page, String item) {
        return origins.getOrDefault(page + "|" + item, lastSource);
    }

    public void forgetOrigin(int page, String item) {
        origins.remove(page + "|" + item);
    }

    public int held(Pos source, String item, int currentTotal) {
        if (source == null) return 0;
        for (int i = 0; i < heldReturns.size(); i++) {
            ReturnHold hold = heldReturns.get(i);
            if (!hold.source.equals(source) || !hold.item.equals(item)) continue;
            int remaining = Math.min(currentTotal, hold.count);
            if (remaining == 0) heldReturns.remove(i);
            else if (remaining != hold.count) heldReturns.set(i, new ReturnHold(source, item, remaining));
            return remaining;
        }
        return 0;
    }

    public void hold(Pos source, String item, int quantity, int previousTotal) {
        int retained = held(source, item, previousTotal);
        heldReturns.removeIf(hold -> hold.source.equals(source) && hold.item.equals(item));
        heldReturns.add(new ReturnHold(source, item, retained + quantity));
    }

    public boolean reconcileReturns(Pos source, java.util.function.ToIntFunction<String> stock) {
        boolean changed = false;
        for (ReturnHold hold : List.copyOf(heldReturns)) {
            if (hold.source.equals(source) && held(source, hold.item, stock.applyAsInt(hold.item)) != hold.count)
                changed = true;
        }
        return changed;
    }

    /** Returned stock becomes eligible again only when an active carriage rule accepts it. */
    public boolean releaseAcceptedReturns(Pos source, int activePages) {
        return heldReturns.removeIf(hold -> {
            if (!hold.source.equals(source)) return false;
            for (int page = 1; page <= Math.min(activePages, MAX_CARS); page++) {
                Set<String> selected = rules.get(page);
                if (selected != null && selected.contains(hold.item)) return true;
            }
            return false;
        });
    }

    public static String key(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public List<ItemStack> contents() {
        List<ItemStack> result = new ArrayList<>();
        storage.values().forEach(page -> slots(page).forEach(slot -> result.add(slot.stack())));
        slots(returns).forEach(slot -> result.add(slot.stack()));
        return result;
    }

    private List<Page> savePages() {
        Set<Integer> indices = new TreeSet<>(storage.keySet());
        indices.addAll(rules.keySet());
        return indices.stream().map(index -> new Page(index, List.copyOf(filters(index)), slots(page(index)))).toList();
    }

    private static List<Slot> slots(SimpleContainer page) {
        List<Slot> result = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (!page.getItem(i).isEmpty()) result.add(new Slot(i, page.getItem(i)));
        }
        return result;
    }

    private static void restore(SimpleContainer page, List<Slot> stock) {
        for (Slot slot : stock) {
            if (slot.slot() >= 0 && slot.slot() < SLOTS) page.setItem(slot.slot(), slot.stack().copy());
        }
    }
}
