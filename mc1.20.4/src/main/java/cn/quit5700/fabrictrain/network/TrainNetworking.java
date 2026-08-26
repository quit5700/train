package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
import net.minecraft.resources.ResourceLocation;

public final class TrainNetworking {
    private static final ResourceLocation DRIVE = FabricTrainMod.id("drive");

    private TrainNetworking() {
    }

    public static void registerServer() {
        LegacyPayloadTypeRegistry.playC2S().register(DrivePayload.ID, DrivePayload.CODEC);
        LegacyServerPlayNetworking.registerGlobalReceiver(DrivePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().getVehicle() instanceof PassengerCarEntity
                            || context.player().getVehicle() instanceof LocomotiveEntity) {
                        TrainControllerManager.drive(context.player(), context.player().getVehicle(), payload.forward(), payload.brake());
                    }
                }));
    }

    public record DrivePayload(boolean forward, boolean brake) implements LegacyPayload {
        public static final Type<DrivePayload> ID = new Type<>(DRIVE);
        public static final LegacyPayloadCodec<FriendlyByteBuf, DrivePayload> CODEC = LegacyPayload.codec(
                DrivePayload::write, DrivePayload::read);

        private void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(forward);
            buffer.writeBoolean(brake);
        }

        private static DrivePayload read(FriendlyByteBuf buffer) {
            return new DrivePayload(buffer.readBoolean(), buffer.readBoolean());
        }

        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}


