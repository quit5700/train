package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
import cn.quit5700.fabrictrain.item.LineSetterItem;
import java.util.List;
import cn.quit5700.fabrictrain.train.DerailedTrainRecoveryService;
import cn.quit5700.fabrictrain.train.TrainControllerManager;

public final class LineSettingsNetworking {
    private LineSettingsNetworking() {}

    public static void registerServer() {
        LegacyPayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        LegacyServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) -> context.server().execute(() -> {
            RailwaySavedData data = RailwayDataManager.state(context.player().serverLevel());
            RailwayLine line = data.line(payload.lineId());
            if (line == null) return;
            if (payload.delete()) {
                LineDeletionService.delete(context.server(), (net.minecraft.server.level.ServerLevel) context.player().serverLevel(), payload.lineId());
                return;
            }
            boolean structuralChange = payload.nodeAction() >= 0;
            if (structuralChange) data.modifyLineNode(payload.lineId(), payload.nodeIndex(), payload.nodeAction());
            if (payload.editNodes()) LineSetterItem.beginEditing(context.player().getUUID(), payload.lineId());
            LineStatus status = LineStatus.values()[cn.quit5700.fabrictrain.compat.MathCompat.clamp(payload.statusOrdinal(), 0, LineStatus.values().length - 1)];
            data.updateLineSettings(payload.lineId(), payload.name(), payload.intervalSeconds(), payload.speedKmh(), payload.maximumTrains(),
                    structuralChange ? LineStatus.NEEDS_VALIDATION : status);
            if (payload.refresh()) {
                RailwayLine updated = data.line(payload.lineId());
                boolean valid = WorldLineValidator.validate(context.player().serverLevel(), updated, data);
                data.setLineStatus(payload.lineId(), valid ? LineStatus.RUNNING : LineStatus.NEEDS_VALIDATION);
                DerailedTrainRecoveryService.recoverLoaded(context.player().serverLevel(), updated, data);
                if(valid)TrainControllerManager.startLine(context.server(),payload.lineId());
            }
        }));
    }

    public record OpenPayload(long lineId, String name, int statusOrdinal, int intervalSeconds, double speedKmh,
                              int maximumTrains, List<Long> nodeIds) implements LegacyPayload {
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_line_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, OpenPayload> CODEC = LegacyPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(FriendlyByteBuf b) { b.writeVarLong(lineId); b.writeUtf(name,64); b.writeVarInt(statusOrdinal); b.writeVarInt(intervalSeconds); b.writeDouble(speedKmh); b.writeVarInt(maximumTrains); b.writeVarInt(nodeIds.size()); for (long id : nodeIds) b.writeVarLong(id); }
        private static OpenPayload read(FriendlyByteBuf b) { long id=b.readVarLong(); String name=b.readUtf(64); int status=b.readVarInt(); int interval=b.readVarInt(); double speed=b.readDouble(); int maximum=b.readVarInt(); int size=cn.quit5700.fabrictrain.compat.MathCompat.clamp(b.readVarInt(),0,10000); java.util.ArrayList<Long> nodes=new java.util.ArrayList<>(size); for(int i=0;i<size;i++) nodes.add(b.readVarLong()); return new OpenPayload(id,name,status,interval,speed,maximum,List.copyOf(nodes)); }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }

    public record UpdatePayload(long lineId, String name, int statusOrdinal, int intervalSeconds, double speedKmh,
                                int maximumTrains, boolean refresh, boolean delete, int nodeIndex,
                                int nodeAction, boolean editNodes) implements LegacyPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_line_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, UpdatePayload> CODEC = LegacyPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(FriendlyByteBuf b) { b.writeVarLong(lineId); b.writeUtf(name,64); b.writeVarInt(statusOrdinal); b.writeVarInt(intervalSeconds); b.writeDouble(speedKmh); b.writeVarInt(maximumTrains); b.writeBoolean(refresh); b.writeBoolean(delete); b.writeVarInt(nodeIndex); b.writeVarInt(nodeAction); b.writeBoolean(editNodes); }
        private static UpdatePayload read(FriendlyByteBuf b) { return new UpdatePayload(b.readVarLong(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readDouble(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readBoolean()); }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}



