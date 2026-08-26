package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class RailwayManagerNetworking {
    private RailwayManagerNetworking() {}
    public static final int CREATE=0, ADD=1, MOVE=2, REMOVE=3, DIRECTION=4, RENAME=5, DELETE=6;

    public static void registerServer() {
        LegacyPayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(SelectPayload.ID, SelectPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(EditPayload.ID, EditPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(ClosePayload.ID, ClosePayload.CODEC);
        LegacyServerPlayNetworking.registerGlobalReceiver(SelectPayload.ID, (payload, context) -> context.server().execute(() -> select(context.player(), payload)));
        LegacyServerPlayNetworking.registerGlobalReceiver(EditPayload.ID, (payload, context) -> context.server().execute(() -> edit(context.player(), payload)));
        LegacyServerPlayNetworking.registerGlobalReceiver(ClosePayload.ID, (payload, context) ->
                context.server().execute(() -> DepotRemoteAccessSessions.revokeAll(context.player().getUUID())));
    }

    private static void select(ServerPlayer player, SelectPayload payload) {
        if (!DepotRemoteAccessSessions.hasManagerAccess(player.getUUID(), System.nanoTime())) {
            cn.quit5700.fabrictrain.network.OperationChat.suppress(net.minecraft.network.chat.Component.translatable(
                    "message.fabric_train_simulator.depot.remote_access_expired"));
            return;
        }
        RailwaySavedData data=RailwayDataManager.state(player.serverLevel());
        if(payload.device()) {
            RailwayDevice device=data.device(payload.id()); if(device==null)return;
            if(device.type()==DeviceType.DEPOT) {
                for(ServerLevel level:player.serverLevel().getServer().getAllLevels()) {
                    if(!RailwayDataManager.dimension(level).equals(device.dimension()))continue;
                    BlockPos pos=new BlockPos(device.x(),device.y(),device.z());
                    if(level.getBlockEntity(pos) instanceof TrainDepotBlockEntity depot) {
                        DepotRemoteAccessSessions.authorize(player.getUUID(), device.dimension(), pos, System.nanoTime());
                        DepotNetworking.open(player,pos,depot);
                    }
                }
            } else DeviceSettingsNetworking.open(player,device);
        } else {cn.quit5700.fabrictrain.item.LineSetterItem.select(player.getUUID(),payload.id());open(player, payload.id());}
    }

    private static void edit(ServerPlayer player, EditPayload payload) {
        if (!DepotRemoteAccessSessions.hasManagerAccess(player.getUUID(), System.nanoTime())) {
            cn.quit5700.fabrictrain.network.OperationChat.suppress(net.minecraft.network.chat.Component.translatable(
                    "message.fabric_train_simulator.depot.remote_access_expired"));
            return;
        }
        RailwaySavedData data=RailwayDataManager.state(player.serverLevel());
        try {
            switch(payload.operation()) {
                case CREATE -> { RailwayLine created=data.createEmptyLine(payload.text()); open(player,created.id()); return; }
                case ADD -> data.insertLineNode(payload.lineId(),payload.index(),payload.deviceId());
                case MOVE -> { int from=payload.index(),to=payload.value(); while(from>to){data.modifyLineNode(payload.lineId(),from,0);from--;}while(from<to){data.modifyLineNode(payload.lineId(),from,1);from++;} }
                case REMOVE -> data.modifyLineNode(payload.lineId(),payload.index(),2);
                case DIRECTION -> data.setSwitchDirection(payload.lineId(),payload.index(),payload.value());
                case RENAME -> { RailwayLine line=data.line(payload.lineId()); if(line!=null){data.updateLineSettings(line.id(),payload.text(),line.departureIntervalSeconds(),line.speedLimitKmh(),line.maximumTrains(),LineStatus.NEEDS_VALIDATION);open(player,-1);return;} }
                case DELETE -> { LineDeletionService.delete(player.serverLevel().getServer(), (ServerLevel) player.serverLevel(), payload.lineId()); open(player,0); return; }
                default -> { return; }
            }
        } catch (IllegalArgumentException ignored) { }
        open(player,payload.lineId());
    }

    public static void open(ServerPlayer player) { open(player,0); }
    public static void open(ServerPlayer player,long selectedLineId) {
        DeviceRecoveryService.recoverPending(player.serverLevel(), 8);
        RailwaySavedData data=RailwayDataManager.state(player.serverLevel());
        List<DeviceSummary> devices=data.devices().values().stream().sorted(java.util.Comparator.comparingLong(RailwayDevice::id)).map(d->new DeviceSummary(d.id(),d.type().ordinal(),d.name())).toList();
        List<LineSummary> lines=data.lines().values().stream().sorted(java.util.Comparator.comparingLong(RailwayLine::id)).map(l->{
            List<Integer> directions=new ArrayList<>(l.nodeIds().size()); for(int i=0;i<l.nodeIds().size();i++) directions.add(l.switchDirection(i));
            List<StopSummary> stops=new ArrayList<>(l.nodeIds().size());
            for(int i=0;i<l.nodeIds().size();i++){StopTrack stop=l.stationStopTrack(i);stops.add(stop==null
                    ?new StopSummary(false,0,0,0):new StopSummary(true,stop.x(),stop.y(),stop.z()));}
            return new LineSummary(l.id(),l.name(),l.status().ordinal(),data.activeTrainCount(l.id()),l.maximumTrains(),l.nodeIds(),directions,stops);
        }).toList();
        LegacyServerPlayNetworking.send(player,new OpenPayload(devices,lines,selectedLineId));
    }
    public record DeviceSummary(long id,int type,String name) {}
    public record StopSummary(boolean bound,int x,int y,int z) {}
    public record LineSummary(long id,String name,int status,long active,int maximum,List<Long> nodes,List<Integer> directions,List<StopSummary> stops) {}
    public record OpenPayload(List<DeviceSummary> devices,List<LineSummary> lines,long selectedLineId) implements LegacyPayload {
        public static final Type<OpenPayload> ID=new Type<>(FabricTrainMod.id("open_railway_manager"));
        public static final LegacyPayloadCodec<FriendlyByteBuf,OpenPayload> CODEC=LegacyPayload.codec(OpenPayload::write,OpenPayload::read);
        private void write(FriendlyByteBuf b){b.writeVarInt(devices.size());for(DeviceSummary d:devices){b.writeVarLong(d.id());b.writeVarInt(d.type());b.writeUtf(d.name(),64);}b.writeVarInt(lines.size());for(LineSummary l:lines){b.writeVarLong(l.id());b.writeUtf(l.name(),64);b.writeVarInt(l.status());b.writeVarLong(l.active());b.writeVarInt(l.maximum());b.writeVarInt(l.nodes().size());for(int i=0;i<l.nodes().size();i++){b.writeVarLong(l.nodes().get(i));b.writeVarInt(l.directions().get(i));StopSummary s=l.stops().get(i);b.writeBoolean(s.bound());if(s.bound()){b.writeInt(s.x());b.writeInt(s.y());b.writeInt(s.z());}}}b.writeVarLong(selectedLineId);}
        private static OpenPayload read(FriendlyByteBuf b){int dc=cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(),0,10000);ArrayList<DeviceSummary> ds=new ArrayList<>(dc);for(int i=0;i<dc;i++)ds.add(new DeviceSummary(b.readVarLong(),b.readVarInt(),b.readUtf(64)));int lc=cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(),0,10000);ArrayList<LineSummary> ls=new ArrayList<>(lc);for(int i=0;i<lc;i++){long id=b.readVarLong();String name=b.readUtf(64);int status=b.readVarInt();long active=b.readVarLong();int maximum=b.readVarInt();int nc=cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(),0,1000);ArrayList<Long> nodes=new ArrayList<>(nc);ArrayList<Integer> directions=new ArrayList<>(nc);ArrayList<StopSummary> stops=new ArrayList<>(nc);for(int j=0;j<nc;j++){nodes.add(b.readVarLong());directions.add(b.readVarInt());boolean bound=b.readBoolean();stops.add(bound?new StopSummary(true,b.readInt(),b.readInt(),b.readInt()):new StopSummary(false,0,0,0));}ls.add(new LineSummary(id,name,status,active,maximum,List.copyOf(nodes),List.copyOf(directions),List.copyOf(stops)));}return new OpenPayload(List.copyOf(ds),List.copyOf(ls),b.readVarLong());}
        @Override public Type<? extends LegacyPayload> type(){return ID;}
    }
    public record SelectPayload(boolean device,long id) implements LegacyPayload {
        public static final Type<SelectPayload> ID=new Type<>(FabricTrainMod.id("select_railway_manager"));
        public static final LegacyPayloadCodec<FriendlyByteBuf,SelectPayload> CODEC=LegacyPayload.codec((p,b)->{b.writeBoolean(p.device);b.writeVarLong(p.id);},b->new SelectPayload(b.readBoolean(),b.readVarLong()));
        @Override public Type<? extends LegacyPayload> type(){return ID;}
    }
    public record EditPayload(int operation,long lineId,long deviceId,int index,int value,String text) implements LegacyPayload {
        public static final Type<EditPayload> ID=new Type<>(FabricTrainMod.id("edit_railway_manager"));
        public static final LegacyPayloadCodec<FriendlyByteBuf,EditPayload> CODEC=LegacyPayload.codec((p,b)->{b.writeVarInt(p.operation);b.writeVarLong(p.lineId);b.writeVarLong(p.deviceId);b.writeVarInt(p.index);b.writeVarInt(p.value);b.writeUtf(p.text,64);},b->new EditPayload(b.readVarInt(),b.readVarLong(),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readUtf(64)));
        @Override public Type<? extends LegacyPayload> type(){return ID;}
    }
    public record ClosePayload() implements LegacyPayload {
        public static final Type<ClosePayload> ID = new Type<>(FabricTrainMod.id("close_railway_manager"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, ClosePayload> CODEC =
                LegacyPayload.codec((payload, buffer) -> {}, buffer -> new ClosePayload());
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}



