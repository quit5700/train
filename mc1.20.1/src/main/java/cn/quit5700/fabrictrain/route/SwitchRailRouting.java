package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;

import java.util.Optional;

public final class SwitchRailRouting {
    private SwitchRailRouting() {}

    public static RailShape shapeFor(Direction travel, int choice) {
        if (travel.getAxis() == Direction.Axis.Y) return RailShape.NORTH_SOUTH;
        Direction entry = travel.getOpposite();
        Direction exit = switch (cn.quit5700.fabrictrain.compat.MathCompat.clamp(choice, 0, 2)) {
            case 0 -> travel.getCounterClockWise();
            case 2 -> travel.getClockWise();
            default -> travel;
        };
        return shapeBetween(entry, exit);
    }

    public static Direction exitFor(Direction entry, int choice) {
        if (entry.getAxis() == Direction.Axis.Y) entry = Direction.NORTH;
        Direction travel = entry.getOpposite();
        return switch (cn.quit5700.fabrictrain.compat.MathCompat.clamp(choice, 0, 2)) {
            case 0 -> travel.getCounterClockWise();
            case 2 -> travel.getClockWise();
            default -> travel;
        };
    }

    public static RailShape shapeForEntry(Direction entry, int choice) {
        return shapeBetween(entry, exitFor(entry, choice));
    }

    public static Optional<RailShape> shapeForFixedChoice(Direction fixedEntry, Direction travel, int choice) {
        if (travel.getAxis() == Direction.Axis.Y) return Optional.empty();
        Direction incomingSide = travel.getOpposite();
        Direction fixedExit = exitFor(fixedEntry, choice);
        if (incomingSide == fixedExit) return Optional.empty();
        return Optional.of(shapeBetween(incomingSide, fixedExit));
    }

    public static Direction entryFor(RailwayDevice device, BlockState state) {
        Direction placement = state.hasProperty(DeviceRailBlock.PLACEMENT_ENTRY)
                ? state.getValue(DeviceRailBlock.PLACEMENT_ENTRY)
                : state.getValue(DeviceRailBlock.ENTRY_DIRECTION);
        return SwitchEntryDirectionRules.resolve(device.switchEntryOrdinal(), placement);
    }

    public static RailShape shapeBetween(Direction first, Direction second) {
        if ((first == Direction.NORTH && second == Direction.SOUTH)
                || (first == Direction.SOUTH && second == Direction.NORTH)) return RailShape.NORTH_SOUTH;
        if ((first == Direction.EAST && second == Direction.WEST)
                || (first == Direction.WEST && second == Direction.EAST)) return RailShape.EAST_WEST;
        if (contains(first, second, Direction.NORTH, Direction.EAST)) return RailShape.NORTH_EAST;
        if (contains(first, second, Direction.NORTH, Direction.WEST)) return RailShape.NORTH_WEST;
        if (contains(first, second, Direction.SOUTH, Direction.EAST)) return RailShape.SOUTH_EAST;
        return RailShape.SOUTH_WEST;
    }

    private static boolean contains(Direction first, Direction second, Direction expectedFirst, Direction expectedSecond) {
        return (first == expectedFirst && second == expectedSecond)
                || (first == expectedSecond && second == expectedFirst);
    }

    public static void applyChoice(ServerLevel level, RailwayDevice before, int newChoice) {
        BlockPos pos = new BlockPos(before.x(), before.y(), before.z());
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.SWITCH_RAIL) || !state.hasProperty(RailBlock.SHAPE)) return;
        int choice = cn.quit5700.fabrictrain.compat.MathCompat.clamp(newChoice, 0, 2);
        Direction entry = entryFor(before, state);
        level.setBlock(pos, state.setValue(DeviceRailBlock.SWITCH_CHOICE, choice)
                .setValue(DeviceRailBlock.ENTRY_DIRECTION, entry)
                .setValue(RailBlock.SHAPE, shapeForEntry(entry, choice)), 3);
    }

    public static void applySettings(ServerLevel level, RailwayDevice device, int newChoice, Direction entry) {
        BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.SWITCH_RAIL) || !state.hasProperty(RailBlock.SHAPE)) return;
        int choice = cn.quit5700.fabrictrain.compat.MathCompat.clamp(newChoice, 0, 2);
        level.setBlock(pos, state.setValue(DeviceRailBlock.SWITCH_CHOICE, choice)
                .setValue(DeviceRailBlock.ENTRY_DIRECTION, entry)
                .setValue(RailBlock.SHAPE, shapeForEntry(entry, choice)), 3);
    }
}

