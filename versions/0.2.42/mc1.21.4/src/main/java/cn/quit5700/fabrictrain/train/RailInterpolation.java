package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Ordered network targets and interpolation on the same rail edges as the server. */
public final class RailInterpolation {
    private final ArrayDeque<Vec3> pending = new ArrayDeque<>();
    private Vec3 lastTarget;
    public Vec3 step(Level level, Vec3 current, Vec3 target, int ticks) {
        if (lastTarget == null || lastTarget.distanceToSqr(target) > 1e-12) {
            List<Vec3> extension = path(level, lastTarget == null ? current : lastTarget, target);
            if (extension == null || pending.size() + extension.size() > 512) {
                pending.clear(); lastTarget = null; return null;
            }
            for (Vec3 point : extension) if (pending.isEmpty() || pending.getLast().distanceToSqr(point) > 1e-12)
                pending.addLast(point);
            lastTarget = target;
        }
        double total = length(current, pending);
        double distance = total / Math.max(1, ticks);
        Vec3 position = current;
        while (!pending.isEmpty()) {
            Vec3 end = pending.getFirst(); double available = position.distanceTo(end);
            if (available <= distance + 1e-10) { distance -= available; position = end; pending.removeFirst(); }
            else return position.lerp(end, distance / available);
        }
        return position;
    }
    public static Vec3 renderPosition(Level level, Vec3 previous, Vec3 current, float partial) {
        List<Vec3> points = path(level, previous, current);
        return points == null ? null : sample(previous, points, length(previous, points) * partial);
    }
    static double length(Vec3 start, Iterable<Vec3> points) {
        double sum = 0; for (Vec3 end : points) { sum += start.distanceTo(end); start = end; } return sum;
    }
    static Vec3 sample(Vec3 start, Iterable<Vec3> points, double distance) {
        for (Vec3 end : points) {
            double available = start.distanceTo(end);
            if (available > distance && available > 1e-10) return start.lerp(end, distance / available);
            distance -= available; start = end;
        }
        return start;
    }
    private static List<Vec3> path(Level level, Vec3 start, Vec3 target) {
        BlockPos first = rail(level, BlockPos.containing(start));
        BlockPos last = rail(level, BlockPos.containing(target));
        if (first == null || last == null || start.distanceToSqr(target) > 16384D) return null;
        var parents = new HashMap<BlockPos, BlockPos>();
        var queue = new ArrayDeque<BlockPos>(); queue.add(first); parents.put(first, first);
        while (!queue.isEmpty() && !parents.containsKey(last) && parents.size() <= 256) {
            BlockPos at = queue.removeFirst(); var exits = exits(level, at); if (exits == null) continue;
            for (Vec3i side : List.of(exits.getFirst(), exits.getSecond())) {
                BlockPos next = rail(level, at.offset(side));
                if (next == null || parents.containsKey(next)) continue;
                var other = exits(level, next);
                if (other == null || !sameSide(other.getFirst(), next, at) && !sameSide(other.getSecond(), next, at)) continue;
                parents.put(next, at); queue.addLast(next);
            }
        }
        if (!parents.containsKey(last)) return null;
        var rails = new ArrayList<BlockPos>();
        for (BlockPos at = last;; at = parents.get(at)) { rails.add(at); if (at.equals(first)) break; }
        Collections.reverse(rails); var points = new ArrayList<Vec3>();
        for (int index = 0; index < rails.size(); index++) {
            BlockPos at = rails.get(index); var pair = exits(level, at);
            Vec3i a = pair.getFirst(), b = pair.getSecond();
            BlockPos before = index > 0 ? rails.get(index - 1) : null;
            BlockPos after = index + 1 < rails.size() ? rails.get(index + 1) : null;
            boolean firstIsEntry = before != null ? sameSide(a, at, before) : after == null || sameSide(b, at, after);
            var segment = RailSegmentGeometry.between(at, firstIsEntry ? a : b, firstIsEntry ? b : a);
            if (index == 0) points.add(segment.at(segment.project(start)));
            points.add(index + 1 == rails.size() ? segment.at(segment.project(target)) : segment.exit());
        }
        return points;
    }
    private static boolean sameSide(Vec3i side, BlockPos at, BlockPos next) {
        return side.getX() == Integer.signum(next.getX() - at.getX()) && side.getZ() == Integer.signum(next.getZ() - at.getZ());
    }
    private static BlockPos rail(Level level, BlockPos p) {
        if (BaseRailBlock.isRail(level, p)) return p;
        if (BaseRailBlock.isRail(level, p.below())) return p.below();
        if (BaseRailBlock.isRail(level, p.above())) return p.above();
        return null;
    }
    private static com.mojang.datafixers.util.Pair<Vec3i, Vec3i> exits(Level level, BlockPos p) {
        var state = level.getBlockState(p);
        return state.hasProperty(RailBlock.SHAPE) ? AbstractMinecart.exits(state.getValue(RailBlock.SHAPE)) : null;
    }
}
