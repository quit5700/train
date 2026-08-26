package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class RailFormationPlacer {
    private static final double SPACING = FormationSpacing.BLOCKS;
    private RailFormationPlacer() {}

    public static boolean place(ServerLevel level, List<Entity> ordered, Entity firstSelected, Entity secondSelected) {
        if (ordered.isEmpty() || !(ordered.getFirst() instanceof AbstractMinecart leader)
                || !(firstSelected instanceof AbstractMinecart firstCart)
                || !(secondSelected instanceof AbstractMinecart secondCart)) return false;
        BlockPos firstRail = firstCart.getCurrentBlockPosOrRailBelow();
        BlockPos secondRail = secondCart.getCurrentBlockPosOrRailBelow();
        if (!sameRailNetwork(level, firstRail, secondRail, 128)) return false;
        BlockPos start = leader.getCurrentBlockPosOrRailBelow();
        List<Vec3> path = backwardPath(level, leader, start, ordered.size() * SPACING + 2.0D);
        if (path.isEmpty()) return false;
        ArrayList<Vec3> targets = new ArrayList<>();
        targets.add(leader.position());
        for (int index = 1; index < ordered.size(); index++) {
            Vec3 target = sample(path, index * SPACING);
            if (target == null) return false;
            targets.add(target);
        }
        for (int index = 0; index < ordered.size(); index++) {
            Entity entity = ordered.get(index);
            if (entity instanceof PassengerCarEntity passenger) passenger.releasePlacementBrake();
            if (entity instanceof CargoCarEntity cargo) cargo.releasePlacementBrake();
            entity.setDeltaMovement(Vec3.ZERO);
            if (index > 0) entity.setPos(targets.get(index));
            entity.needsSync = true;
            entity.syncPacketPositionCodec(entity.getX(), entity.getY(), entity.getZ());
        }
        return true;
    }

    static Vec3 sample(List<Vec3> path, double distance) {
        if (path.isEmpty()) return null;
        double walked = 0;
        for (int i = 1; i < path.size(); i++) {
            Vec3 from = path.get(i - 1);
            Vec3 to = path.get(i);
            double segment = from.distanceTo(to);
            if (segment > 0 && walked + segment >= distance) {
                return from.lerp(to, (distance - walked) / segment);
            }
            walked += segment;
        }
        return null;
    }

    private static List<Vec3> backwardPath(ServerLevel level, AbstractMinecart leader, BlockPos start, double needed) {
        if (!BaseRailBlock.isRail(level, start)) return List.of();
        ArrayList<Vec3> points = new ArrayList<>();
        points.add(leader.position());
        Vec3 facing = facing(leader);
        BlockPos previous = null;
        BlockPos current = start;
        Set<BlockPos> visited = new HashSet<>();
        double length = 0;
        for (int step = 0; step < 256 && length < needed; step++) {
            if (!visited.add(current)) break;
            var state = level.getBlockState(current);
            if (!state.hasProperty(RailBlock.SHAPE)) break;
            var exits = AbstractMinecart.exits(state.getValue(RailBlock.SHAPE));
            BlockPos first = railAt(level, current.offset(exits.getFirst()));
            BlockPos second = railAt(level, current.offset(exits.getSecond()));
            BlockPos next;
            if (previous == null) {
                double firstDot = dot(exits.getFirst(), facing);
                double secondDot = dot(exits.getSecond(), facing);
                next = firstDot <= secondDot ? first : second;
            } else if (first != null && !first.equals(previous)) next = first;
            else if (second != null && !second.equals(previous)) next = second;
            else break;
            if (next == null) break;
            Vec3 point = Vec3.atBottomCenterOf(next).add(0, 0.1D, 0);
            length += points.getLast().distanceTo(point);
            points.add(point);
            previous = current;
            current = next;
        }
        return points;
    }

    private static boolean sameRailNetwork(ServerLevel level, BlockPos start, BlockPos target, int budget) {
        start = railAt(level, start);
        target = railAt(level, target);
        if (start == null || target == null) return false;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        HashSet<BlockPos> seen = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty() && budget-- > 0) {
            BlockPos pos = queue.removeFirst();
            if (!seen.add(pos)) continue;
            if (pos.equals(target)) return true;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) != 1 || Math.abs(dy) > 1) continue;
                BlockPos candidate = pos.offset(dx, dy, dz);
                if (BaseRailBlock.isRail(level, candidate) && !seen.contains(candidate)) queue.add(candidate);
            }
        }
        return false;
    }

    private static BlockPos railAt(ServerLevel level, BlockPos pos) {
        if (BaseRailBlock.isRail(level, pos)) return pos;
        if (BaseRailBlock.isRail(level, pos.below())) return pos.below();
        if (BaseRailBlock.isRail(level, pos.above())) return pos.above();
        return null;
    }

    private static Vec3 facing(Entity entity) {
        double radians = Math.toRadians(entity.getYRot());
        return new Vec3(-Math.sin(radians), 0, Math.cos(radians));
    }

    private static double dot(Vec3i step, Vec3 facing) {
        return step.getX() * facing.x + step.getZ() * facing.z;
    }
}
