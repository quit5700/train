package cn.quit5700.fabrictrain;

import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.registry.ModItems;
import cn.quit5700.fabrictrain.registry.ModItemGroup;
import cn.quit5700.fabrictrain.registry.ModEntities;
import cn.quit5700.fabrictrain.registry.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import cn.quit5700.fabrictrain.item.LineSetterItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import cn.quit5700.fabrictrain.train.TrainControllerManager;
import cn.quit5700.fabrictrain.train.TrainInteractionEvents;
import cn.quit5700.fabrictrain.network.TrainNetworking;
import cn.quit5700.fabrictrain.depot.DepotDispatchService;
import cn.quit5700.fabrictrain.network.DepotNetworking;
import cn.quit5700.fabrictrain.network.LineSettingsNetworking;
import cn.quit5700.fabrictrain.network.TrainClassNetworking;
import cn.quit5700.fabrictrain.cargo.CargoLedgerService;
import cn.quit5700.fabrictrain.train.TrainSession;
import cn.quit5700.fabrictrain.train.OfflineTrainService;
import cn.quit5700.fabrictrain.network.CargoSettingsNetworking;
import cn.quit5700.fabrictrain.network.DeviceSettingsNetworking;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.network.RailwayManagerNetworking;
import cn.quit5700.fabrictrain.network.FormationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public final class FabricTrainMod implements ModInitializer {
    public static final String MOD_ID = "fabric_train_simulator";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        cn.quit5700.fabrictrain.network.DeviceNamePackets.register();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModEntities.initialize();
        ModItems.initialize();
        cn.quit5700.stationaccess.StationAccess.initialize();
        ModItemGroup.initialize();
        cn.quit5700.fabrictrain.compat.TrainHelpCompat.register();
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            cn.quit5700.fabrictrain.data.RailwayDataMigration.prepare(server, false);
            cn.quit5700.stationaccess.AccessData.prepare(server);
            TrainSession.beginServerSession();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            cn.quit5700.fabrictrain.data.RailwayDataManager.state(server.overworld()).prepareTrainRegistryForRestart();
            cn.quit5700.fabrictrain.data.DeviceRecoveryService.begin(server);
            cn.quit5700.fabrictrain.offline.InfrastructureService.begin(server);
            OfflineTrainService.beginServerRecovery(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LineSetterItem.clearSessions());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> cn.quit5700.fabrictrain.item.TrainConnectorItem.clearSessions());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TrainControllerManager.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> OfflineTrainService.clearRuntime());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(cn.quit5700.fabrictrain.offline.InfrastructureService::chunkLoad);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_UNLOAD.register(cn.quit5700.fabrictrain.offline.InfrastructureService::chunkUnload);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity,level)->cn.quit5700.fabrictrain.offline.InfrastructureService.discardIfStale(level,entity));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->TrainControllerManager.naturalUnload(level,entity));
        ServerLifecycleEvents.SERVER_STOPPING.register(cn.quit5700.fabrictrain.offline.InfrastructureService::stop);
        ServerLifecycleEvents.SERVER_STOPPED.register(cn.quit5700.fabrictrain.offline.InfrastructureService::clear);
        cn.quit5700.fabrictrain.network.TrainSpeedTelemetry.register();
        ServerTickEvents.END_SERVER_TICK.register(TrainControllerManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.network.TrainSpeedTelemetry::tick);
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.data.DeviceRecoveryService::tick);
        ServerTickEvents.END_SERVER_TICK.register(OfflineTrainService::tick);
        ServerTickEvents.END_SERVER_TICK.register(DepotDispatchService::tick);
        ServerTickEvents.END_SERVER_TICK.register(CargoLedgerService::tick);
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.cargo.DestinationUnloadService::tick);
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.route.StopTrackHighlightService::tick);
        ServerTickEvents.END_SERVER_TICK.register(RailPowerService::tick);
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.redstone.DeviceRedstoneService::tick);
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.offline.InfrastructureService::finish);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level,player,pos,state,be)->{
            if(level instanceof net.minecraft.server.level.ServerLevel serverLevel)cn.quit5700.fabrictrain.offline.InfrastructureService.forget(serverLevel,pos);
        });
        TrainInteractionEvents.register();
        TrainNetworking.registerServer();
        DepotNetworking.registerServer();
        cn.quit5700.fabrictrain.network.FreightNetworking.registerServer();
        ServerTickEvents.END_SERVER_TICK.register(cn.quit5700.fabrictrain.freight.FreightYardService::tick);
        LineSettingsNetworking.registerServer();
        TrainClassNetworking.registerServer();
        CargoSettingsNetworking.registerServer();
        DeviceSettingsNetworking.registerServer();
        RailwayManagerNetworking.registerServer();
        FormationNetworking.registerServer();
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handler.getPlayer().sendSystemMessage(Component.translatable(
                        "message.fabric_train_simulator.settings_shortcut_hint")));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> DepotDispatchService.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> RailPowerService.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> cn.quit5700.fabrictrain.redstone.DeviceRedstoneService.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> cn.quit5700.fabrictrain.network.DepotRemoteAccessSessions.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> cn.quit5700.fabrictrain.data.DeviceRecoveryService.clear());
    }
}

