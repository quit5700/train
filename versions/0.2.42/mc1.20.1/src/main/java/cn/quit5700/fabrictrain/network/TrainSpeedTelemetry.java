package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.train.LinkedTrainCar;
import cn.quit5700.fabrictrain.train.TrainSpeedPresentation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class TrainSpeedTelemetry {
    private record Position(UUID vehicle, Vec3 position) {}
    private static final Map<ServerPlayer, Position> PREVIOUS = new WeakHashMap<>();

    private TrainSpeedTelemetry() {}

    public static void register() {
        LegacyPayloadTypeRegistry.playS2C().register(Sample.ID, Sample.CODEC);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var vehicle = player.getVehicle();
            if (!(vehicle instanceof LinkedTrainCar)) {
                PREVIOUS.remove(player);
                continue;
            }
            var current = new Position(vehicle.getUUID(), vehicle.position());
            var previous = PREVIOUS.put(player, current);
            if (previous == null || !previous.vehicle().equals(current.vehicle())) continue;
            if (server.getTickCount() % 4 != 0 || !ServerPlayNetworking.canSend(player, Sample.ID.id())) continue;
            double speed = TrainSpeedPresentation.measuredSpeed(current.position().distanceTo(previous.position()));
            LegacyServerPlayNetworking.send(player, new Sample(current.vehicle(), speed));
        }
    }

    public record Sample(UUID vehicle, double speed) implements LegacyPayload {
        public static final Type<Sample> ID = new Type<>(FabricTrainMod.id("train_speed_sample"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, Sample> CODEC = LegacyPayload.codec(Sample::write, Sample::read);
        private void write(FriendlyByteBuf buffer) { buffer.writeUUID(vehicle); buffer.writeDouble(speed); }
        private static Sample read(FriendlyByteBuf buffer) { return new Sample(buffer.readUUID(), buffer.readDouble()); }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}
