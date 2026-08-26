package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.data.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.Container;
import net.minecraft.core.Direction;
import cn.quit5700.fabrictrain.route.SwitchRailRouting;
import cn.quit5700.fabrictrain.route.SwitchEntryDirectionRules;
import cn.quit5700.fabrictrain.route.SwitchEntryStateSynchronizer;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.registry.ModBlocks;

public final class DeviceSettingsNetworking {
    private DeviceSettingsNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playS2C().register(OpenPayload.ID, OpenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestOpenPayload.ID, RequestOpenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdatePayload.ID, UpdatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestOpenPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    BlockPos pos = payload.pos();
                    if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D)
                        return;
                    var state = player.level().getBlockState(pos);
                    DeviceType type = state.is(ModBlocks.STATION_MARKER) ? DeviceType.STATION
                            : state.is(ModBlocks.SWITCH_RAIL) ? DeviceType.SWITCH : null;
                    if (type == null || !(player.level() instanceof ServerLevel serverLevel)) return;
                    open(player, RailwayDataManager.register(serverLevel, pos, type));
                }));
        ServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) -> context.server().execute(() -> {
            RailwaySavedData data = RailwayDataManager.state(context.player().level());
            RailwayDevice before = data.device(payload.deviceId());
            if (before == null) return;
            int shape = Math.clamp(payload.switchShapeOrdinal(), 0, 2);
            RailwayDevice updated = data.updateDevice(payload.deviceId(), payload.name(), payload.dwellSeconds(), shape, payload.announcement());
            data.updateCargoDwell(payload.deviceId(), payload.cargoDwellSeconds());
            if (updated != null && before.type() == DeviceType.SWITCH) {
                for (ServerLevel level : context.player().level().getServer().getAllLevels()) {
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
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            if (!RailwayDataManager.dimension(level).equals(device.dimension())) continue;
            BlockPos pos = new BlockPos(device.x(), device.y(), device.z());
            if(device.type()==DeviceType.SWITCH){
                RailwayDevice synchronizedDevice = SwitchEntryStateSynchronizer.synchronizeDevice(
                        level, RailwayDataManager.state(level), device);
                if (synchronizedDevice != null) switchEntryOrdinal = synchronizedDevice.switchEntryOrdinal();
            }
            for (Direction direction : Direction.values()) {
                if (level.getBlockEntity(pos.relative(direction)) instanceof Container) { connected = true; break; }
            }
        }
        ServerPlayNetworking.send(player, new OpenPayload(device.id(), device.type().ordinal(), device.name(),
                device.dwellSeconds(), device.cargoDwellSeconds(), device.switchShapeOrdinal(), switchEntryOrdinal,
                device.announcement(), connected));
    }

    private static double squaredDistance(double x, double y, double z, RailwayDevice d) {
        double dx = x - d.x(), dy = y - d.y(), dz = z - d.z(); return dx * dx + dy * dy + dz * dz;
    }

    public record RequestOpenPayload(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestOpenPayload> ID = new Type<>(FabricTrainMod.id("request_open_device_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestOpenPayload> CODEC =
                CustomPacketPayload.codec((payload, buffer) -> buffer.writeBlockPos(payload.pos),
                        buffer -> new RequestOpenPayload(buffer.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record OpenPayload(long deviceId, int typeOrdinal, String name, int dwellSeconds, int cargoDwellSeconds, int switchShapeOrdinal, int switchEntryOrdinal,
                              String announcement, boolean containerConnected) implements CustomPacketPayload {
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_device_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = CustomPacketPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(RegistryFriendlyByteBuf b) { b.writeVarLong(deviceId); b.writeVarInt(typeOrdinal); b.writeUtf(name, 64); b.writeVarInt(dwellSeconds); b.writeVarInt(cargoDwellSeconds); b.writeVarInt(switchShapeOrdinal);b.writeVarInt(switchEntryOrdinal);b.writeUtf(announcement,128);b.writeBoolean(containerConnected); }
        private static OpenPayload read(RegistryFriendlyByteBuf b) { return new OpenPayload(b.readVarLong(), b.readVarInt(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readVarInt(),b.readVarInt(),b.readUtf(128),b.readBoolean()); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record UpdatePayload(long deviceId, String name, int dwellSeconds, int cargoDwellSeconds,
                                int switchShapeOrdinal, int switchEntryOrdinal, String announcement) implements CustomPacketPayload {
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_device_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePayload> CODEC = CustomPacketPayload.codec(UpdatePayload::write, UpdatePayload::read);
        private void write(RegistryFriendlyByteBuf b) { b.writeVarLong(deviceId); b.writeUtf(name, 64); b.writeVarInt(dwellSeconds); b.writeVarInt(cargoDwellSeconds); b.writeVarInt(switchShapeOrdinal);b.writeVarInt(switchEntryOrdinal);b.writeUtf(announcement,128); }
        private static UpdatePayload read(RegistryFriendlyByteBuf b) { return new UpdatePayload(b.readVarLong(), b.readUtf(64), b.readVarInt(), b.readVarInt(), b.readVarInt(),b.readVarInt(),b.readUtf(128)); }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
}
