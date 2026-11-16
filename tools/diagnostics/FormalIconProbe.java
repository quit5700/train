package cn.quit5700.trainrelease;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.google.gson.Gson;

/** 正式包版本与定稿图标的原生解码检查；只用于隔离客户端，不进入发行包。 */
public final class FormalIconProbe implements ModInitializer,ClientModInitializer {
 private int ticks;private boolean done;
 public void onInitialize() {}
 public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(done){mc.stop();return;}if(++ticks<60)return;
  Map<String,Object> r=new LinkedHashMap<>();Map<String,Boolean> c=new LinkedHashMap<>();String error="";
  try{
   var mod=FabricLoader.getInstance().getModContainer("fabric_train_simulator").orElseThrow();
   c.put("formal_version",mod.getMetadata().getVersion().getFriendlyString().equals("0.2.42"));
   String icon=mod.getMetadata().getIconPath(128).orElseThrow();var path=mod.findPath(icon).orElseThrow();byte[] bytes=Files.readAllBytes(path);
   String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
   c.put("confirmed_original_icon",hash.equals("2f940150d42540fe5dd88ba7b41b8c08f63f5dc68d985f8e3aaf550e799cef85"));
   System.out.println("[TRAIN_RELEASE] 开始原生PNG解码");System.out.flush();
   try(var in=Files.newInputStream(path);var image=NativeImage.read(in)){c.put("native_png_decode",image.getWidth()==1254&&image.getHeight()==1254);}
   System.out.println("[TRAIN_RELEASE] 原生PNG解码完成");System.out.flush();
   c.put("optional_quick_build_absent",FabricLoader.getInstance().getModContainer("quick_build").isEmpty());
   c.put("optional_jade_absent",FabricLoader.getInstance().getModContainer("jade").isEmpty());
  }catch(Throwable e){error=e.toString();e.printStackTrace();}
  r.put("complete",true);r.put("passed",error.isEmpty()&&c.size()==5&&!c.containsValue(false));r.put("checks",c);r.put("error",error);r.put("ticks",ticks);
  try{Files.writeString(Path.of(System.getProperty("train.release.result")),new Gson().toJson(r),StandardCharsets.UTF_8);}catch(Exception e){throw new RuntimeException(e);}
  done=true;mc.stop();
 });}
}
