package cn.quit5700.fabrictrain.train;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import net.minecraft.core.Direction;
import cn.quit5700.fabrictrain.route.TrackPoint;
import java.util.List;
import java.util.Optional;

final class RailMotionCursor {
    private BlockPos previous;
    private BlockPos current;
    private BlockPos next;
    private double progress;
    private final List<BlockPos> route;
    private final boolean loop;
    private final int loopStartIndex;
    private int routeIndex;

    private RailMotionCursor(BlockPos previous, BlockPos current, BlockPos next) {
        this(previous, current, next, List.of(), false, 0, 0);
    }

    private RailMotionCursor(BlockPos previous, BlockPos current, BlockPos next,
                             List<BlockPos> route, boolean loop, int loopStartIndex, int routeIndex) {
        this.previous = previous;
        this.current = current;
        this.next = next;
        this.route = route;
        this.loop = loop;
        this.loopStartIndex = loopStartIndex;
        this.routeIndex = routeIndex;
    }

    static RailMotionCursor create(ServerLevel level, AbstractMinecart leader, Vec3 facing) {
        BlockPos current = railAt(level, leader.getCurrentBlockPosOrRailBelow());
        if (current == null) return null;
        var exits = exits(level, current);
        if (exits == null) return null;
        BlockPos first = railAt(level, current.offset(exits.getFirst()));
        BlockPos second = railAt(level, current.offset(exits.getSecond()));
        if (first == null && second == null) return null;
        BlockPos next;
        BlockPos previous;
        if (first == null) {
            next = second;
            previous = null;
        } else if (second == null) {
            next = first;
            previous = null;
        } else {
            Vec3 firstDirection = Vec3.atCenterOf(first).subtract(Vec3.atCenterOf(current)).normalize();
            Vec3 secondDirection = Vec3.atCenterOf(second).subtract(Vec3.atCenterOf(current)).normalize();
            if (firstDirection.dot(facing) >= secondDirection.dot(facing)) {
                next = first;
                previous = second;
            } else {
                next = second;
                previous = first;
            }
        }
        return new RailMotionCursor(previous, current, next);
    }

    static RailMotionCursor createRoute(ServerLevel level, AbstractMinecart leader, List<TrackPoint> points,
                                        boolean loop, int loopStartIndex) {
        if (points == null || points.size() < 2) return null;
        List<BlockPos> route = points.stream().map(point -> new BlockPos(point.x(), point.y(), point.z())).toList();
        BlockPos leaderRail = railAt(level, leader.getCurrentBlockPosOrRailBelow());
        if (leaderRail == null) return null;
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int index = 0; index < route.size(); index++) {
            double distance = leader.position().distanceToSqr(center(route.get(index)));
            if (distance < nearestDistance) {
                nearest = index;
                nearestDistance = distance;
            }
        }
        BlockPos current = route.get(nearest);
        if (!current.equals(leaderRail)) return null;
        int safeLoopStart = Math.clamp(loopStartIndex, 0, route.size() - 2);
        int normalized = normalizeLoopIndex(route, loop, safeLoopStart, nearest);
        current = route.get(normalized);
        BlockPos previous = normalized > 0 ? route.get(normalized - 1) : null;
        BlockPos next = routeNext(route, loop, safeLoopStart, normalized);
        if (next == null) return null;
        RailMotionCursor cursor = new RailMotionCursor(previous, current, next, route, loop, safeLoopStart, normalized);
        cursor.progress = projectedProgress(leader.position(), current, next);
        return cursor;
    }

    static RailMotionCursor restoreRoute(OfflineTrainState state) {
        List<BlockPos> route = state.path().stream()
                .map(point -> new BlockPos(point.x(), point.y(), point.z())).toList();
        int index = state.pathIndex();
        int start = Math.clamp(state.loopStartIndex(), 0, route.size() - 2);
        BlockPos current = route.get(index);
        BlockPos previous = index > 0 ? route.get(index - 1) : null;
        BlockPos next = routeNext(route, state.loop(), start, index);
        RailMotionCursor cursor = new RailMotionCursor(previous, current, next, route, state.loop(), start, index);
        if (next != null)
            cursor.progress = Math.clamp(state.segmentProgress(), 0, center(current).distanceTo(center(next)));
        return cursor;
    }

    Pose advance(ServerLevel level, double distance) {
        if (!route.isEmpty()) return advanceRoute(level, distance);
        double remaining = distance;
        int budget = 32;
        while (Math.abs(remaining) > 0.000001D && budget-- > 0) {
            if (remaining > 0) {
                if (next == null) return null;
                double length = center(current).distanceTo(center(next));
                double available = Math.max(0, length - progress);
                if (remaining <= available) {
                    progress += remaining;
                    remaining = 0;
                } else {
                    remaining -= available;
                    BlockPos oldCurrent = current;
                    current = next;
                    previous = oldCurrent;
                    next = continuation(level, current, previous);
                    progress = 0;
                }
            } else {
                if (-remaining <= progress) {
                    progress += remaining;
                    remaining = 0;
                } else {
                    remaining += progress;
                    if (previous == null) return null;
                    BlockPos oldCurrent = current;
                    current = previous;
                    next = oldCurrent;
                    previous = continuation(level, current, next);
                    progress = center(current).distanceTo(center(next));
                }
            }
        }
        if (next == null) return new Pose(center(current), 0);
        Vec3 from = center(current);
        Vec3 to = center(next);
        double length = from.distanceTo(to);
        double ratio = length < 0.0001D ? 0 : Math.max(0, Math.min(1, progress / length));
        Vec3 position = from.lerp(to, ratio);
        Vec3 tangent = to.subtract(from);
        float yaw = (float) Math.toDegrees(Math.atan2(-tangent.x, tangent.z));
        return new Pose(position, yaw);
    }

    private Pose advanceRoute(ServerLevel level, double distance) {
        if (next == null && !loop && routeIndex == route.size() - 1
                && distance >= 0 && BaseRailBlock.isRail(level,current)) return terminalPose();
        double remaining = distance;
        int budget = 32;
        while (Math.abs(remaining) > 0.000001D && budget-- > 0) {
            if (!BaseRailBlock.isRail(level, current) || next == null || !BaseRailBlock.isRail(level, next)) return null;
            if (remaining > 0) {
                double length = center(current).distanceTo(center(next));
                double available = Math.max(0, length - progress);
                if (remaining <= available) {
                    progress += remaining;
                    remaining = 0;
                } else {
                    remaining -= available;
                    previous = current;
                    current = next;
                    routeIndex = normalizeLoopIndex(route, loop, loopStartIndex, routeIndex + 1);
                    next = routeNext(route, loop, loopStartIndex, routeIndex);
                    progress = 0;
                    if (next == null && remaining > 0.000001D) return terminalPose();
                }
            } else {
                if (-remaining <= progress) {
                    progress += remaining;
                    remaining = 0;
                } else {
                    remaining += progress;
                    if (previous == null) return null;
                    next = current;
                    current = previous;
                    routeIndex = Math.max(0, routeIndex - 1);
                    previous = routeIndex > 0 ? route.get(routeIndex - 1) : null;
                    progress = center(current).distanceTo(center(next));
                }
            }
        }
        if (next == null) return terminalPose();
        Vec3 from = center(current);
        Vec3 to = center(next);
        double length = from.distanceTo(to);
        double ratio = length < 0.0001D ? 0 : Math.max(0, Math.min(1, progress / length));
        Vec3 position = from.lerp(to, ratio);
        Vec3 tangent = to.subtract(from);
        float yaw = (float) Math.toDegrees(Math.atan2(-tangent.x, tangent.z));
        return new Pose(position, yaw);
    }

    /** 到达非循环线路终点时消费多余正向步长，交给下一刻的终点节点回收。 */
    private Pose terminalPose() {
        Vec3 tangent=previous==null?new Vec3(0,0,1):center(current).subtract(center(previous));
        float yaw=(float)Math.toDegrees(Math.atan2(-tangent.x,tangent.z));
        return new Pose(center(current),yaw);
    }

    private static int normalizeLoopIndex(List<BlockPos> route, boolean loop, int loopStartIndex, int index) {
        if (!loop || index < route.size() - 1) return Math.min(index, route.size() - 1);
        return route.getLast().equals(route.get(loopStartIndex)) ? loopStartIndex : Math.min(index, route.size() - 1);
    }

    private static BlockPos routeNext(List<BlockPos> route, boolean loop, int loopStartIndex, int index) {
        if (index + 1 < route.size()) return route.get(index + 1);
        if (!loop || route.size() < 2) return null;
        return route.get(loopStartIndex);
    }

    private static double projectedProgress(Vec3 position, BlockPos current, BlockPos next) {
        Vec3 from = center(current);
        Vec3 segment = center(next).subtract(from);
        double lengthSquared = segment.lengthSqr();
        if (lengthSquared < 0.000001D) return 0;
        double ratio = Math.max(0, Math.min(1, position.subtract(from).dot(segment) / lengthSquared));
        return Math.sqrt(lengthSquared) * ratio;
    }

    Optional<Direction> travelDirection(boolean forward) {
        BlockPos target=forward?next:previous;
        if(target==null)return Optional.empty();
        return FormationDirection.fromPositions(target.getX(),target.getZ(),current.getX(),current.getZ());
    }

    BlockPos currentRail() { return current; }
    BlockPos nextRail() { return next == null ? current : next; }
    int routeIndex() { return routeIndex; }
    double segmentProgress() { return progress; }

    void snapTo(BlockPos stopPos) {
        if (stopPos == null) return;
        if (route.isEmpty()) {
            if (current.equals(stopPos)) progress = 0.0D;
            return;
        }
        int selected = -1;
        for (int index = Math.clamp(routeIndex, 0, route.size() - 1); index < route.size(); index++) {
            if (route.get(index).equals(stopPos)) {
                selected = index;
                break;
            }
        }
        if (selected < 0) {
            for (int index = 0; index < Math.clamp(routeIndex, 0, route.size()); index++) {
                if (route.get(index).equals(stopPos)) {
                    selected = index;
                    break;
                }
            }
        }
        if (selected < 0) return;
        routeIndex = selected;
        current = route.get(selected);
        previous = selected > 0 ? route.get(selected - 1) : null;
        next = routeNext(route, loop, loopStartIndex, selected);
        progress = 0.0D;
    }

    Optional<RailShape> routeShapeAt(BlockPos switchPos) {
        return routeShapeAt(route, loop, loopStartIndex, routeIndex, previous, switchPos);
    }

    static Optional<RailShape> routeShapeAt(List<BlockPos> route, boolean loop, int loopStartIndex,
                                            int routeIndex, BlockPos previous, BlockPos switchPos) {
        if (route.isEmpty() || switchPos == null) return Optional.empty();
        int start = Math.clamp(routeIndex, 0, route.size() - 1);
        for (int index = start; index < route.size(); index++) {
            if (!route.get(index).equals(switchPos)) continue;
            BlockPos before = index > 0 ? route.get(index - 1) : previous;
            BlockPos after;
            if (index + 1 < route.size()) {
                after = route.get(index + 1);
            } else if (loop && loopStartIndex + 1 < route.size()) {
                after = route.get(loopStartIndex + 1);
            } else {
                after = null;
            }
            Optional<Direction> incomingSide = horizontalSide(switchPos, before);
            Optional<Direction> outgoingSide = horizontalSide(switchPos, after);
            if (incomingSide.isPresent() && outgoingSide.isPresent()
                    && incomingSide.get() != outgoingSide.get()) {
                return Optional.of(cn.quit5700.fabrictrain.route.SwitchRailRouting.shapeBetween(
                        incomingSide.get(), outgoingSide.get()));
            }
        }
        return Optional.empty();
    }

    private static Optional<Direction> horizontalSide(BlockPos origin, BlockPos neighbour) {
        if (origin == null || neighbour == null) return Optional.empty();
        int dx = neighbour.getX() - origin.getX();
        int dz = neighbour.getZ() - origin.getZ();
        if (dx == 0 && dz == 0) return Optional.empty();
        if (Math.abs(dx) >= Math.abs(dz)) return Optional.of(dx >= 0 ? Direction.EAST : Direction.WEST);
        return Optional.of(dz >= 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private static BlockPos continuation(ServerLevel level, BlockPos at, BlockPos cameFrom) {
        var exits = exits(level, at);
        if (exits == null) return null;
        BlockPos first = railAt(level, at.offset(exits.getFirst()));
        BlockPos second = railAt(level, at.offset(exits.getSecond()));
        if (first != null && !first.equals(cameFrom)) return first;
        if (second != null && !second.equals(cameFrom)) return second;
        return null;
    }

    private static com.mojang.datafixers.util.Pair<net.minecraft.core.Vec3i, net.minecraft.core.Vec3i> exits(
            ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!state.hasProperty(RailBlock.SHAPE)) return null;
        return AbstractMinecart.exits(state.getValue(RailBlock.SHAPE));
    }

    private static BlockPos railAt(ServerLevel level, BlockPos pos) {
        if (BaseRailBlock.isRail(level, pos)) return pos;
        if (BaseRailBlock.isRail(level, pos.below())) return pos.below();
        if (BaseRailBlock.isRail(level, pos.above())) return pos.above();
        return null;
    }

    private static Vec3 center(BlockPos pos) {
        return Vec3.atBottomCenterOf(pos).add(0, 0.1D, 0);
    }

    record Pose(Vec3 position, float yaw) {}
}
