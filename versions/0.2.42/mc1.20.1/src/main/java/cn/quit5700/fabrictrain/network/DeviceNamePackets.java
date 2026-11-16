package cn.quit5700.fabrictrain.network;
import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
public final class DeviceNamePackets {
 public record Request(BlockPos pos) implements LegacyPayload{
  public static final Type<Request> ID=new Type<>(FabricTrainMod.id("device_name_request"));
  public static final LegacyPayloadCodec<FriendlyByteBuf,Request> CODEC=LegacyPayload.codec((p,b)->b.writeBlockPos(p.pos),b->new Request(b.readBlockPos()));
  public Type<? extends LegacyPayload> type(){return ID;}
 }
 public record Response(BlockPos pos,String name) implements LegacyPayload{
  public static final Type<Response> ID=new Type<>(FabricTrainMod.id("device_name_response"));
  public static final LegacyPayloadCodec<FriendlyByteBuf,Response> CODEC=LegacyPayload.codec((p,b)->{b.writeBlockPos(p.pos);b.writeUtf(p.name,64);},b->new Response(b.readBlockPos(),b.readUtf(64)));
  public Type<? extends LegacyPayload> type(){return ID;}
 }
 public static void register(){
  LegacyPayloadTypeRegistry.playS2C().register(Response.ID,Response.CODEC);LegacyPayloadTypeRegistry.playC2S().register(Request.ID,Request.CODEC);
  LegacyServerPlayNetworking.registerGlobalReceiver(Request.ID,(p,c)->c.server().execute(()->{
   var world=c.player().serverLevel();if(!world.hasChunkAt(p.pos)||c.player().distanceToSqr(p.pos.getX()+.5,p.pos.getY()+.5,p.pos.getZ()+.5)>1024)return;
   var d=RailwayDataManager.state(world).find(RailwayDataManager.dimension(world),p.pos.getX(),p.pos.getY(),p.pos.getZ());
   LegacyServerPlayNetworking.send(c.player(),new Response(p.pos,d==null?"":d.name()));
  }));
 }
}
