package cn.quit5700.fabrictrain.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;

/**
 * Keeps vanilla minecart positioning and rail rotation while suppressing only
 * the vanilla minecart shell. The entity display block remains visible.
 */
public final class TrainVehicleRenderer extends MinecartRenderer {
    public TrainVehicleRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, layer);
        model.root().visible = false;
    }
}
