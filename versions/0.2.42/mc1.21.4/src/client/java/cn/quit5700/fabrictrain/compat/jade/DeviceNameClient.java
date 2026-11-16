package cn.quit5700.fabrictrain.compat.jade;
import java.util.*;
import net.minecraft.core.BlockPos;
import cn.quit5700.fabrictrain.network.DeviceNamePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
public final class DeviceNameClient {
 private static final Map<BlockPos,String> names=new HashMap<>();private static long last;private static BlockPos target;
 public static void register(){
  ClientPlayNetworking.registerGlobalReceiver(DeviceNamePackets.Response.ID,(p,c)->c.client().execute(()->names.put(p.pos(),p.name())));
  ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{names.clear();target=null;last=0;});
 }
 public static String name(BlockPos pos){long now=System.nanoTime();if(!pos.equals(target)||now-last>250000000L){last=now;target=pos.immutable();ClientPlayNetworking.send(new DeviceNamePackets.Request(pos));}return names.getOrDefault(pos,"");}
}
