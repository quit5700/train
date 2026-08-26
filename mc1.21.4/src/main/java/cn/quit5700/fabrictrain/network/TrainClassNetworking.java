package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.TrainClass;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import java.util.List;
import cn.quit5700.fabrictrain.cargo.StationSelectionService;

public final class TrainClassNetworking {
    private TrainClassNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) -> context.server().execute(() -> {
            Entity entity = context.player().serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof LocomotiveEntity locomotive) || context.player().distanceToSqr(entity) > 64.0D) return;
            boolean hasCargo = hasCargo(context.player().serverLevel().getAllEntities(), locomotive);
            TrainClass requested = TrainClass.values()[Math.clamp(payload.trainClassOrdinal(), 0, TrainClass.values().length - 1)];
            TrainClass selected = hasCargo ? TrainClass.CARGO : requested;
            long destination=payload.destinationStationId();
            var data=cn.quit5700.fabrictrain.data.RailwayDataManager.state(context.player().serverLevel());
            if(!StationSelectionService.canSelect(data,locomotive.trainLink(),destination))destination=0;
            boolean destinationChanged = locomotive.trainLink().destinationStationId() != destination;
            locomotive.trainLink().setDestinationStationId(destination);
            data.updateClassSpeedLimits(
                    payload.freightSpeedKmh(), payload.passengerSpeedKmh(), payload.expressSpeedKmh());
            for (Entity candidate : context.player().serverLevel().getAllEntities()) {
                if (candidate instanceof LinkedTrainCar linked
                        && linked.trainLink().trainId().equals(locomotive.trainLink().trainId())) {
                    linked.trainLink().setTrainClass(selected);
                    linked.trainLink().setDestinationStationId(destination);
                    linked.trainLink().setConfiguredSpeedLimitKmh(data.classSpeedLimit(selected));
                    if(destinationChanged && linked instanceof CargoCarEntity cargo) cargo.setDestinationStationId(0L);
                }
            }
        }));
    }

    private static boolean hasCargo(Iterable<Entity> entities, LocomotiveEntity locomotive) {
        for (Entity candidate : entities) {
            if (candidate instanceof CargoCarEntity cargo
                    && cargo.trainLink().trainId().equals(locomotive.trainLink().trainId())) return true;
        }
        return false;
    }

    public record Station(long id,String name){}
    public record OpenPayload(int entityId, int trainClassOrdinal, boolean hasCargo, double freightSpeedKmh,
                              double passengerSpeedKmh, double expressSpeedKmh,long destinationStationId,List<Station> stations) implements CustomPacketPayload {
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_train_class"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = CustomPacketPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(RegistryFriendlyByteBuf b) { b.writeVarInt(entityId); b.writeVarInt(trainClassOrdinal); b.writeBoolean(hasCargo); b.writeDouble(freightSpeedKmh); b.writeDouble(passengerSpeedKmh); b.writeDouble(expressSpeedKmh);b.writeVarLong(destinationStationId);b.writeVarInt(stations.size());for(Station s:stations){b.writeVarLong(s.id());b.writeUtf(s.name(),64);} }
        private static OpenPayload read(RegistryFriendlyByteBuf b) {int entity=b.readVarInt(),type=b.readVarInt();boolean cargo=b.readBoolean();double freight=b.readDouble(),passenger=b.readDouble(),express=b.readDouble();long destination=b.readVarLong();int size=Math.clamp(b.readVarInt(),0,1000);java.util.ArrayList<Station> stations=new java.util.ArrayList<>();for(int i=0;i<size;i++)stations.add(new Station(b.readVarLong(),b.readUtf(64)));return new OpenPayload(entity,type,cargo,freight,passenger,express,destination,List.copyOf(stations)); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record UpdatePayload(int entityId, int trainClassOrdinal, double freightSpeedKmh,
                                double passengerSpeedKmh, double expressSpeedKmh,long destinationStationId) implements CustomPacketPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_train_class"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePayload> CODEC = CustomPacketPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(RegistryFriendlyByteBuf b) { b.writeVarInt(entityId); b.writeVarInt(trainClassOrdinal); b.writeDouble(freightSpeedKmh); b.writeDouble(passengerSpeedKmh); b.writeDouble(expressSpeedKmh);b.writeVarLong(destinationStationId); }
        private static UpdatePayload read(RegistryFriendlyByteBuf b) { return new UpdatePayload(b.readVarInt(), b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(),b.readVarLong()); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
}
