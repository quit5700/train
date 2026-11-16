package cn.quit5700.fabrictrain.client;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.network.TrainSpeedTelemetry;
import cn.quit5700.fabrictrain.train.TrainSpeedPresentation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class TrainSpeedHud {
    private static TrainSpeedTelemetry.Sample latest;

    private TrainSpeedHud() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TrainSpeedTelemetry.Sample.ID, (sample, context) ->
                Minecraft.getInstance().execute(() -> latest = sample));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> latest = null);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR,
                FabricTrainMod.id("train_speed"), (graphics, ticks) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft client = Minecraft.getInstance();
        var vehicle = client.player == null ? null : client.player.getVehicle();
        if (vehicle == null) {
            latest = null;
            return;
        }
        if (latest == null || !vehicle.getUUID().equals(latest.vehicle())) return;
        var text = Component.translatable("hud.fabric_train_simulator.actual_speed",
                TrainSpeedPresentation.format(latest.speed()));
        int y = client.getWindow().getGuiScaledHeight() - 51;
        graphics.fill(4, y - 3, 12 + client.font.width(text), y + 12, 0xA020232A);
        graphics.text(client.font, text, 8, y, 0xFFFFFFFF);
    }
}
