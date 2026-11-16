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

    private static final Map<ServerLevel, Set<ChunkBoundary>> UNLOADED_BOUNDARIES = new WeakHashMap<>();

    private static final Map<ServerLevel, Map<BlockPos, Set<BlockPos>>> SOURCES = new WeakHashMap<>();
    public static Set<BlockPos> generatorPositions(ServerLevel level) { return Set.copyOf(GENERATORS.getOrDefault(level, Map.of()).keySet()); }
    public static Set<BlockPos> sourcesFor(ServerLevel level, BlockPos rail) {
        Set<BlockPos> result = new HashSet<>();
        SOURCES.getOrDefault(level,Map.of()).forEach((source,rails)->{if(rails.contains(rail))result.add(source);});
        return Set.copyOf(result);
    }
    public static boolean rawPowered(ServerLevel level, BlockPos rail) { return POWERED_RAILS.getOrDefault(level,Set.of()).contains(rail); }
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
        Boolean remote=cn.quit5700.fabrictrain.offline.InfrastructureService.remotePowerAt(level,pos);
        return remote==null?powered.contains(pos):remote;
    }

    public static void tick(MinecraftServer server) {
        synchronized (RailPowerService.class) {
            // A load event can precede getChunkNow making the chunk readable.
            // Check only chunks where an active network previously reached unloaded space.
            if (server.getTickCount() % 5 == 0) {
                UNLOADED_BOUNDARIES.forEach((level, chunks) -> {
                    if (level.getServer() == server && chunks.stream().anyMatch(chunk ->
                            level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null))
                        DIRTY_LEVELS.add(level);
                });
            }
            if (DIRTY_LEVELS.isEmpty()) return;
            for (ServerLevel level : List.copyOf(DIRTY_LEVELS)) rebuild(level, server.getTickCount());
            DIRTY_LEVELS.clear();
        }
    }

    public static synchronized void refreshNow(ServerLevel level) {
        rebuild(level, level.getServer().getTickCount());
        DIRTY_LEVELS.remove(level);
    }

    public static synchronized void markDirty(ServerLevel level) { DIRTY_LEVELS.add(level); SavedPowerPlanBridge.invalidate(level); }

    private static void rebuild(ServerLevel level, int tick) {
        Map<BlockPos, GeneratorState> states = GENERATORS.computeIfAbsent(level, ignored -> new HashMap<>());
        states.entrySet().removeIf(entry -> tick - entry.getValue().lastSeenTick > 40);
        Set<BlockPos> oldPowered = POWERED_RAILS.getOrDefault(level, Set.of());
        HashSet<BlockPos> powered = new HashSet<>();
        Set<ChunkBoundary> boundaries = new HashSet<>();
        for (BlockPos old : oldPowered) if (!level.hasChunkAt(old)) powered.add(old);
        for (Map.Entry<BlockPos, GeneratorState> entry : states.entrySet()) {
            if (!entry.getValue().active) continue;
            Set<BlockPos> sourceRails = new HashSet<>();
            traceWires(level, entry.getKey(), sourceRails, boundaries);
            SOURCES.computeIfAbsent(level,k->new HashMap<>()).put(entry.getKey(),Set.copyOf(sourceRails));
            powered.addAll(sourceRails);
        }
        if (boundaries.isEmpty()) UNLOADED_BOUNDARIES.remove(level);
        else UNLOADED_BOUNDARIES.put(level, boundaries);
        POWERED_RAILS.put(level, Set.copyOf(powered));
        HashSet<BlockPos> changed = new HashSet<>(oldPowered);
        changed.addAll(powered);
        changed.removeIf(pos -> oldPowered.contains(pos) == powered.contains(pos));
        if (!changed.isEmpty()) PowerSectionCache.refreshChanged(level, changed);
    }

    private static void traceWires(ServerLevel level, BlockPos generator, Set<BlockPos> poweredRails,
                                   Set<ChunkBoundary> boundaries) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        HashSet<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos next = generator.relative(direction);
            if (isWire(frontierState(level, next, boundaries))) queue.add(next);
        }
        while (!queue.isEmpty() && visited.size() < MAX_WIRE_BLOCKS) {
            BlockPos wire = queue.removeFirst();
            if (!visited.add(wire) || !level.hasChunkAt(wire)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos next = wire.relative(direction);
                BlockState state = frontierState(level, next, boundaries);
                if (isWire(state) && !visited.contains(next)) queue.add(next);
                else if (isRail(state)) floodRails(level, next, poweredRails, boundaries);
            }
        }
    }

    private static void floodRails(ServerLevel level, BlockPos start, Set<BlockPos> powered,
                                   Set<ChunkBoundary> boundaries) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty() && powered.size() < MAX_RAIL_BLOCKS) {
            BlockPos rail = queue.removeFirst();
            if (powered.contains(rail) || !isRail(frontierState(level, rail, boundaries))) continue;
            powered.add(rail.immutable());
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos beside = rail.relative(direction);
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos candidate = beside.offset(0, dy, 0);
                    if (!powered.contains(candidate) && isRail(frontierState(level, candidate, boundaries))) queue.add(candidate);
                }
            }
        }
    }

    private static BlockState frontierState(ServerLevel level, BlockPos pos, Set<ChunkBoundary> boundaries) {
        int x = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        var chunk = level.getChunkSource().getChunkNow(x, z);
        if (chunk != null) return chunk.getBlockState(pos);
        boundaries.add(new ChunkBoundary(x, z));
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean isWire(BlockState state) { return state.is(Blocks.REDSTONE_WIRE); }
    private static boolean isRail(BlockState state) { return state.is(ModBlocks.SIGNAL_RAIL) || state.is(ModBlocks.SWITCH_RAIL); }
    public static synchronized void clear() { PowerFeedProof.clear(); SavedPowerPlanBridge.clear(); GENERATORS.clear(); SOURCES.clear(); POWERED_RAILS.clear(); DIRTY_LEVELS.clear(); UNLOADED_BOUNDARIES.clear(); }
    private record ChunkBoundary(int x, int z) {}
    private record GeneratorState(boolean active, int lastSeenTick) {}
}
