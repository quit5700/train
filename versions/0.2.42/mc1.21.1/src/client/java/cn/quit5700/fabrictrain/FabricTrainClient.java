package cn.quit5700.fabrictrain;

import cn.quit5700.fabrictrain.registry.ModEntities;
import cn.quit5700.fabrictrain.registry.ModBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import cn.quit5700.fabrictrain.client.TrainVehicleRenderer;
import cn.quit5700.fabrictrain.client.TrainInputController;
import cn.quit5700.fabrictrain.client.DepotSettingsScreen;
import cn.quit5700.fabrictrain.client.TrainListScreen;
import cn.quit5700.fabrictrain.network.DepotNetworking;
import cn.quit5700.fabrictrain.network.LineSettingsNetworking;
import cn.quit5700.fabrictrain.client.LineSettingsScreen;
import cn.quit5700.fabrictrain.client.TrainClassScreen;
import cn.quit5700.fabrictrain.network.TrainClassNetworking;
import cn.quit5700.fabrictrain.network.CargoSettingsNetworking;
import cn.quit5700.fabrictrain.client.CargoSettingsScreen;
import cn.quit5700.fabrictrain.network.DeviceSettingsNetworking;
import cn.quit5700.fabrictrain.client.DeviceSettingsScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import cn.quit5700.fabrictrain.network.RailwayManagerNetworking;
import cn.quit5700.fabrictrain.client.RailwayManagerScreen;
import com.mojang.blaze3d.platform.InputConstants;
import cn.quit5700.fabrictrain.train.DepotSettingsGesture;

public final class FabricTrainClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, view, width, height) -> {
            if (view instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen chest
                    && !(view instanceof cn.quit5700.fabrictrain.client.GeneratorScreen)
                    && client.player != null
                    && cn.quit5700.fabrictrain.client.GeneratorScreen.isGenerator(view.getTitle()))
                client.setScreen(new cn.quit5700.fabrictrain.client.GeneratorScreen(
                    chest.getMenu(), client.player.getInventory(), view.getTitle()));
        });
        cn.quit5700.fabrictrain.client.TrainSpeedHud.register();
        ClientPlayNetworking.registerGlobalReceiver(cn.quit5700.fabrictrain.network.FreightNetworking.Snapshot.ID, (payload, context) ->
            Minecraft.getInstance().execute(() -> {
                var snapshot = com.google.gson.JsonParser.parseString(payload.json()).getAsJsonObject();
                var client = Minecraft.getInstance();
                if (snapshot.get("kind").getAsString().equals("stations")) {
                    if (client.screen instanceof DepotSettingsScreen depot) depot.acceptFreightStations(snapshot);
                } else if (snapshot.get("open").getAsBoolean()) {
                    client.setScreen(new cn.quit5700.fabrictrain.client.FreightYardScreen(snapshot));
                } else if (client.screen instanceof cn.quit5700.fabrictrain.client.FreightYardScreen yard) {
                    yard.accept(snapshot);
                }
            }));
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, view, width, height) -> {
            if (!(view instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
                    || client.player == null
                    || !(client.player.getVehicle() instanceof cn.quit5700.fabrictrain.entity.LocomotiveEntity head)
                    || !client.player.level().getBlockState(cn.quit5700.fabrictrain.compat.MinecartCompat.currentRailPos(head).below())
                        .is(cn.quit5700.fabrictrain.registry.ModBlocks.FREIGHT_YARD)) return;
            net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(view).add(
                net.minecraft.client.gui.components.Button.builder(
                    cn.quit5700.fabrictrain.client.FreightYardScreen.tr("load"), button ->
                        ClientPlayNetworking.send(new cn.quit5700.fabrictrain.network.FreightNetworking.Request(
                            "{\"action\":\"manual_load\"}")))
                    .bounds(Math.max(2, width / 2 - 42), Math.max(2, height / 2 - 108), 84, 20).build());
        });
        new cn.quit5700.stationaccess.client.AccessClient().onInitializeClient();
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(),
                ModBlocks.SIGNAL_RAIL, ModBlocks.SWITCH_RAIL);
        EntityRendererRegistry.register(ModEntities.LOCOMOTIVE,
                context -> new TrainVehicleRenderer(context, ModelLayers.MINECART));
        EntityRendererRegistry.register(ModEntities.PASSENGER_CAR,
                context -> new TrainVehicleRenderer(context, ModelLayers.MINECART));
        EntityRendererRegistry.register(ModEntities.CARGO_CAR,
                context -> new TrainVehicleRenderer(context, ModelLayers.MINECART));
        TrainInputController.register();
        DepotSettingsGesture.installClientOpener(pos -> {
            boolean control = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isShiftKeyDown();
            if (!control) return false;
            ClientPlayNetworking.send(new DepotNetworking.RequestOpenPayload(pos));
            return true;
        });
        ClientPlayNetworking.registerGlobalReceiver(DepotNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                        new DepotSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(DepotNetworking.TrainListPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> {
                    if (!TrainListScreen.refreshActive(payload))
                        Minecraft.getInstance().setScreen(new TrainListScreen(payload));
                }));
        ClientPlayNetworking.registerGlobalReceiver(LineSettingsNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                        new LineSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(TrainClassNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> {
                    if (payload.entityId() == TrainClassNetworking.GLOBAL_SPEED_SETTINGS) {
                        var screen = Minecraft.getInstance().screen;
                        if (screen instanceof DepotSettingsScreen depot) depot.refreshSpeeds(payload);
                        else if (screen instanceof TrainClassScreen train) train.refreshSpeeds(payload);
                        return;
                    }
                    if(payload.refresh()){if(Minecraft.getInstance().screen instanceof TrainClassScreen active)active.refreshOptions(payload);}else Minecraft.getInstance().setScreen(
                        new TrainClassScreen(payload)); }));
        ClientPlayNetworking.registerGlobalReceiver(CargoSettingsNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> { if(payload.refresh()){if(Minecraft.getInstance().screen instanceof CargoSettingsScreen active)active.refreshOptions(payload);}else Minecraft.getInstance().setScreen(
                        new CargoSettingsScreen(payload)); }));
        ClientPlayNetworking.registerGlobalReceiver(DeviceSettingsNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                        new DeviceSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(RailwayManagerNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(
                        new RailwayManagerScreen(payload))));
    }
}
