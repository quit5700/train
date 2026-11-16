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
    @Override public void extractRenderState(net.minecraft.world.entity.vehicle.AbstractMinecart car,
            net.minecraft.client.renderer.entity.state.MinecartRenderState state, float partial) {
        super.extractRenderState(car, state, partial);
        if (state.isNewRender || !(car instanceof cn.quit5700.fabrictrain.entity.LocomotiveEntity head)
                || !head.fabricTrain$isLogicallyControlled()
                || !(car.getBehavior() instanceof net.minecraft.world.entity.vehicle.OldMinecartBehavior behavior)) return;
        var position = cn.quit5700.fabrictrain.train.RailInterpolation.renderPosition(car.level(),
                new net.minecraft.world.phys.Vec3(car.xOld, car.yOld, car.zOld), car.position(), partial);
        if (position == null) return;
        var onRail = behavior.getPos(position.x, position.y, position.z);
        if (onRail == null) return;
        state.posOnRail = onRail;
        var front = behavior.getPosOffs(position.x, position.y, position.z, .3D);
        var back = behavior.getPosOffs(position.x, position.y, position.z, -.3D);
        state.frontPos = front == null ? onRail : front;
        state.backPos = back == null ? onRail : back;
    }
}
