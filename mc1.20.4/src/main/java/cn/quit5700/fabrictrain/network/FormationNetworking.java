package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.TrainFormationService;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public final class FormationNetworking {
    private FormationNetworking() {}

    public static void registerServer() {
        LegacyPayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        LegacyServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    Entity seed = context.player().serverLevel().getEntity(payload.seedEntityId());
                    if (!(seed instanceof LinkedTrainCar) || context.player().distanceToSqr(seed) > 256.0D) return;
                    if (payload.dissolve()) TrainFormationService.dissolve(seed);
                    else TrainFormationService.reorder(seed, payload.entityIds());
                }));
    }

    public static void open(ServerPlayer player, Entity seed) {
        List<Car> cars = TrainFormationService.formation(seed).stream()
                .map(entity -> new Car(entity.getId(), entity.getType().getDescription().getString())).toList();
        LegacyServerPlayNetworking.send(player, new OpenPayload(seed.getId(), cars));
    }

    public record Car(int entityId, String name) {}

    public record OpenPayload(int seedEntityId, List<Car> cars) implements LegacyPayload {
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_formation"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, OpenPayload> CODEC =
                LegacyPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(FriendlyByteBuf b) {
            b.writeVarInt(seedEntityId); b.writeVarInt(cars.size());
            for (Car car : cars) { b.writeVarInt(car.entityId()); b.writeUtf(car.name(), 128); }
        }
        private static OpenPayload read(FriendlyByteBuf b) {
            int seed = b.readVarInt(), size = cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(), 0, 101);
            ArrayList<Car> cars = new ArrayList<>(size);
            for (int i = 0; i < size; i++) cars.add(new Car(b.readVarInt(), b.readUtf(128)));
            return new OpenPayload(seed, List.copyOf(cars));
        }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }

    public record UpdatePayload(int seedEntityId, boolean dissolve, List<Integer> entityIds)
            implements LegacyPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_formation"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, UpdatePayload> CODEC =
                LegacyPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(FriendlyByteBuf b) {
            b.writeVarInt(seedEntityId); b.writeBoolean(dissolve); b.writeVarInt(entityIds.size());
            entityIds.forEach(b::writeVarInt);
        }
        private static UpdatePayload read(FriendlyByteBuf b) {
            int seed = b.readVarInt(); boolean dissolve = b.readBoolean();
            int size = cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(), 0, 101); ArrayList<Integer> ids = new ArrayList<>(size);
            for (int i = 0; i < size; i++) ids.add(b.readVarInt());
            return new UpdatePayload(seed, dissolve, List.copyOf(ids));
        }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}



