package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class RailPowerService {
    private static final int MAX_WIRE_BLOCKS = 20_000;
    private static final int MAX_RAIL_BLOCKS = 100_000;
    private static final Map<ServerLevel, Map<BlockPos, GeneratorState>> GENERATORS = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> POWERED_RAILS = new WeakHashMap<>();
    private static final Set<ServerLevel> DIRTY_LEVELS = Collections.newSetFromMap(new WeakHashMap<>());

    private RailPowerService() {}

    public static synchronized void updateGenerator(ServerLevel level, BlockPos pos, boolean active) {
        Map<BlockPos, GeneratorState> generators = GENERATORS.computeIfAbsent(level, ignored -> new HashMap<>());
        GeneratorState previous = generators.put(pos.immutable(), new GeneratorState(active,
                level.getServer().getTickCount()));
        if (previous == null || previous.active != active) DIRTY_LEVELS.add(level);
    }

    public static synchronized void removeGenerator(ServerLevel level, BlockPos pos) {
        Map<BlockPos, GeneratorState> states = GENERATORS.get(level);
        if (states != null && states.remove(pos) != null) DIRTY_LEVELS.add(level);
    }

    public static synchronized boolean isPowered(ServerLevel level, BlockPos pos) {
        Set<BlockPos> powered = POWERED_RAILS.getOrDefault(level, Set.of());
        return powered.contains(pos);
    }

    public static void tick(MinecraftServer server) {
        synchronized (RailPowerService.class) {
            if (DIRTY_LEVELS.isEmpty()) return;
            for (ServerLevel level : List.copyOf(DIRTY_LEVELS)) rebuild(level, server.getTickCount());
            DIRTY_LEVELS.clear();
        }
    }

    public static synchronized void refreshNow(ServerLevel level) {
        rebuild(level, level.getServer().getTickCount());
        DIRTY_LEVELS.remove(level);
    }

    public static synchronized void markDirty(ServerLevel level) { DIRTY_LEVELS.add(level); }

    private static void rebuild(ServerLevel level, int tick) {
        Map<BlockPos, GeneratorState> states = GENERATORS.computeIfAbsent(level, ignored -> new HashMap<>());
        states.entrySet().removeIf(entry -> tick - entry.getValue().lastSeenTick > 40);
        Set<BlockPos> oldPowered = POWERED_RAILS.getOrDefault(level, Set.of());
        HashSet<BlockPos> powered = new HashSet<>();
        for (BlockPos old : oldPowered) if (!level.hasChunkAt(old)) powered.add(old);
        for (Map.Entry<BlockPos, GeneratorState> entry : states.entrySet()) {
            if (!entry.getValue().active) continue;
            traceWires(level, entry.getKey(), powered);
        }
        POWERED_RAILS.put(level, Set.copyOf(powered));
        HashSet<BlockPos> changed = new HashSet<>(oldPowered);
        changed.addAll(powered);
        changed.removeIf(pos -> oldPowered.contains(pos) == powered.contains(pos));
        if (!changed.isEmpty()) PowerSectionCache.refreshChanged(level, changed);
    }

    private static void traceWires(ServerLevel level, BlockPos generator, Set<BlockPos> poweredRails) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos next = generator.relative(direction);
            if (isWire(level.getBlockState(next))) queue.add(next);
        }
        while (!queue.isEmpty() && visited.size() < MAX_WIRE_BLOCKS) {
            BlockPos wire = queue.removeFirst();
            if (!visited.add(wire) || !level.hasChunkAt(wire)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = wire.relative(direction);
                BlockState state = level.getBlockState(next);
                if (isWire(state) && !visited.contains(next)) queue.add(next);
                else if (isRail(state)) floodRails(level, next, poweredRails);
            }
        }
    }

    private static void floodRails(ServerLevel level, BlockPos start, Set<BlockPos> powered) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && powered.size() < MAX_RAIL_BLOCKS) {
            BlockPos rail = queue.removeFirst();
            if (powered.contains(rail) || !level.hasChunkAt(rail) || !isRail(level.getBlockState(rail))) continue;
            powered.add(rail.immutable());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos beside = rail.relative(direction);
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos candidate = beside.offset(0, dy, 0);
                    if (!powered.contains(candidate) && isRail(level.getBlockState(candidate))) queue.add(candidate);
                }
            }
        }
    }

    private static boolean isWire(BlockState state) { return state.is(Blocks.REDSTONE_WIRE); }
    private static boolean isRail(BlockState state) { return state.is(ModBlocks.SIGNAL_RAIL) || state.is(ModBlocks.SWITCH_RAIL); }
    public static synchronized void clear() { GENERATORS.clear(); POWERED_RAILS.clear(); DIRTY_LEVELS.clear(); }
    private record GeneratorState(boolean active, int lastSeenTick) {}
}
