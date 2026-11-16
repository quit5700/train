package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;

import java.util.List;
import cn.quit5700.fabrictrain.cargo.StationSelectionService;
import cn.quit5700.fabrictrain.cargo.StationPresenceService;
import cn.quit5700.fabrictrain.cargo.CargoTransferService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class CargoSettingsNetworking {
    private CargoSettingsNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(RefreshPayload.ID,RefreshPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RefreshPayload.ID,(payload,context)->context.server().execute(()->{
            var p=context.player();var level=(net.minecraft.server.level.ServerLevel)p.level();
            var e=level.getEntity(payload.entityId());if(e==null||p.distanceToSqr(e)>64.0D)return;
            var data=RailwayDataManager.state(level);
            if(payload.locomotive()&&e instanceof cn.quit5700.fabrictrain.entity.LocomotiveEntity head){
                var options=StationSelectionService.availableStations(level,data,head.trainLink()).stream().map(d->new TrainClassNetworking.Station(d.id(),d.name())).toList();
                boolean cargo=false;for(var car:level.getAllEntities())if(car instanceof CargoCarEntity c&&c.trainLink().trainId().equals(head.trainLink().trainId())){cargo=true;break;}
                ServerPlayNetworking.send(p,new TrainClassNetworking.OpenPayload(head.getId(),head.trainLink().trainClass().ordinal(),cargo,data.classSpeedLimit(cn.quit5700.fabrictrain.train.TrainClass.CARGO),data.classSpeedLimit(cn.quit5700.fabrictrain.train.TrainClass.PASSENGER),data.classSpeedLimit(cn.quit5700.fabrictrain.train.TrainClass.EXPRESS),head.trainLink().destinationStationId(),options,true));
            }else if(!payload.locomotive()&&e instanceof CargoCarEntity cargo){
                var options=StationSelectionService.availableStations(level,data,cargo.trainLink()).stream().map(d->new Station(d.id(),d.name(),0)).toList();
                ServerPlayNetworking.send(p,new OpenPayload(cargo.getId(),cargo.destinationStationId(),options,true));
            }
        }));

        PayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UnloadAllPayload.ID, UnloadAllPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) -> context.server().execute(() -> {
            Entity entity = context.player().serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof CargoCarEntity cargo) || context.player().distanceToSqr(entity) > 64.0D) return;
            var data=RailwayDataManager.state(context.player().serverLevel());
            if(StationSelectionService.canSelect((net.minecraft.server.level.ServerLevel) context.player().level(),data,cargo.trainLink(),payload.stationId()))
                cargo.setDestinationStationId(payload.stationId());
        }));
        ServerPlayNetworking.registerGlobalReceiver(UnloadAllPayload.ID, (payload, context) -> context.server().execute(() -> {
            Entity entity = context.player().serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof CargoCarEntity cargo) || context.player().distanceToSqr(entity) > 64.0D) return;
            var level = (net.minecraft.server.level.ServerLevel) context.player().serverLevel();
            var data = RailwayDataManager.state(level);
            RailwayDevice station = StationPresenceService.findStationAt(level, data, cargo);
            if (station == null) {
                cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable("message.fabric_train_simulator.cargo.not_at_station"));
                return;
            }
            BlockPos stationPos = new BlockPos(station.x(), station.y(), station.z());
            if (!CargoTransferService.hasAdjacentContainer(level, stationPos)) {
                cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable("message.fabric_train_simulator.cargo.no_container", station.name()));
                return;
            }
            int moved = CargoTransferService.unloadAtStation(level, stationPos, cargo);
            cn.quit5700.fabrictrain.network.OperationChat.suppress(Component.translatable(moved > 0
                    ? "message.fabric_train_simulator.cargo.unloaded_all"
                    : "message.fabric_train_simulator.cargo.nothing_unloaded", moved, station.name()));
        }));
    }

    public record Station(long id, String name, int etaSeconds) {}

    public record OpenPayload(int entityId, long selectedStationId, List<Station> stations,boolean refresh) implements CustomPacketPayload {
        public OpenPayload(int entityId,long selectedStationId,List<Station> stations){this(entityId,selectedStationId,stations,false);}
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_cargo_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = CustomPacketPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(RegistryFriendlyByteBuf b) {
            b.writeVarInt(entityId); b.writeVarLong(selectedStationId); b.writeVarInt(stations.size());
            for (Station station : stations) { b.writeVarLong(station.id()); b.writeUtf(station.name(), 128); b.writeVarInt(station.etaSeconds()); } b.writeBoolean(refresh);
        }
        private static OpenPayload read(RegistryFriendlyByteBuf b) {
            int entityId = b.readVarInt(); long selected = b.readVarLong(); int size = Math.clamp(b.readVarInt(), 0, 1000);
            java.util.ArrayList<Station> stations = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) stations.add(new Station(b.readVarLong(), b.readUtf(128), b.readVarInt()));
            return new OpenPayload(entityId, selected, List.copyOf(stations), b.readBoolean());
        }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record UpdatePayload(int entityId, long stationId) implements CustomPacketPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_cargo_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePayload> CODEC = CustomPacketPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(RegistryFriendlyByteBuf b) { b.writeVarInt(entityId); b.writeVarLong(stationId); }
        private static UpdatePayload read(RegistryFriendlyByteBuf b) { return new UpdatePayload(b.readVarInt(), b.readVarLong()); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record UnloadAllPayload(int entityId) implements CustomPacketPayload {
        public static final Type<UnloadAllPayload> ID = new Type<>(FabricTrainMod.id("unload_all_cargo"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UnloadAllPayload> CODEC =
                CustomPacketPayload.codec((payload, buffer) -> buffer.writeVarInt(payload.entityId),
                        buffer -> new UnloadAllPayload(buffer.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record RefreshPayload(int entityId,boolean locomotive) implements CustomPacketPayload {
        public static final Type<RefreshPayload> ID=new Type<>(FabricTrainMod.id("refresh_unload_stations"));
        public static final StreamCodec<RegistryFriendlyByteBuf,RefreshPayload> CODEC=CustomPacketPayload.codec((p,b)->{b.writeVarInt(p.entityId);b.writeBoolean(p.locomotive);},b->new RefreshPayload(b.readVarInt(),b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return ID;}
    }
}
