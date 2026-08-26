package cn.quit5700.fabrictrain;

import cn.quit5700.fabrictrain.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayers;
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
        EntityRendererRegistry.register(ModEntities.LOCOMOTIVE,
                context -> new TrainVehicleRenderer(context, ModelLayers.MINECART));
        EntityRendererRegistry.register(ModEntities.PASSENGER_CAR,
                context -> new TrainVehicleRenderer(context, ModelLayers.MINECART));
        EntityRendererRegistry.register(ModEntities.CARGO_CAR,
                context -> new TrainVehicleRenderer(context, ModelLayers.MINECART));
        TrainInputController.register();
        DepotSettingsGesture.installClientOpener(pos -> {
            boolean control=InputConstants.isKeyDown(Minecraft.getInstance().getWindow(),InputConstants.KEY_LCONTROL)
                    ||InputConstants.isKeyDown(Minecraft.getInstance().getWindow(),InputConstants.KEY_RCONTROL);
            if (!control) return false;
            ClientPlayNetworking.send(new DepotNetworking.RequestOpenPayload(pos));
            return true;
        });
        ClientPlayNetworking.registerGlobalReceiver(DepotNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                        new DepotSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(DepotNetworking.TrainListPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> {
                    if (!TrainListScreen.refreshActive(payload))
                        Minecraft.getInstance().setScreenAndShow(new TrainListScreen(payload));
                }));
        ClientPlayNetworking.registerGlobalReceiver(LineSettingsNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                        new LineSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(TrainClassNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                        new TrainClassScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(CargoSettingsNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                        new CargoSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(DeviceSettingsNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                        new DeviceSettingsScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(RailwayManagerNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreenAndShow(
                        new RailwayManagerScreen(payload))));
    }
}
