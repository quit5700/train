package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.data.RailwayLine;
import cn.quit5700.fabrictrain.data.RailwaySavedData;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Reconciles legacy switch records before any route calculation uses them. */
public final class SwitchEntryStateSynchronizer {
    private SwitchEntryStateSynchronizer() { }

    public static RailwayDevice synchronizeDevice(ServerLevel level, RailwaySavedData data, RailwayDevice device) {
        if (device == null || device.type() != DeviceType.SWITCH
                || !device.dimension().equals(RailwayDataManager.dimension(level))) return device;
        BlockState state = level.getBlockState(new BlockPos(device.x(), device.y(), device.z()));
        if (!state.is(ModBlocks.SWITCH_RAIL) || !state.hasProperty(DeviceRailBlock.PLACEMENT_ENTRY)) return device;
        Direction entry = SwitchRailRouting.entryFor(device, state);
        return device.switchEntryOrdinal() == entry.ordinal()
                ? device : data.updateSwitchEntry(device.id(), entry.ordinal());
    }

    public static void synchronizeLine(ServerLevel level, RailwaySavedData data, RailwayLine line) {
        if (line == null) return;
        for (long deviceId : line.nodeIds()) synchronizeDevice(level, data, data.device(deviceId));
    }
}
