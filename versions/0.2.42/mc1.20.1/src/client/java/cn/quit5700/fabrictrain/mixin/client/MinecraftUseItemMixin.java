package cn.quit5700.fabrictrain.mixin.client;

import cn.quit5700.fabrictrain.registry.ModBlocks;
import cn.quit5700.fabrictrain.train.DepotSettingsGesture;
import cn.quit5700.fabrictrain.network.DeviceSettingsNetworking;
import cn.quit5700.fabrictrain.network.LegacyClientPlayNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 2000)
abstract class MinecraftUseItemMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void fabricTrain$openDepotSettingsBeforeOtherInteractions(CallbackInfo callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)) return;
        boolean control = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isShiftKeyDown();
        if (!control) return;
        var state = minecraft.level.getBlockState(hit.getBlockPos());
        if (state.is(ModBlocks.TRAIN_DEPOT)) {
            if (cn.quit5700.fabrictrain.train.DepotRailPlacement.isTopRail(
                    minecraft.player.getMainHandItem(), hit.getDirection())) return;
            if (DepotSettingsGesture.tryOpen(hit.getBlockPos())) callback.cancel();
            return;
        }
        if (state.is(ModBlocks.STATION_MARKER) || state.is(ModBlocks.SWITCH_RAIL)) {
            LegacyClientPlayNetworking.send(new DeviceSettingsNetworking.RequestOpenPayload(hit.getBlockPos()));
            callback.cancel();
        }
    }
}


