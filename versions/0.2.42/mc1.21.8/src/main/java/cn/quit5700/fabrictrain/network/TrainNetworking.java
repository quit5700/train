package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class TrainNetworking {
    private static final ResourceLocation DRIVE = FabricTrainMod.id("drive");

    private TrainNetworking() {
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(DrivePayload.ID, DrivePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DrivePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().getVehicle() instanceof PassengerCarEntity
                            || context.player().getVehicle() instanceof LocomotiveEntity) {
                        TrainControllerManager.drive(context.player(), context.player().getVehicle(), payload.forward(), payload.brake());
                    }
                }));
    }

    public record DrivePayload(boolean forward, boolean brake) implements CustomPacketPayload {
        public static final Type<DrivePayload> ID = new Type<>(DRIVE);
        public static final StreamCodec<RegistryFriendlyByteBuf, DrivePayload> CODEC = CustomPacketPayload.codec(
                DrivePayload::write, DrivePayload::read);

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBoolean(forward);
            buffer.writeBoolean(brake);
        }

        private static DrivePayload read(RegistryFriendlyByteBuf buffer) {
            return new DrivePayload(buffer.readBoolean(), buffer.readBoolean());
        }

        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
}
