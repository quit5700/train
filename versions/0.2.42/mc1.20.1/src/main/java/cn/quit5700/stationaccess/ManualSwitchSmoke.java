package cn.quit5700.stationaccess;

import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.block.entity.PowerGeneratorBlockEntity;
import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.registry.ModEntities;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

final class ManualSwitchSmoke {
    private static final BlockPos SWITCH = new BlockPos(51, 81, 54);
    private static final BlockPos START = SWITCH.east(7);
    private static final BlockPos WIRE = START.south();
    private static final BlockPos GENERATOR = WIRE.south();
    private static int phase;
    private static int since;
    private static int stoppedTicks;
    private static double previousX;
    private static double stoppedX;
    private static LocomotiveEntity locomotive;

    static boolean tick(ServerLevel world, int tick, ServerPlayer rider, Predicate<Entity> board,
                        BiConsumer<Boolean, String> check) {
        if (phase == 0) {
            world.setChunkForced(3, 3, true);
            world.getChunkAt(SWITCH);
            for (int x = 49; x <= 62; x++) {
                BlockPos rail = new BlockPos(x, SWITCH.getY(), SWITCH.getZ());
                world.setBlock(rail.below(), Blocks.STONE.defaultBlockState(), 2);
                world.setBlock(rail, ModBlocks.SIGNAL_RAIL.defaultBlockState()
                        .setValue(RailBlock.SHAPE, RailShape.EAST_WEST), 2);
            }
            world.setBlock(SWITCH, ModBlocks.SWITCH_RAIL.defaultBlockState()
                    .setValue(DeviceRailBlock.ENTRY_DIRECTION, Direction.WEST)
                    .setValue(DeviceRailBlock.PLACEMENT_ENTRY, Direction.WEST)
                    .setValue(DeviceRailBlock.SWITCH_CHOICE, 1)
                    .setValue(RailBlock.SHAPE, RailShape.EAST_WEST), 2);
            var device = RailwayDataManager.register(world, SWITCH, DeviceType.SWITCH);
            RailwayDataManager.state(world).updateDevice(device.id(), "manual-switch-probe", 0, 1);
            world.setBlock(WIRE.below(), Blocks.STONE.defaultBlockState(), 2);
            world.setBlock(WIRE, Blocks.REDSTONE_WIRE.defaultBlockState(), 2);
            world.setBlock(GENERATOR.below(), Blocks.STONE.defaultBlockState(), 2);
            world.setBlock(GENERATOR, ModBlocks.POWER_GENERATOR.defaultBlockState(), 2);
            ((PowerGeneratorBlockEntity) world.getBlockEntity(GENERATOR)).setItem(0, new ItemStack(Items.REDSTONE_BLOCK));
            locomotive = new LocomotiveEntity(ModEntities.LOCOMOTIVE, world);
            locomotive.trainLink().setFormationForward(Direction.EAST);
            locomotive.trainLink().setParked(true);
            locomotive.setPos(START.getX() + 0.5, START.getY() + 0.0625, START.getZ() + 0.5);
            check.accept(world.addFreshEntity(locomotive), "manual_switch_test_spawns_real_locomotive");
            phase = 1;
            since = tick;
            return false;
        }
        if (phase == 1) {
            boolean ready = RailPowerService.isPowered(world, START) && world.getEntity(locomotive.getUUID()) == locomotive;
            if (!ready && tick - since < 100) return false;
            check.accept(ready, "manual_switch_test_rail_is_powered_and_locomotive_is_loaded");
            rider.setPos(locomotive.getX(), locomotive.getY(), locomotive.getZ());
            check.accept(board.test(locomotive), "manual_switch_test_player_boards");
            previousX = locomotive.getX();
            TrainControllerManager.drive(rider, locomotive, false, true); // S.
            phase = 2;
            since = tick;
            return false;
        }
        if (phase == 2) {
            double x = locomotive.getX();
            stoppedTicks = Math.abs(x - previousX) < 0.00001 ? stoppedTicks + 1 : 0;
            previousX = x;
            if (stoppedTicks < 12 && tick - since < 160) return false;
            check.accept(x < START.getX() - 1 && x > SWITCH.getX() + 1 && stoppedTicks >= 12,
                    "S_moves_backwards_then_stops_before_switch");
            stoppedX = x;
            TrainControllerManager.drive(rider, locomotive, true, false); // W directly after S.
            phase = 3;
            since = tick;
            return false;
        }
        if (phase == 3) {
            if (locomotive.getX() <= stoppedX + 1 && tick - since < 50) return false;
            check.accept(locomotive.getX() > stoppedX + 1 && RailPowerService.isPowered(world, START),
                    "W_moves_forward_after_S_is_blocked");
            TrainControllerManager.drive(rider, locomotive, false, true);
            phase = 4;
            previousX = locomotive.getX();
            stoppedTicks = 0;
            since = tick;
            return false;
        }
        if (phase == 4) {
            double x = locomotive.getX();
            stoppedTicks = Math.abs(x - previousX) < 0.00001 ? stoppedTicks + 1 : 0;
            previousX = x;
            if ((x > stoppedX + 0.5 || stoppedTicks < 12) && tick - since < 150) return false;
            check.accept(x > SWITCH.getX() + 1 && x <= stoppedX + 0.5 && stoppedTicks >= 12,
                    "S_still_stops_after_leaving_the_switch");
            rider.stopRiding();
            locomotive.discard();
            world.setChunkForced(3, 3, false);
            phase = 5;
        }
        return true;
    }
}
