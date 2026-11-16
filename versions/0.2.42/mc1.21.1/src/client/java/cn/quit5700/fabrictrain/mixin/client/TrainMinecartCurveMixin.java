package cn.quit5700.fabrictrain.mixin.client;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.train.RailInterpolation;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecartRenderer.class)
abstract class TrainMinecartCurveMixin {
 @Unique private Vec3 fabricTrain$railRenderPosition;
 @Inject(method="render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
 private void fabricTrain$prepareCurve(AbstractMinecart car,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light,CallbackInfo ci){
  fabricTrain$railRenderPosition=car instanceof LocomotiveEntity head&&head.fabricTrain$isLogicallyControlled()
    ?RailInterpolation.renderPosition(car.level(),new Vec3(car.xOld,car.yOld,car.zOld),car.position(),partial):null;
 }
 @Redirect(method="render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/vehicle/AbstractMinecart;getPos(DDD)Lnet/minecraft/world/phys/Vec3;"))
 private Vec3 fabricTrain$curvePosition(AbstractMinecart car,double x,double y,double z){
  Vec3 p=fabricTrain$railRenderPosition;return p==null?car.getPos(x,y,z):car.getPos(p.x,p.y,p.z);
 }
 @Redirect(method="render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/vehicle/AbstractMinecart;getPosOffs(DDDD)Lnet/minecraft/world/phys/Vec3;"))
 private Vec3 fabricTrain$curveOffset(AbstractMinecart car,double x,double y,double z,double offset){
  Vec3 p=fabricTrain$railRenderPosition;return p==null?car.getPosOffs(x,y,z,offset):car.getPosOffs(p.x,p.y,p.z,offset);
 }
}
