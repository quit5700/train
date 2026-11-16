package cn.quit5700.stationaccess.client;
import cn.quit5700.stationaccess.*;
import cn.quit5700.stationaccess.core.Box;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
public final class RangeController {
 public static BlockPos first;private static boolean held;private static Object world;
 public static boolean holding(Minecraft mc){return mc.player!=null&&(mc.player.getMainHandItem().is(StationAccess.SETTER)||mc.player.getOffhandItem().is(StationAccess.SETTER));}
 public static void selection(JsonObject json){first=json.has("first")?at(AccessData.GSON.fromJson(json.get("first"),Box.Cell.class)):null;}
 public static BlockPos at(Box.Cell p){return new BlockPos(p.x(),p.y(),p.z());}
 public static BlockPos target(Minecraft mc){var hit=mc.player.pick(8,0,false);return hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK?block.getBlockPos():BlockPos.containing(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(4)));}
 public static BlockPos breachTarget(Minecraft mc){if(mc.player==null||mc.level==null||!CursorActions.breach(mc.player.isCreative(),holding(mc),mc.player.isShiftKeyDown()))return null;Vec3 eye=mc.player.getEyePosition();var hit=mc.player.pick(5,0,false);Vec3 end=hit.getType()==HitResult.Type.BLOCK?hit.getLocation():eye.add(mc.player.getLookAngle().scale(5));BlockPos best=null;double nearest=Double.MAX_VALUE;String dimension=mc.level.dimension().location().toString();for(var area:ClientAreas.areas)if(area.dimension.equals(dimension))for(var cell:area.box.crossings(AccessService.point(eye),AccessService.point(end)))if(area.closed(cell)){var pos=at(cell);if(!mc.level.getBlockState(pos).isAir()&&!mc.level.getBlockState(pos).is(StationAccess.BOUNDARY))continue;double distance=eye.distanceToSqr(Vec3.atCenterOf(pos));if(distance<nearest){best=pos;nearest=distance;}}return best;}
 public static void register(){RangeRenderer.register();ClientPreAttackCallback.EVENT.register((mc,player,count)->holding(mc));ClientTickEvents.END_CLIENT_TICK.register(mc->{if(world!=mc.level){world=mc.level;first=null;held=false;}boolean down=mc.screen==null&&holding(mc)&&mc.options.keyAttack.isDown();if(down&&!held){boolean breach=CursorActions.breach(mc.player.isCreative(),holding(mc),mc.player.isShiftKeyDown());var p=breach?breachTarget(mc):target(mc);if(p==null){held=down;return;}var o=new JsonObject();o.addProperty("op",breach?"breach":"point");o.addProperty("hand",(mc.player.getMainHandItem().is(StationAccess.SETTER)?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND).name());o.addProperty("x",p.getX());o.addProperty("y",p.getY());o.addProperty("z",p.getZ());ClientPlayNetworking.send(new AccessNetwork.Request(o.toString()));}held=down;});}
}
