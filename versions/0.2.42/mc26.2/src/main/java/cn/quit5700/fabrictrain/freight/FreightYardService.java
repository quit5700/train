package cn.quit5700.fabrictrain.freight;

import cn.quit5700.fabrictrain.cargo.CargoTransferService;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.depot.DepotTemplate;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.offline.InfrastructureService;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.train.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.*;

public final class FreightYardService {
    private FreightYardService() {}

    public static FreightYard yard(ServerLevel level, RailwayDevice device) {
        var data = InfrastructureService.data(level.getServer());
        var pos = new BlockPos(device.x(), device.y(), device.z());
        FreightYard yard = data.freightYards.get(device.id());
        if (yard == null) {
            yard = new FreightYard(InfrastructureService.pos(level, pos), device.id());
            data.freightYards.put(device.id(), yard);
            data.setDirty();
        }
        return yard;
    }

    public static RailwayLine activeLine(RailwaySavedData data, FreightYard yard) {
        for (long lineId : yard.lineOrder) {
            RailwayLine line = data.line(lineId);
            if (line != null && line.nodeIds().contains(yard.id)) return line;
        }
        return null;
    }

    public static boolean startsAtYard(RailwaySavedData data, RailwayLine line) {
        if (line == null || line.nodeIds().isEmpty()) return false;
        var device = data.device(line.nodeIds().get(0));
        return device != null && device.type() == DeviceType.FREIGHT_YARD;
    }

    public static void membership(ServerLevel level, long lineId) {
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayLine line = data.line(lineId);
        var infrastructure = InfrastructureService.data(level.getServer());
        if (line != null) for (long id : line.nodeIds()) {
            RailwayDevice device = data.device(id);
            if (device != null && device.type() == DeviceType.FREIGHT_YARD
                    && device.dimension().equals(RailwayDataManager.dimension(level))) yard(level, device);
        }
        for (FreightYard yard : infrastructure.freightYards.values()) {
            boolean present = line != null && line.nodeIds().contains(yard.id);
            boolean changed = false;
            if (present && !yard.lineOrder.contains(lineId)) changed = yard.lineOrder.add(lineId);
            if (!present) changed = yard.lineOrder.remove(lineId);
            if (changed) infrastructure.setDirty();
        }
    }

    public static List<Integer> cargoPositions(ServerLevel level, RailwayLine line) {
        if (line == null) return List.of();
        RailwayDevice dispatch = RailwayDataManager.state(level).device(line.depotId());
        if (dispatch == null) return List.of();
        var depot = InfrastructureService.depot(level, line.depotId());
        BlockPos pos = new BlockPos(dispatch.x(), dispatch.y(), dispatch.z());
        var block = LoadedRailWorld.entity(level, pos);
        var template = block instanceof cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity loaded
                ? loaded.template() : depot == null ? DepotTemplate.defaultTemplate() : DepotTemplate.decode(depot.config().template());
        var cars = template.cars();
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < cars.size(); index++)
            if (cars.get(index) == CarType.CARGO) positions.add(index + 2);
        return positions;
    }

    public static boolean accepts(ServerLevel level, RailwayDevice device, long lineId, TrainClass type) {
        if (type != TrainClass.CARGO || device.type() != DeviceType.FREIGHT_YARD) return false;
        var active = activeLine(RailwayDataManager.state(level), yard(level, device));
        return active != null && active.id() == lineId;
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 8 != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            RailwaySavedData registry = RailwayDataManager.state(level);
            var infrastructure = InfrastructureService.data(server);
            for (FreightYard yard : new ArrayList<>(infrastructure.freightYards.values())) {
                BlockPos pos = yard.pos.block();
                if (!yard.pos.dimension().equals(RailwayDataManager.dimension(level))) continue;
                // A chunk holder may exist before/after its readable full chunk. Unknown is not air.
                var resident = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                if (resident == null) continue;
                if (!resident.getBlockState(pos).is(ModBlocks.FREIGHT_YARD)) {
                    drop(level, pos);
                    RailwayDataManager.remove(level, pos);
                    continue;
                }
                RailwayLine line = activeLine(registry, yard);
                int count = cargoPositions(level, line).size();
                boolean split = count > 0;
                boolean changed = false;
                if (split) changed = transferSource(yard, yard.page(0), null, count, true);
                for (int page : yard.storedPages()) {
                    if (page == 0 || split && page <= count) continue;
                    changed |= returnPage(level, yard, page);
                }
                for (var input : inputs(level, pos).entrySet()) {
                    BlockPos source = input.getKey();
                    Container container = input.getValue();
                    var origin = InfrastructureService.pos(level, source);
                    boolean chest = container instanceof ChestBlockEntity || isTemporaryChest(container);
                    int limit = chest && split ? 64 : 1;
                    changed |= yard.reconcileReturns(origin, key -> total(container, key));
                    changed |= yard.releaseAcceptedReturns(origin, count);
                    boolean movedAny = false;
                    for (int moved = 0; moved < limit && transferSource(yard, container, origin, count, split); moved++)
                        movedAny = true;
                    if (chest && movedAny) yard.lastSource = origin;
                    changed |= movedAny;
                }
                if (changed) infrastructure.setDirty();
            }
        }
    }

    private static Map<BlockPos, Container> inputs(ServerLevel level, BlockPos yard) {
        Map<BlockPos, Container> inputs = new LinkedHashMap<>();
        Set<BlockPos> checked = new HashSet<>();
        Direction[] directions = Direction.values();
        for (Direction side : directions) {
            BlockPos start = yard.relative(side);
            if (inputs.containsKey(start) || !(LoadedRailWorld.entity(level, start) instanceof Container source)
                    || !isInput(level, start, side, source)) continue;
            if (!isUniversalBuffer(source)) {
                inputs.put(start, source);
                continue;
            }
            ArrayDeque<BlockPos> group = new ArrayDeque<>();
            group.add(start);
            while (!group.isEmpty()) {
                BlockPos pos = group.removeFirst();
                if (!checked.add(pos) || !(LoadedRailWorld.entity(level, pos) instanceof Container member)
                        || !isUniversalBuffer(member)) continue;
                inputs.put(pos, member);
                for (Direction direction : directions) group.addLast(pos.relative(direction));
            }
        }
        return inputs;
    }

    private static boolean isInput(ServerLevel level, BlockPos pos, Direction side, Container source) {
        if (source instanceof ChestBlockEntity || isTemporaryChest(source)) return true;
        var state = LoadedRailWorld.state(level, pos);
        return state.hasProperty(HopperBlock.FACING) && state.getValue(HopperBlock.FACING) == side.getOpposite()
                && state.getValue(HopperBlock.ENABLED);
    }

    private static boolean isUniversalBuffer(Container source) {
        return source instanceof BlockEntity entity && net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(entity.getBlockState().getBlock()).toString().equals("universal_chest:buffer");
    }

    private static boolean isTemporaryChest(Container source) {
        if (!(source instanceof BlockEntity block)) return false;
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block.getBlockState().getBlock()).toString();
        return id.equals("universal_chest:buffer") || (id.startsWith("quick_build:") || id.startsWith("building_wand:"))
                && (id.contains("temporary_chest") || id.contains("temp_chest"));
    }

    private static boolean transferSource(FreightYard yard, Container source,
                                          cn.quit5700.fabrictrain.offline.InfrastructureData.Pos origin,
                                          int pages, boolean split) {
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack.isEmpty()) continue;
            String key = FreightYard.key(stack);
            int available = total(source, key);
            int allowance = available - yard.held(origin, key, available);
            if (allowance <= 0) continue;
            var supply = origin == null ? yard.origin(0, key)
                    : source instanceof ChestBlockEntity || isTemporaryChest(source) ? origin : null;
            if (!split) {
                ItemStack remaining = stack.copyWithCount(Math.min(allowance, stack.getCount()));
                int before = remaining.getCount();
                CargoTransferService.insert(yard.page(0), remaining);
                int moved = before - remaining.getCount();
                if (moved == 0) continue;
                source.removeItem(slot, moved);
                source.setChanged();
                yard.rememberOrigin(0, key, supply);
                return true;
            }
            if (pages <= 0) return false;
            int start = yard.nextPage(key, pages);
            for (int i = 0; i < pages; i++) {
                int index = (start + i) % pages;
                if (!yard.filters(index + 1).contains(key)) continue;
                ItemStack single = stack.copyWithCount(1);
                CargoTransferService.insert(yard.page(index + 1), single);
                if (!single.isEmpty()) continue;
                source.removeItem(slot, 1);
                source.setChanged();
                yard.advance(key, index);
                yard.rememberOrigin(index + 1, key, supply);
                return true;
            }
        }
        return false;
    }

    private static int total(Container source, String key) {
        int total = 0;
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack item = source.getItem(slot);
            if (!item.isEmpty() && FreightYard.key(item).equals(key)) total += item.getCount();
        }
        return total;
    }

    private static boolean returnPage(ServerLevel level, FreightYard yard, int index) {
        Container page = yard.page(index);
        if (page.isEmpty()) return false;
        boolean changed = false;
        for (int slot = 0; slot < page.getContainerSize(); slot++) {
            ItemStack stack = page.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack remaining = stack.copy();
            String key = FreightYard.key(stack);
            var origin = yard.origin(index, key);
            if (origin != null && origin.dimension().equals(yard.pos.dimension())
                    && LoadedRailWorld.entity(level, origin.block()) instanceof Container source
                    && (source instanceof ChestBlockEntity || isTemporaryChest(source))) {
                int before = total(source, key);
                CargoTransferService.insert(source, remaining);
                if (stack.getCount() > remaining.getCount()) yard.hold(origin, key, stack.getCount() - remaining.getCount(), before);
                source.setChanged();
            }
            CargoTransferService.insert(yard.returns, remaining);
            if (remaining.getCount() == stack.getCount()) continue;
            page.setItem(slot, remaining);
            if (total(page, key) == 0) yard.forgetOrigin(index, key);
            changed = true;
        }
        return changed;
    }

    public static void loadEntities(ServerLevel level, RailwayDevice device, List<? extends Entity> formation) {
        FreightYard yard = yard(level, device);
        int available = cargoPositions(level, activeLine(RailwayDataManager.state(level), yard)).size();
        boolean split = available > 0;
        var cargo = formation.stream().filter(CargoCarEntity.class::isInstance).map(CargoCarEntity.class::cast)
                .sorted(Comparator.comparingInt(car -> car.trainLink().carIndex())).toList();
        for (int i = 0; i < cargo.size(); i++) {
            if (split && i >= available) break;
            transfer(yard.page(split ? i + 1 : 0), cargo.get(i));
        }
        InfrastructureService.data(level.getServer()).setDirty();
    }

    public static ArrayList<OfflineCarState> loadOffline(ServerLevel level, RailwayDevice device,
                                                          List<OfflineCarState> formation) {
        FreightYard yard = yard(level, device);
        int available = cargoPositions(level, activeLine(RailwayDataManager.state(level), yard)).size();
        boolean split = available > 0;
        ArrayList<OfflineCarState> result = new ArrayList<>(formation);
        int index = 0;
        for (int i = 0; i < formation.size(); i++) {
            OfflineCarState car = formation.get(i);
            if (car.type() != CarType.CARGO) continue;
            index++;
            if (split && index > available) break;
            SimpleContainer stock = new SimpleContainer(27);
            for (int slot = 0; slot < car.items().size() && slot < stock.getContainerSize(); slot++)
                stock.setItem(slot, car.items().get(slot).copy());
            transfer(yard.page(split ? index : 0), stock);
            List<ItemStack> items = new ArrayList<>();
            for (int slot = 0; slot < stock.getContainerSize(); slot++)
                if (!stock.getItem(slot).isEmpty()) items.add(stock.getItem(slot).copy());
            result.set(i, new OfflineCarState(CarType.CARGO, car.destinationStationId(), items));
        }
        InfrastructureService.data(level.getServer()).setDirty();
        return result;
    }

    private static void transfer(Container source, Container target) {
        for (int slot = 0; slot < source.getContainerSize(); slot++) {
            ItemStack item = source.getItem(slot);
            if (item.isEmpty()) continue;
            ItemStack remaining = item.copy();
            CargoTransferService.insert(target, remaining);
            if (remaining.getCount() != item.getCount()) source.setItem(slot, remaining);
        }
        source.setChanged();
        target.setChanged();
    }

    public static void drop(ServerLevel level, BlockPos pos) {
        RailwayDevice device = RailwayDataManager.state(level).find(RailwayDataManager.dimension(level),
                pos.getX(), pos.getY(), pos.getZ());
        if (device == null) return;
        var data = InfrastructureService.data(level.getServer());
        FreightYard yard = data.freightYards.remove(device.id());
        if (yard == null) return;
        for (ItemStack item : yard.contents()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item);
        }
        data.setDirty();
    }
}
