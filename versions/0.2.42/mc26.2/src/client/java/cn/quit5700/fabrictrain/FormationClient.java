package cn.quit5700.fabrictrain;

import cn.quit5700.fabrictrain.client.FormationScreen;
import cn.quit5700.fabrictrain.network.FormationNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class FormationClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FormationNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreenAndShow(new FormationScreen(payload))));
    }
}
