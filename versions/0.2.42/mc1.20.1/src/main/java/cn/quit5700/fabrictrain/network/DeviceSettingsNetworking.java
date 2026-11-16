package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.network.LegacyPayloadTypeRegistry;
import cn.quit5700.fabrictrain.network.LegacyServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import cn.quit5700.fabrictrain.network.LegacyPayloadCodec;
import cn.quit5700.fabrictrain.network.LegacyPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.Container;
import net.minecraft.core.Direction;
import cn.quit5700.fabrictrain.route.SwitchRailRouting;
import cn.quit5700.fabrictrain.route.SwitchEntryDirectionRules;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.registry.ModBlocks;

public final class DeviceSettingsNetworking {
    private DeviceSettingsNetworking() {}

    public static void registerServer() {
        LegacyPayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(RequestOpenPayload.ID, RequestOpenPayload.CODEC);
        LegacyPayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        LegacyServerPlayNetworking.registerGlobalReceiver(RequestOpenPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    BlockPos pos = payload.pos();
                    if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D)
                        return;
                    var state = player.serverLevel().getBlockState(pos);
                    DeviceType type = state.is(ModBlocks.STATION_MARKER) ? DeviceType.STATION
                            : state.is(ModBlocks.SWITCH_RAIL) ? DeviceType.SWITCH : null;
                    if (type == null) return;
                    ServerLevel serverLevel = player.serverLevel();
                    open(player, RailwayDataManager.register(serverLevel, pos, type));
                }));
        LegacyServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) -> context.server().execute(() -> {
            RailwaySavedData data = RailwayDataManager.state(context.player().serverLevel());
            RailwayDevice before = data.device(payload.deviceId());
            if (before == null) return;
            int shape = cn.quit5700.fabrictrain.compat.MathCompat.clamp(payload.switchShapeOrdinal(), 0, 2);
            RailwayDevice updated = data.updateDevice(payload.deviceId(), payload.name(), payload.dwellSeconds(), shape, payload.announcement());
            data.updateCargoDwell(payload.deviceId(), payload.cargoDwellSeconds());
            if (updated != null && before.type() == DeviceType.SWITCH) {
                for (ServerLevel level : context.player().serverLevel().getServer().getAllLevels()) {
                    if (RailwayDataManager.dimension(level).equals(before.dimension())) {
                        BlockPos pos=new BlockPos(before.x(),before.y(),before.z());
                        var state=level.getBlockState(pos);
                        Direction fallback=state.hasProperty(DeviceRailBlock.PLACEMENT_ENTRY)
                                ?state.getValue(DeviceRailBlock.PLACEMENT_ENTRY):Direction.NORTH;
                        Direction entry=SwitchEntryDirectionRules.resolve(payload.switchEntryOrdinal(),fallback);
                        updated=data.updateSwitchEntry(payload.deviceId(),entry.ordinal());
                        SwitchRailRouting.applySettings(level, updated, shape, entry);
                        break;
                    }
                }
            }
        }));
    }

    public static void open(net.minecraft.server.level.ServerPlayer player, RailwayDevice device) {
        boolean connected = false;
        int switchEntryOrdinal=device.switchEntryOrdinal();
        for (ServerLevel level : player.serverLevel().getServer().getAllLevels()) {
            if (!RailwayDataManager.dimension(level).equals(device.dimension())) continue;
            BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
            if(device.type()==DeviceType.SWITCH&&level.getBlockState(pos).hasProperty(DeviceRailBlock.PLACEMENT_ENTRY)){
                Direction entry=SwitchRailRouting.entryFor(device,level.getBlockState(pos));
                switchEntryOrdinal=entry.ordinal();
                if(device.switchEntryOrdinal()!=switchEntryOrdinal)
                    RailwayDataManager.state(level).updateSwitchEntry(device.id(),switchEntryOrdinal);
            }
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(pos.relative(direction)) instanceof Container) { connected = true; break; }
            }
        }
        LegacyServerPlayNetworking.send(player, new OpenPayload(device.id(), device.type().ordinal(), device.name(),
                device.dwellSeconds(), device.cargoDwellSeconds(), device.switchShapeOrdinal(), switchEntryOrdinal,
                device.announcement(), connected));
    }

    private static double squaredDistance(double x, double y, double z, RailwayDevice d) {
        double dx = x - d.x(), dy = y - d.y(), dz = z - d.z(); return dx * dx + dy * dy + dz * dz;
    }

    public record RequestOpenPayload(BlockPos pos) implements LegacyPayload {
        public static final Type<RequestOpenPayload> ID = new Type<>(FabricTrainMod.id("request_open_device_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, RequestOpenPayload> CODEC =
                LegacyPayload.codec((payload, buffer) -> buffer.writeBlockPos(payload.pos),
                        buffer -> new RequestOpenPayload(buffer.readBlockPos()));
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }

    public record OpenPayload(long deviceId, int typeOrdinal, String name, int dwellSeconds, int cargoDwellSeconds, int switchShapeOrdinal, int switchEntryOrdinal,
                              String announcement, boolean containerConnected) implements LegacyPayload {
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_device_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, OpenPayload> CODEC = LegacyPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(FriendlyByteBuf b) { b.writeVarLong(deviceId); b.writeVarInt(typeOrdinal); b.writeUtf(name, 64); b.writeVarInt(dwellSeconds); b.writeVarInt(cargoDwellSeconds); b.writeVarInt(switchShapeOrdinal);b.writeVarInt(switchEntryOrdinal);b.writeUtf(announcement,128);b.writeBoolean(containerConnected); }
        private static OpenPayload read(FriendlyByteBuf b) { return new OpenPayload(b.readVarLong(), b.readVarInt(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readVarInt(),b.readVarInt(),b.readUtf(128),b.readBoolean()); }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }

    public record UpdatePayload(long deviceId, String name, int dwellSeconds, int cargoDwellSeconds,
                                int switchShapeOrdinal, int switchEntryOrdinal, String announcement) implements LegacyPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_device_settings"));
        public static final LegacyPayloadCodec<FriendlyByteBuf, UpdatePayload> CODEC = LegacyPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(FriendlyByteBuf b) { b.writeVarLong(deviceId); b.writeUtf(name, 64); b.writeVarInt(dwellSeconds); b.writeVarInt(cargoDwellSeconds); b.writeVarInt(switchShapeOrdinal);b.writeVarInt(switchEntryOrdinal);b.writeUtf(announcement,128); }
        private static UpdatePayload read(FriendlyByteBuf b) { return new UpdatePayload(b.readVarLong(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readVarInt(),b.readVarInt(),b.readUtf(128)); }
        @Override public Type<? extends LegacyPayload> type() { return ID; }
    }
}



