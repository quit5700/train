package cn.quit5700.fabrictrain.redstone;

import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;
import cn.quit5700.fabrictrain.route.SwitchRailRouting;

public final class DeviceRedstoneService {
    private static final Map<Long,SignalKind> LAST=new HashMap<>();
    private DeviceRedstoneService(){}
    public static void tick(MinecraftServer server){
        RailwaySavedData data=RailwayDataManager.state(server.overworld());
        for(ServerLevel level:server.getAllLevels())for(RailwayDevice device:data.devices().values()){
            if(!device.dimension().equals(RailwayDataManager.dimension(level)))continue;
            SignalKind now=signal(level,new BlockPos(device.x(),device.y(),device.z()));SignalKind before=LAST.getOrDefault(device.id(),SignalKind.NONE);LAST.put(device.id(),now);
            if(device.type()==DeviceType.STATION){
                if(now==SignalKind.CONTINUOUS)TrainControllerManager.emergencyStopAt(device.id());
                else if(before==SignalKind.CONTINUOUS)TrainControllerManager.releaseEmergencyAt(device.id());
                else if(now==SignalKind.PULSE&&before!=SignalKind.PULSE)TrainControllerManager.departStation(server,device.id());
            }else if(device.type()==DeviceType.SWITCH&&now==SignalKind.PULSE&&before!=SignalKind.PULSE){
                int direction=(device.switchShapeOrdinal()+1)%3;
                data.updateDevice(device.id(),device.name(),device.dwellSeconds(),direction,device.announcement());
                SwitchRailRouting.applyChoice(level,device,direction);
                Component message=Component.translatable("screen.fabric_train_simulator.switch."+(direction==0?"left":direction==1?"straight":"right"));
                level.players().stream().filter(p->p.distanceToSqr(device.x()+.5,device.y()+.5,device.z()+.5)<=1024).forEach(p->p.sendOverlayMessage(message));
            }
        }
    }
    public static void clear(){LAST.clear();}
    static SignalKind signal(ServerLevel level,BlockPos device){
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();HashSet<BlockPos> seen=new HashSet<>();
        for(Direction d:Direction.values())queue.add(device.relative(d));
        int budget=128;
        while(!queue.isEmpty()&&budget-->0){BlockPos pos=queue.removeFirst();if(!seen.add(pos))continue;BlockState state=level.getBlockState(pos);Block block=state.getBlock();
            if(block==Blocks.REDSTONE_WIRE){for(Direction d:Direction.values())queue.add(pos.relative(d));continue;}
            if(!powered(state,level,pos))continue;
            if(block instanceof ButtonBlock||block instanceof PressurePlateBlock)return SignalKind.PULSE;
            if(block instanceof LeverBlock||block==Blocks.REDSTONE_BLOCK||block==Blocks.REDSTONE_TORCH||block==Blocks.REDSTONE_WALL_TORCH)return SignalKind.CONTINUOUS;
        }
        return SignalKind.NONE;
    }
    private static boolean powered(BlockState state,ServerLevel level,BlockPos pos){
        if(state.hasProperty(BlockStateProperties.POWERED)&&state.getValue(BlockStateProperties.POWERED))return true;
        if(state.getBlock()==Blocks.REDSTONE_BLOCK)return true;
        for(Direction direction:Direction.values())if(state.getSignal(level,pos,direction)>0)return true;return false;
    }
    enum SignalKind{NONE,PULSE,CONTINUOUS}
}
