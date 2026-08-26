package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
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
        LegacyPayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(UnloadAllPayload.ID, UnloadAllPayload.CODEC);
        LegacyServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) -> context.server().execute(() -> {
            Entity entity = context.player().serverLevel().getEntity(payload.entityId());
            if (!(entity instanceof CargoCarEntity cargo) || context.player().distanceToSqr(entity) > 64.0D) return;
            var data=RailwayDataManager.state(context.player().serverLevel());
            if(StationSelectionService.canSelect(data,cargo.trainLink(),payload.stationId()))
                cargo.setDestinationStationId(payload.stationId());
        }));
        LegacyServerPlayNetworking.registerGlobalReceiver(UnloadAllPayload.ID, (payload, context) -> context.server().execute(() -> {
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

    public record OpenPayload(int entityId, long selectedStationId, List<Station> stations) implements LegacyPayload {
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_cargo_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, OpenPayload> CODEC = LegacyPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(FriendlyByteBuf b) {
            b.writeVarInt(entityId); b.writeVarLong(selectedStationId); b.writeVarInt(stations.size());
            for (Station station : stations) { b.writeVarLong(station.id()); b.writeUtf(station.name(), 128); b.writeVarInt(station.etaSeconds()); }
        }
        private static OpenPayload read(FriendlyByteBuf b) {
            int entityId = b.readVarInt(); long selected = b.readVarLong(); int size = cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(), 0, 1000);
            java.util.ArrayList<Station> stations = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) stations.add(new Station(b.readVarLong(), b.readUtf(128), b.readVarInt()));
            return new OpenPayload(entityId, selected, List.copyOf(stations));
        }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }

    public record UpdatePayload(int entityId, long stationId) implements LegacyPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_cargo_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, UpdatePayload> CODEC = LegacyPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(FriendlyByteBuf b) { b.writeVarInt(entityId); b.writeVarLong(stationId); }
        private static UpdatePayload read(FriendlyByteBuf b) { return new UpdatePayload(b.readVarInt(), b.readVarLong()); }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }

    public record UnloadAllPayload(int entityId) implements LegacyPayload {
        public static final Type<UnloadAllPayload> ID = new Type<>(FabricTrainMod.id("unload_all_cargo"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, UnloadAllPayload> CODEC =
                LegacyPayload.codec((payload, buffer) -> buffer.writeVarInt(payload.entityId),
                        buffer -> new UnloadAllPayload(buffer.readVarInt()));
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}



