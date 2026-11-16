package cn.quit5700.fabrictrain.network;
import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
public final class DeviceNamePackets {
 public record Request(BlockPos pos) implements CustomPacketPayload{
  public static final Type<Request> ID=new Type<>(FabricTrainMod.id("device_name_request"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=CustomPacketPayload.codec((p,b)->b.writeBlockPos(p.pos),b->new Request(b.readBlockPos()));
  public Type<? extends CustomPacketPayload> type(){return ID;}
 }
 public record Response(BlockPos pos,String name) implements CustomPacketPayload{
  public static final Type<Response> ID=new Type<>(FabricTrainMod.id("device_name_response"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=CustomPacketPayload.codec((p,b)->{b.writeBlockPos(p.pos);b.writeUtf(p.name,64);},b->new Response(b.readBlockPos(),b.readUtf(64)));
  public Type<? extends CustomPacketPayload> type(){return ID;}
 }
 public static void register(){
  PayloadTypeRegistry.playS2C().register(Response.ID,Response.CODEC);PayloadTypeRegistry.playC2S().register(Request.ID,Request.CODEC);
  ServerPlayNetworking.registerGlobalReceiver(Request.ID,(p,c)->c.server().execute(()->{
   var world=c.player().level();if(!world.hasChunkAt(p.pos)||c.player().distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>1024)return;
   var d=RailwayDataManager.state(world).find(RailwayDataManager.dimension(world),p.pos.getX(),p.pos.getY(),p.pos.getZ());
   ServerPlayNetworking.send(c.player(),new Response(p.pos,d==null?"":d.name()));
  }));
 }
}
