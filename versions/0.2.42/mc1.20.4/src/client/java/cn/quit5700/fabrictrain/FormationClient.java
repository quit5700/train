package cn.quit5700.fabrictrain;

import cn.quit5700.fabrictrain.client.FormationScreen;
import cn.quit5700.fabrictrain.network.FormationNetworking;
import net.fabricmc.api.ClientModInitializer;
import cn.quit5700.fabrictrain.network.LegacyClientPlayNetworking;
import net.minecraft.client.Minecraft;

public final class FormationClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        LegacyClientPlayNetworking.registerGlobalReceiver(FormationNetworking.OpenPayload.ID, (payload, context) ->
                Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new FormationScreen(payload))));
    }
}


