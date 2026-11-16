package cn.quit5700.fabrictrain.power;

import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Verifies only resident portions of an already validated route, without loading chunks. */
public final class LoadedRouteProof {
    public interface View {
        Object chunk(int x, int z);
        boolean signal(Object chunk, BlockPos pos);
    }
    public static final class Budget {
        private int remaining;
        public Budget(int limit) { remaining = Math.max(0, limit); }
        public boolean take() { if (remaining == 0) return false; remaining--; return true; }
        public int remaining() { return remaining; }
    }
    public record Result(boolean hasUnloaded, boolean complete, boolean intact) {
        public boolean allowsBridge() { return hasUnloaded && complete && intact; }
    }
    private record Key(int x, int z) {}
    private static final class Group {
        final Key key;
        final List<BlockPos> positions;
        Object chunk;
        long epoch = Long.MIN_VALUE;
        int cursor;
        boolean invalid;
        Group(Key key, Set<BlockPos> positions) { this.key=key; this.positions=List.copyOf(positions); }
    }
    private final List<Group> groups;
    public LoadedRouteProof(List<TrackPoint> path) {
        Map<Key, Set<BlockPos>> chunks = new LinkedHashMap<>();
        for (TrackPoint point : path) {
            BlockPos pos = new BlockPos(point.x(),point.y(),point.z());
            chunks.computeIfAbsent(new Key(pos.getX() >> 4,pos.getZ() >> 4), k -> new LinkedHashSet<>()).add(pos);
        }
        groups = chunks.entrySet().stream().map(e -> new Group(e.getKey(),e.getValue())).toList();
    }
    public Result inspect(View view, long epoch, Budget budget) {
        boolean unloaded=false, complete=true, intact=true;
        for (Group group : groups) {
            Object chunk = view.chunk(group.key.x(), group.key.z());
            if (chunk == null) { unloaded=true; intact &= !group.invalid; group.chunk=null; continue; }
            if (chunk != group.chunk || group.epoch != epoch) {
                group.chunk=chunk; group.epoch=epoch; group.cursor=0; group.invalid=false;
            }
            while (!group.invalid && group.cursor < group.positions.size() && budget.take()) {
                if (!view.signal(chunk,group.positions.get(group.cursor++))) group.invalid=true;
            }
            intact &= !group.invalid;
            complete &= group.invalid || group.cursor == group.positions.size();
        }
        return new Result(unloaded,complete,intact && !groups.isEmpty());
    }
}
