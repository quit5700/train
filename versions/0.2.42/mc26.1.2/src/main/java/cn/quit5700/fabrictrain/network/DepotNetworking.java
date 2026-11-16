package cn.quit5700.fabrictrain.network;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.data.RailwayDataManager;
import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.data.RailwayDevice;
import cn.quit5700.fabrictrain.data.RailwayLine;
import cn.quit5700.fabrictrain.data.RailwaySavedData;
import cn.quit5700.fabrictrain.depot.DepotDispatchService;
import cn.quit5700.fabrictrain.depot.DepotDispatchResult;
import cn.quit5700.fabrictrain.depot.DepotTemplate;
import cn.quit5700.fabrictrain.depot.DepotDispatchConfig;
import cn.quit5700.fabrictrain.depot.DepotSettingsAction;
import cn.quit5700.fabrictrain.train.ControlMode;
import cn.quit5700.fabrictrain.depot.DepotDispatchPrecheck;
import cn.quit5700.fabrictrain.route.WorldLineValidator;
import cn.quit5700.fabrictrain.route.RuntimePathCache;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.train.TrainClass;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import cn.quit5700.fabrictrain.data.LineStatus;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import net.minecraft.world.entity.Entity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class DepotNetworking {
    private DepotNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.clientboundPlay().register(OpenPayload.ID, OpenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TrainListPayload.ID, TrainListPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestOpenPayload.ID,RequestOpenPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestTrainListPayload.ID, RequestTrainListPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeleteTrainPayload.ID, DeleteTrainPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ClosePayload.ID, ClosePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdatePayload.ID, UpdatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RequestOpenPayload.ID,(payload,context)->context.server().execute(()->{
            ServerPlayer player=context.player();BlockPos pos=payload.pos();
            if (!canAccess(player, pos)) { denyAccess(player); return; }
            if(player.level().getBlockEntity(pos) instanceof TrainDepotBlockEntity depot)open(player,pos,depot);
        }));
        ServerPlayNetworking.registerGlobalReceiver(RequestTrainListPayload.ID, (payload, context) ->
                context.server().execute(() -> sendTrainList(context.player(), payload.pos())));
        ServerPlayNetworking.registerGlobalReceiver(DeleteTrainPayload.ID, (payload, context) ->
                context.server().execute(() -> deleteListedTrain(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ClosePayload.ID, (payload, context) ->
                context.server().execute(() -> DepotRemoteAccessSessions.revokeAll(context.player().getUUID())));
        ServerPlayNetworking.registerGlobalReceiver(UpdatePayload.ID, (payload, context) ->
                context.server().execute(() -> update(context.player(), payload)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                { cn.quit5700.fabrictrain.depot.DispatchAuthorityService.refreshPlayer(handler.getPlayer()); DepotRemoteAccessSessions.revokeAll(handler.getPlayer().getUUID()); });
    }

    private static void sendTrainList(ServerPlayer player, BlockPos pos) {
        if (!canAccess(player, pos)) { denyAccess(player); return; }
        if (!(player.level().getBlockEntity(pos) instanceof TrainDepotBlockEntity depot)) return;
        RailwaySavedData data = RailwayDataManager.state(player.level());
        long depotId=deviceIdAt(player,pos);
        List<TrainListEntry> trains = data.presentTrainsFromDepot(depotId).values().stream()
                .sorted(java.util.Comparator.comparingLong(record -> record.number()))
                .map(record -> new TrainListEntry(record.trainId(), record.number(),
                        record.controlMode() == ControlMode.ROUTE_AUTOMATIC, record.lineId(),
                        data.line(record.lineId())==null?"":data.line(record.lineId()).name(),
                        data.stationHistoryText(record.trainId())))
                .toList();
        ServerPlayNetworking.send(player, new TrainListPayload(pos, trains));
    }

    private static void deleteListedTrain(ServerPlayer player, DeleteTrainPayload payload) {
        if (!canAccess(player, payload.pos())) { denyAccess(player); return; }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof TrainDepotBlockEntity depot)) return;
        RailwaySavedData data = RailwayDataManager.state(player.level());
        long depotId=deviceIdAt(player,payload.pos());
        if (!data.presentTrainsFromDepot(depotId).containsKey(payload.trainId())) {
            sendTrainList(player, payload.pos());
            return;
        }
        TrainControllerManager.deleteTrainEverywhere(player.level().getServer(), payload.trainId());
        sendTrainList(player, payload.pos());
    }

    public static void open(ServerPlayer player, BlockPos pos, TrainDepotBlockEntity depot) {
        open(player, pos, depot, "");
    }

    private static void open(ServerPlayer player, BlockPos pos, TrainDepotBlockEntity depot,
                             String operationMessageKey, String... operationArguments) {
        var data = RailwayDataManager.state(player.level());
        List<LineOption> lines = data.lines().values().stream()
                .sorted(java.util.Comparator.comparingLong(RailwayLine::id))
                .map(line -> new LineOption(line.id(), line.name(), line.status().ordinal())).toList();
        long depotId = deviceIdAt(player, pos);
        long selectedLineId = depotId == 0L ? 0L : data.dispatchLineIdForDepot(depotId);
        if (depot.selectedLineId() != selectedLineId) depot.setSelectedLineId(selectedLineId);
        long active = selectedLineId == 0 ? 0 : data.activeAutomaticTrainCount(selectedLineId);
        RailwayLine selectedLine = data.line(selectedLineId);
        int lineStatus = selectedLine == null ? -1 : selectedLine.status().ordinal();
        int pauseState = pauseState(player, selectedLine, data);
        RailwayDevice depotDevice = data.device(depotId);
        String depotName = depotDevice == null ? "" : depotDevice.name();
        TrainClassNetworking.authorizeSettings(player);
        ServerPlayNetworking.send(player, new OpenPayload(pos, depotName, depot.template().encode(), selectedLineId,
                depot.trainClass().ordinal(), depot.dispatchIntervalSeconds(), depot.maximumTrains(),
                active,data.classSpeedLimit(TrainClass.CARGO),data.classSpeedLimit(TrainClass.PASSENGER),
                data.classSpeedLimit(TrainClass.EXPRESS), depot.automaticDispatchEnabled(), lineStatus, pauseState,
                depot.lastAutomaticDispatchResultOrdinal(), operationMessageKey == null ? "" : operationMessageKey,
                List.of(operationArguments), lines));
    }

    private static long deviceIdAt(ServerPlayer player, BlockPos pos) {
        var device = RailwayDataManager.state(player.level()).find(RailwayDataManager.dimension(player.level()),
                pos.getX(), pos.getY(), pos.getZ());
        return device == null ? 0 : device.id();
    }

    private static void update(ServerPlayer player, UpdatePayload payload) {
        if (!canAccess(player, payload.pos())) { denyAccess(player); return; }
        if (!(player.level().getBlockEntity(payload.pos()) instanceof TrainDepotBlockEntity depot)) return;
        ServerLevel level = (ServerLevel) player.level();
        RailwaySavedData data = RailwayDataManager.state(level);
        RailwayDevice device = RailwayDataManager.register(level, payload.pos(), DeviceType.DEPOT);
        DepotSettingsAction action = DepotSettingsAction.values()[Math.clamp(payload.action(), 0,
                DepotSettingsAction.values().length - 1)];
        if(payload.maximum()<0){open(player,payload.pos(),depot,"message.fabric_train_simulator.depot.invalid_maximum");return;}
        if(payload.maximum()==0&&(action==DepotSettingsAction.DISPATCH_AUTOMATIC)){
            depot.configure(depot.selectedLineId(),depot.trainClass(),depot.dispatchIntervalSeconds(),0,depot.speedLimitKmh(TrainClass.CARGO),depot.speedLimitKmh(TrainClass.PASSENGER),depot.speedLimitKmh(TrainClass.EXPRESS));
            depot.setAutomaticDispatchEnabled(false);
            cn.quit5700.fabrictrain.depot.DepotRecallService.request(level,payload.pos(),depot,device.id());
            open(player,payload.pos(),depot,"message.fabric_train_simulator.depot.recalled");return;
        }
        if(action==DepotSettingsAction.GENERATE_MANUAL&&(payload.manualMode()<0||payload.manualMode()>1)){
            open(player,payload.pos(),depot,"message.fabric_train_simulator.depot.manual_mode_required");return;
        }
        DepotTemplate template = DepotTemplate.decode(payload.template());
        if (!template.destinations().isEmpty()) {
            var eligible = cn.quit5700.fabrictrain.cargo.StationSelectionService.depotStations(level, data, payload.lineId())
                    .stream().map(RailwayDevice::id).collect(java.util.stream.Collectors.toSet());
            template = template.filterDestinations(id -> id == 0L || eligible.contains(id));
        }
        TrainClass trainClass = TrainClass.values()[Math.clamp(payload.trainClass(), 0, TrainClass.values().length - 1)];
        DepotDispatchConfig config = new DepotDispatchConfig(template, trainClass, payload.interval(), payload.maximum(),
                payload.cargoSpeedKmh(), payload.passengerSpeedKmh(), payload.expressSpeedKmh());
        if (action == DepotSettingsAction.OPEN_LINE) {
            if (data.line(payload.lineId()) != null) {
                DepotRemoteAccessSessions.authorizeManager(player.getUUID(), System.nanoTime());
                RailwayManagerNetworking.open(player, payload.lineId());
            }
            return;
        }
        if (action == DepotSettingsAction.TOGGLE_PAUSE) {
            RailwayLine line = data.line(payload.lineId());
            String operationKey;
            if (line == null) {
                operationKey = "message.fabric_train_simulator.depot.no_line";
            } else if (line.status() == LineStatus.RUNNING) {
                boolean hasStation = line.nodeIds().stream().map(data::device)
                        .anyMatch(candidate -> candidate != null && candidate.type() == DeviceType.STATION);
                if (!hasStation) {
                    open(player, payload.pos(), depot,
                            "message.fabric_train_simulator.depot.pause_no_station");
                    return;
                }
                data.setLineStatus(line.id(), LineStatus.PAUSED);
                operationKey = "message.fabric_train_simulator.depot.paused";
            } else if (line.status() == LineStatus.PAUSED) {
                data.setLineStatus(line.id(), LineStatus.RUNNING);
                DepotDispatchService.resetAutomaticTimer(line.id());
                TrainControllerManager.continueAutomaticLine(player.level().getServer(), line.id());
                operationKey = "message.fabric_train_simulator.depot.continued";
            } else {
                operationKey = precheckKey(DepotDispatchPrecheck.check(
                        line.status(), data.activeAutomaticTrainCount(line.id()), payload.maximum()));
            }
            open(player, payload.pos(), depot, operationKey);
            return;
        }
        if (!saveSettings(player, payload, depot, data, device, template, trainClass)) return;
        boolean saving = action == DepotSettingsAction.SAVE;
        boolean reset = action == DepotSettingsAction.DISPATCH_AUTOMATIC;
        boolean automatic = saving || reset;
        if (saving && (payload.lineId() == 0L || depot.maximumTrains() == 0)) {
            open(player, payload.pos(), depot, "message.fabric_train_simulator.depot.saved");
            return;
        }
        RailwayLine line = data.line(payload.lineId());
        if (automatic) {
            DepotDispatchPrecheck.Result precheck = DepotDispatchPrecheck.check(
                    line == null ? null : line.status(),
                    reset ? 0L : data.activeAutomaticTrainCount(payload.lineId()), depot.maximumTrains());
            if (precheck == DepotDispatchPrecheck.Result.READY
                    || line != null && line.status() == cn.quit5700.fabrictrain.data.LineStatus.NEEDS_VALIDATION) {
                RailPowerService.refreshNow(level);
                RuntimePathCache.remove(line.id());
                WorldLineValidator.ImmediateDispatchResult validation = WorldLineValidator.validateForImmediateDispatchDetailed(
                        level, payload.pos(), line, data);
                if (validation.status() != WorldLineValidator.ImmediateDispatchStatus.READY) {
                    if (validation.status() == WorldLineValidator.ImmediateDispatchStatus.TRACK_BROKEN
                            && validation.fromIndex() >= 0 && validation.toIndex() >= 0) {
                        RailwayDevice from = data.device(line.nodeIds().get(validation.fromIndex()));
                        RailwayDevice to = data.device(line.nodeIds().get(validation.toIndex()));
                        if (from != null && to != null) {
                            open(player, payload.pos(), depot,
                                    "message.fabric_train_simulator.depot.track_broken_between",
                                    from.name(), to.name());
                        } else open(player, payload.pos(), depot, immediateValidationKey(validation.status()));
                    } else open(player, payload.pos(), depot, immediateValidationKey(validation.status()));
                    return;
                }
                data.setLineStatus(line.id(), cn.quit5700.fabrictrain.data.LineStatus.RUNNING);
                line = data.line(line.id());
                precheck = DepotDispatchPrecheck.check(line.status(), reset ? 0L : data.activeAutomaticTrainCount(line.id()), depot.maximumTrains());
            }
            // A valid automatic plan must keep retrying after inventory is replenished.
            if ((precheck == DepotDispatchPrecheck.Result.READY
                    || precheck == DepotDispatchPrecheck.Result.MAXIMUM_REACHED)
                    && depot.maximumTrains() > 0 && depot.selectedLineId() == payload.lineId()
                    && line != null && line.status() == cn.quit5700.fabrictrain.data.LineStatus.RUNNING) {
                depot.setAutomaticDispatchEnabled(true);
                depot.setCreativeAutomaticDispatch(player.isCreative());
                cn.quit5700.fabrictrain.offline.InfrastructureService.cacheValidatedPlan(level,payload.pos(),line,config);
            }
            if (precheck != DepotDispatchPrecheck.Result.READY) {
                String operationKey = precheckKey(precheck);
                open(player, payload.pos(), depot, operationKey);
                return;
            }
        }
        ControlMode controlMode = automatic
                ? ControlMode.ROUTE_AUTOMATIC : ControlMode.MANUAL;
        Set<java.util.UUID> previousAutomaticTrains = reset
                ? TrainControllerManager.automaticTrainIds(level, line.id()) : Set.of();
        DepotDispatchService.DispatchOutcome outcome = DepotDispatchService.dispatchConfiguredOutcome(
                level, payload.pos(), line, depot, config, !player.isCreative(), controlMode);
        DepotDispatchResult result = outcome.result();
        if(result.succeeded()&&action==DepotSettingsAction.GENERATE_MANUAL)DepotDispatchService.configureManualMode(outcome,payload.manualMode()==0);
        if (result.succeeded() && automatic) {
            if (reset) TrainControllerManager.refreshAutomaticLine(level, line.id(), outcome.trainId(), previousAutomaticTrains);
            if (depot.selectedLineId() == payload.lineId()) {
                depot.setAutomaticDispatchEnabled(true);
                depot.setCreativeAutomaticDispatch(player.isCreative());
            }
            DepotDispatchService.resetAutomaticTimer(payload.lineId());
        }
        if (automatic) depot.setLastAutomaticDispatchResult(result);
        String resultKey = outcome.messageKey();
        if (result.succeeded()) {
            if (saving) resultKey = "message.fabric_train_simulator.depot.saved_and_dispatched";
            else if (reset) resultKey = "message.fabric_train_simulator.depot.automatic_generated";
            else resultKey = "message.fabric_train_simulator.depot.manual_generated";
        }
        open(player, payload.pos(), depot, resultKey, outcome.messageArguments());
    }

    private static boolean saveSettings(ServerPlayer player, UpdatePayload payload, TrainDepotBlockEntity depot,
                                        RailwaySavedData data, RailwayDevice device, DepotTemplate template,
                                        TrainClass trainClass) {
        long displacedDepotId;
        try {
            displacedDepotId = data.bindDispatchDepot(device.id(), payload.lineId());
        } catch (IllegalArgumentException invalidBinding) {
            open(player, payload.pos(), depot, "message.fabric_train_simulator.depot.no_line");
            return false;
        }
        clearDisplacedDepotBinding(player, data, displacedDepotId, payload.lineId());
        cn.quit5700.fabrictrain.depot.DispatchAuthorityService.authorize((ServerLevel) player.level(), device.id(), player);
        data.updateDevice(device.id(), payload.name(), device.dwellSeconds(), device.switchShapeOrdinal(), device.announcement());
        depot.setTemplate(template);
        depot.configure(payload.lineId(), trainClass, payload.interval(), payload.maximum(),payload.cargoSpeedKmh(),
                payload.passengerSpeedKmh(),payload.expressSpeedKmh());
        if (payload.lineId() == 0L || data.line(payload.lineId()) == null
                || data.line(payload.lineId()).status() != cn.quit5700.fabrictrain.data.LineStatus.RUNNING)
            depot.setAutomaticDispatchEnabled(false);
        return true;
    }

    private static boolean canAccess(ServerPlayer player, BlockPos pos) {
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D)
            return true;
        return DepotRemoteAccessSessions.hasAccess(player.getUUID(),
                RailwayDataManager.dimension((ServerLevel) player.level()), pos, System.nanoTime());
    }

    private static void denyAccess(ServerPlayer player) {
    }

    private static String precheckKey(DepotDispatchPrecheck.Result result) {
        return switch (result) {
            case NO_LINE -> "message.fabric_train_simulator.depot.no_line";
            case MAXIMUM_REACHED -> "message.fabric_train_simulator.depot.maximum_reached";
            case PAUSED -> "message.fabric_train_simulator.depot.line_paused";
            case CLOSED -> "message.fabric_train_simulator.depot.line_closed";
            case MAINTENANCE -> "message.fabric_train_simulator.depot.line_maintenance";
            case NEEDS_VALIDATION -> "message.fabric_train_simulator.depot.validation_required";
            case READY -> "message.fabric_train_simulator.depot.success";
        };
    }

    private static String immediateValidationKey(WorldLineValidator.ImmediateDispatchStatus status) {
        return switch (status) {
            case FIRST_NODE_NOT_FOUND -> "message.fabric_train_simulator.depot.first_node_not_found";
            case STATION_STOP_UNBOUND -> "message.fabric_train_simulator.depot.station_stop_unbound";
            case TRACK_BROKEN -> "message.fabric_train_simulator.depot.track_broken";
            case UNPOWERED -> "message.fabric_train_simulator.depot.unpowered";
            case READY -> "message.fabric_train_simulator.depot.success";
        };
    }

    private static void clearDisplacedDepotBinding(ServerPlayer player, RailwaySavedData data,
                                                    long displacedDepotId, long lineId) {
        if (displacedDepotId == 0L) return;
        RailwayDevice displaced = data.device(displacedDepotId);
        if (displaced == null) return;
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            if (!RailwayDataManager.dimension(level).equals(displaced.dimension())) continue;
            BlockPos displacedPos = new BlockPos(displaced.x(), displaced.y(), displaced.z());
            if (level.hasChunkAt(displacedPos)
                    && level.getBlockEntity(displacedPos) instanceof TrainDepotBlockEntity oldDepot
                    && oldDepot.selectedLineId() == lineId) {
                oldDepot.setSelectedLineId(0L);
            }
            return;
        }
    }

    private static int pauseState(ServerPlayer player, RailwayLine line, RailwaySavedData data) {
        if (line == null) return -1;
        if (line.status() == LineStatus.RUNNING) return 0;
        if (line.status() != LineStatus.PAUSED) return -1;
        boolean found = false;
        boolean allHeld = true;
        for (Entity entity : player.level().getAllEntities()) {
            if (!(entity instanceof LocomotiveEntity locomotive)
                    || locomotive.trainLink().lineId() != line.id()
                    || locomotive.trainLink().controlMode() != ControlMode.ROUTE_AUTOMATIC) continue;
            found = true;
            RailwayDevice last = data.device(locomotive.trainLink().lastDeviceId());
            if (locomotive.trainLink().automaticRunning() || last == null || last.type() != DeviceType.STATION)
                allHeld = false;
        }
        return !found || allHeld ? 2 : 1;
    }

    public record LineOption(long id, String name, int status) {}

    public record RequestOpenPayload(BlockPos pos) implements CustomPacketPayload{
        public static final Type<RequestOpenPayload> ID=new Type<>(FabricTrainMod.id("request_open_depot_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf,RequestOpenPayload> CODEC=CustomPacketPayload.codec(
                (payload,buffer)->buffer.writeBlockPos(payload.pos),buffer->new RequestOpenPayload(buffer.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type(){return ID;}
    }

    public record RequestTrainListPayload(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestTrainListPayload> ID = new Type<>(FabricTrainMod.id("request_depot_train_list"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestTrainListPayload> CODEC =
                CustomPacketPayload.codec((payload, buffer) -> buffer.writeBlockPos(payload.pos),
                        buffer -> new RequestTrainListPayload(buffer.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record DeleteTrainPayload(BlockPos pos, java.util.UUID trainId) implements CustomPacketPayload {
        public static final Type<DeleteTrainPayload> ID = new Type<>(FabricTrainMod.id("delete_depot_train"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteTrainPayload> CODEC =
                CustomPacketPayload.codec((payload, buffer) -> {
                    buffer.writeBlockPos(payload.pos);
                    buffer.writeUUID(payload.trainId);
                }, buffer -> new DeleteTrainPayload(buffer.readBlockPos(), buffer.readUUID()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record ClosePayload(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ClosePayload> ID = new Type<>(FabricTrainMod.id("close_depot_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClosePayload> CODEC =
                CustomPacketPayload.codec((payload, buffer) -> buffer.writeBlockPos(payload.pos),
                        buffer -> new ClosePayload(buffer.readBlockPos()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record TrainListEntry(java.util.UUID trainId,long number,boolean automatic,long lineId,String lineName,String stationHistory) {
        public TrainListEntry(java.util.UUID id,long number,boolean automatic){this(id,number,automatic,0L,"","");}
        public TrainListEntry(java.util.UUID id,long number,boolean automatic,long lineId,String lineName) {
            this(id, number, automatic, lineId, lineName, "");
        }
    }

    public record TrainListPayload(BlockPos depotPos, List<TrainListEntry> trains) implements CustomPacketPayload {
        public static final Type<TrainListPayload> ID = new Type<>(FabricTrainMod.id("open_depot_train_list_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TrainListPayload> CODEC =
                CustomPacketPayload.codec(TrainListPayload::write, TrainListPayload::read);

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeBlockPos(depotPos);
            buffer.writeVarInt(trains.size());
            for (TrainListEntry train : trains) {
                buffer.writeUUID(train.trainId());
                buffer.writeVarLong(train.number());
                buffer.writeBoolean(train.automatic());buffer.writeVarLong(train.lineId());buffer.writeUtf(train.lineName(),64);
                buffer.writeUtf(train.stationHistory(), 192);
            }
        }

        private static TrainListPayload read(RegistryFriendlyByteBuf buffer) {
            BlockPos depotPos = buffer.readBlockPos();
            int count = Math.clamp(buffer.readVarInt(), 0, 10000);
            ArrayList<TrainListEntry> trains = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                trains.add(new TrainListEntry(buffer.readUUID(), buffer.readVarLong(), buffer.readBoolean(),buffer.readVarLong(),buffer.readUtf(64),buffer.readUtf(192)));
            }
            return new TrainListPayload(depotPos, List.copyOf(trains));
        }

        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record OpenPayload(BlockPos pos, String name, String template, long lineId, int trainClass, int interval, int maximum,
                              long active,double cargoSpeedKmh,double passengerSpeedKmh,double expressSpeedKmh,
                              boolean automaticDispatchEnabled, int lineStatus, int pauseState,
                              int lastAutomaticDispatchResult,
                              String operationMessageKey,
                              List<String> operationArguments,
                              List<LineOption> lines) implements CustomPacketPayload {
        public OpenPayload {
            operationArguments = List.copyOf(operationArguments);
            lines = List.copyOf(lines);
        }
        public OpenPayload(BlockPos pos, String name, String template, long lineId, int trainClass, int interval, int maximum,
                           long active,double cargoSpeedKmh,double passengerSpeedKmh,double expressSpeedKmh,
                           boolean automaticDispatchEnabled, int lineStatus, int pauseState,
                           int lastAutomaticDispatchResult, String operationMessageKey, List<LineOption> lines) {
            this(pos,name,template,lineId,trainClass,interval,maximum,active,cargoSpeedKmh,passengerSpeedKmh,
                    expressSpeedKmh,automaticDispatchEnabled,lineStatus,pauseState,lastAutomaticDispatchResult,
                    operationMessageKey,List.of(),lines);
        }
        public static final Type<OpenPayload> ID = new Type<>(FabricTrainMod.id("open_depot_settings_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = CustomPacketPayload.codec(OpenPayload::write, OpenPayload::read);
        private void write(RegistryFriendlyByteBuf b) {
            b.writeBlockPos(pos); b.writeUtf(name, 64); b.writeUtf(template, DepotTemplate.MAX_ENCODED_LENGTH); b.writeVarLong(lineId); b.writeVarInt(trainClass);
            b.writeVarInt(interval); b.writeVarInt(maximum); b.writeVarLong(active);
            b.writeDouble(cargoSpeedKmh);b.writeDouble(passengerSpeedKmh);b.writeDouble(expressSpeedKmh);b.writeBoolean(automaticDispatchEnabled);b.writeVarInt(lineStatus);b.writeVarInt(pauseState);b.writeVarInt(lastAutomaticDispatchResult);b.writeUtf(operationMessageKey,128);
            b.writeVarInt(operationArguments.size());for(String argument:operationArguments)b.writeUtf(argument,64);
            b.writeVarInt(lines.size());
            for (LineOption line : lines) { b.writeVarLong(line.id()); b.writeUtf(line.name(), 64); b.writeVarInt(line.status()); }
        }
        private static OpenPayload read(RegistryFriendlyByteBuf b) {
            BlockPos pos=b.readBlockPos();String name=b.readUtf(64);String template=b.readUtf(DepotTemplate.MAX_ENCODED_LENGTH);long line=b.readVarLong();int type=b.readVarInt();
            int interval=b.readVarInt(),maximum=b.readVarInt();long active=b.readVarLong();
            double cargoSpeed=b.readDouble(),passengerSpeed=b.readDouble(),expressSpeed=b.readDouble();boolean enabled=b.readBoolean();int lineStatus=b.readVarInt();int pauseState=b.readVarInt();int lastResult=b.readVarInt();String operationMessageKey=b.readUtf(128);
            int argumentCount=b.readVarInt();if(argumentCount<0||argumentCount>8)throw new IllegalArgumentException("Invalid depot operation argument count");
            ArrayList<String> operationArguments=new ArrayList<>();for(int i=0;i<argumentCount;i++)operationArguments.add(b.readUtf(64));
            int size=Math.clamp(b.readVarInt(),0,1000);
            ArrayList<LineOption> lines=new ArrayList<>();for(int i=0;i<size;i++)lines.add(new LineOption(b.readVarLong(),b.readUtf(64),b.readVarInt()));
            return new OpenPayload(pos,name,template,line,type,interval,maximum,active,cargoSpeed,passengerSpeed,expressSpeed,enabled,lineStatus,pauseState,lastResult,operationMessageKey,operationArguments,List.copyOf(lines));
        }
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }

    public record UpdatePayload(BlockPos pos, String name, String template, long lineId, int trainClass, int interval,
                                int maximum,double cargoSpeedKmh,double passengerSpeedKmh,double expressSpeedKmh,
                                int action,int manualMode) implements CustomPacketPayload {
        public UpdatePayload(BlockPos pos,String name,String template,long lineId,int trainClass,int interval,int maximum,double cargo,double passenger,double express,int action){
            this(pos,name,template,lineId,trainClass,interval,maximum,cargo,passenger,express,action,-1);
        }
        public static final Type<UpdatePayload> ID = new Type<>(FabricTrainMod.id("update_depot_settings_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePayload> CODEC = CustomPacketPayload.codec(
                (p,b)->{b.writeBlockPos(p.pos);b.writeUtf(p.name,64);b.writeUtf(p.template, DepotTemplate.MAX_ENCODED_LENGTH);b.writeVarLong(p.lineId);b.writeVarInt(p.trainClass);b.writeVarInt(p.interval);b.writeVarInt(p.maximum);b.writeDouble(p.cargoSpeedKmh);b.writeDouble(p.passengerSpeedKmh);b.writeDouble(p.expressSpeedKmh);b.writeVarInt(p.action);b.writeVarInt(p.manualMode);},
                b->new UpdatePayload(b.readBlockPos(),b.readUtf(64),b.readUtf(DepotTemplate.MAX_ENCODED_LENGTH),b.readVarLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble(),b.readVarInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return ID; }
    }
}
