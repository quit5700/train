package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.cargo.StationSelectionService;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.freight.*;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import cn.quit5700.fabrictrain.train.TrainFormationService;
import cn.quit5700.fabrictrain.train.TrainClass;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class FreightNetworking {
    private FreightNetworking() {}

    public record Request(String json) implements CustomPacketPayload {
        public static final Type<Request> ID = new Type<>(FabricTrainMod.id("freight_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
                CustomPacketPayload.codec((p, b) -> b.writeUtf(p.json, 1024), b -> new Request(b.readUtf(1024)));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record Snapshot(String json) implements CustomPacketPayload {
        public static final Type<Snapshot> ID = new Type<>(FabricTrainMod.id("freight_snapshot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC =
                CustomPacketPayload.codec((p, b) -> b.writeUtf(p.json, 262144), b -> new Snapshot(b.readUtf(262144)));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(Request.ID, Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Snapshot.ID, Snapshot.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.ID, (payload, context) ->
                context.server().execute(() -> handle(context.player(), payload.json())));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.getBlockState(hit.getBlockPos()).is(ModBlocks.FREIGHT_YARD) || player.isSpectator())
                return InteractionResult.PASS;
            if (hit.getDirection() == Direction.UP
                    && player.getItemInHand(hand).getItem() instanceof BlockItem item
                    && item.getBlock() instanceof BaseRailBlock) return InteractionResult.PASS;
            if (player instanceof ServerPlayer operator && level instanceof ServerLevel server) {
                var device = RailwayDataManager.register(server, hit.getBlockPos(), DeviceType.FREIGHT_YARD);
                open(operator, device.id(), false);
            }
            return InteractionResult.SUCCESS;
        });
    }

    public static void membership(ServerPlayer player, long line) {
        FreightYardService.membership(player.serverLevel(), line);
    }

    public static void open(ServerPlayer player, long yardId, boolean remote) {
        var current = player.serverLevel();
        var device = RailwayDataManager.state(current).device(yardId);
        if (device == null) return;
        var pos = position(device);
        if (remote && DepotRemoteAccessSessions.hasManagerAccess(player.getUUID(), System.nanoTime()))
            DepotRemoteAccessSessions.authorize(player.getUUID(), device.dimension(), pos, System.nanoTime());
        var level = access(player, device);
        if (level != null) sendYard(player, level, device, 0, true);
    }

    private static BlockPos position(RailwayDevice device) {
        return new BlockPos(device.x(), device.y(), device.z());
    }

    private static ServerLevel access(ServerPlayer player, RailwayDevice device) {
        if (player.isSpectator() || device == null || device.type() != DeviceType.FREIGHT_YARD) return null;
        var current = player.serverLevel();
        var pos = position(device);
        if (device.dimension().equals(RailwayDataManager.dimension(current)) && current.hasChunkAt(pos)
                && current.getBlockState(pos).is(ModBlocks.FREIGHT_YARD)
                && player.distanceToSqr(Vec3.atCenterOf(pos)) <= 64) return current;
        if (!DepotRemoteAccessSessions.hasAccess(player.getUUID(), device.dimension(), pos, System.nanoTime())) return null;
        for (ServerLevel level : current.getServer().getAllLevels()) {
            if (!device.dimension().equals(RailwayDataManager.dimension(level))) continue;
            if (!level.hasChunkAt(pos) || level.getBlockState(pos).is(ModBlocks.FREIGHT_YARD)) return level;
        }
        return null;
    }

    private static void handle(ServerPlayer player, String json) {
        try {
            JsonObject request = JsonParser.parseString(json).getAsJsonObject();
            String action = request.get("action").getAsString();
            var current = player.serverLevel();
            RailwaySavedData data = RailwayDataManager.state(current);
            if (action.equals("manual_load")) {
                if (player.getVehicle() instanceof LocomotiveEntity head && head.getFirstPassenger() == player
                        && head.trainLink().controlMode().playerDriveAllowed()
                        && head.trainLink().trainClass() == TrainClass.CARGO
                        && TrainControllerManager.freightStopped(head)) {
                    var rail = cn.quit5700.fabrictrain.compat.MinecartCompat.currentRailPos(head);
                    var pos = rail.below();
                    if (LoadedRailWorld.state(current, pos).is(ModBlocks.FREIGHT_YARD)) {
                        var yardDevice = RailwayDataManager.register(current, pos, DeviceType.FREIGHT_YARD);
                        var yard = FreightYardService.yard(current, yardDevice);
                        if (FreightYardService.activeLine(data, yard) != null)
                            FreightYardService.loadEntities(current, yardDevice, TrainFormationService.formation(head));
                    }
                }
                return;
            }
            if (action.equals("stations")) {
                BlockPos pos = new BlockPos(request.get("x").getAsInt(), request.get("y").getAsInt(), request.get("z").getAsInt());
                if (player.distanceToSqr(Vec3.atCenterOf(pos)) > 64
                        && !DepotRemoteAccessSessions.hasAccess(player.getUUID(), RailwayDataManager.dimension(current), pos, System.nanoTime())) return;
                var result = new JsonObject();
                result.addProperty("kind", "stations");
                result.addProperty("line", request.get("line").getAsLong());
                var stations = new JsonArray();
                StationSelectionService.depotStations(current, data, request.get("line").getAsLong()).stream().limit(1000).forEach(station -> {
                    var row = new JsonObject();
                    row.addProperty("id", station.id());
                    row.addProperty("name", station.name());
                    stations.add(row);
                });
                result.add("stations", stations);
                send(player, result);
                return;
            }
            var device = data.device(request.get("yard").getAsLong());
            var level = access(player, device);
            if (level == null) return;
            FreightYard yard = FreightYardService.yard(level, device);
            var positions = FreightYardService.cargoPositions(level, FreightYardService.activeLine(data, yard));
            int page = request.get("page").getAsInt();
            boolean split = !positions.isEmpty();
            if (page < -1 || page > positions.size() || split && page == 0 || !split && page > 0) {
                if (action.equals("take")) return;
                page = split ? 1 : 0;
            }
            switch (action) {
                case "refresh" -> {}
                case "name" -> {
                    String name = request.get("name").getAsString().strip();
                    if (!name.isEmpty() && name.length() <= 64)
                        data.updateDevice(device.id(), name, device.dwellSeconds(), device.switchShapeOrdinal(), device.announcement());
                }
                case "filter" -> {
                    int car = request.get("car").getAsInt();
                    String item = request.get("item").getAsString();
                    if (car < 1 || car > positions.size() || item.length() > 128
                            || !BuiltInRegistries.ITEM.keySet().stream().anyMatch(key -> key.toString().equals(item))) return;
                    if (!yard.filters(car).remove(item)) yard.filters(car).add(item);
                }
                case "take" -> {
                    int slot = request.get("slot").getAsInt();
                    if (slot < 0 || slot >= FreightYard.SLOTS) return;
                    var container = page == -1 ? yard.returns : yard.page(page);
                    ItemStack stack = container.getItem(slot).copy();
                    if (!stack.isEmpty()) {
                        player.getInventory().add(stack);
                        container.setItem(slot, stack);
                    }
                }
                default -> { return; }
            }
            if (!action.equals("refresh")) cn.quit5700.fabrictrain.offline.InfrastructureService.data(level.getServer()).setDirty();
            sendYard(player, level, data.device(device.id()), page, false);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException ignored) {
            // An invalid client request never changes inventories.
        }
    }

    private static void sendYard(ServerPlayer player, ServerLevel level, RailwayDevice device, int page, boolean open) {
        var data = RailwayDataManager.state(level);
        FreightYard yard = FreightYardService.yard(level, device);
        RailwayLine line = FreightYardService.activeLine(data, yard);
        var positions = FreightYardService.cargoPositions(level, line);
        boolean split = !positions.isEmpty();
        if (page < -1 || page > positions.size() || split && page == 0 || !split && page > 0) page = split ? 1 : 0;
        var out = new JsonObject();
        out.addProperty("kind", "yard");
        out.addProperty("open", open);
        out.addProperty("yard", device.id());
        out.addProperty("name", device.name());
        out.addProperty("line", line == null ? "" : line.name());
        out.addProperty("split", split);
        out.addProperty("page", page);
        out.addProperty("returns", !yard.returns.isEmpty());
        var cars = new JsonArray();
        var filters = new JsonArray();
        for (int i = 0; i < positions.size(); i++) {
            cars.add(positions.get(i));
            JsonArray ids = new JsonArray();
            yard.filters(i + 1).forEach(ids::add);
            filters.add(ids);
        }
        out.add("cars", cars);
        out.add("filters", filters);
        var storage = new JsonArray();
        var container = page == -1 ? yard.returns : yard.page(page);
        for (int i = 0; i < FreightYard.SLOTS; i++) {
            ItemStack item = container.getItem(i);
            storage.add(item.isEmpty() ? JsonNull.INSTANCE : ItemStack.CODEC.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), item).result().orElse(JsonNull.INSTANCE));
        }
        out.add("items", storage);
        send(player, out);
    }

    private static void send(ServerPlayer player, JsonObject data) {
        String json = data.toString();
        if (json.length() <= 262144) ServerPlayNetworking.send(player, new Snapshot(json));
    }
}
