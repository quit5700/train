package cn.quit5700.stationaccess.client;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Screenshot;
/** Opt-in native UI capture in a disposable development client, never regular play. */
public final class RangeUiPreview {
 private static int ticks;private static RangeListScreen screen;
 public static void register(){if(!Boolean.getBoolean("station_access.uiPreview"))return;ClientTickEvents.END_CLIENT_TICK.register(mc->{
  if(mc.gui.screen()==null)return;
  ticks++;
  if(ticks==80){var state=new JsonObject();state.addProperty("mode","setter");state.addProperty("nonce","preview");state.addProperty("message","");var rows=new JsonArray();for(int i=0;i<7;i++){var row=new JsonObject();row.addProperty("id","preview-"+i);row.addProperty("shown",i%2==0);row.addProperty("color",i+3);row.addProperty("names",i==0?String.join("   ",java.util.Collections.nCopies(3,net.minecraft.network.chat.Component.translatable("block.fabric_train_simulator.station_marker").getString())):i==1?net.minecraft.network.chat.Component.translatable("block.fabric_train_simulator.station_marker").getString():"");rows.add(row);}state.add("rows",rows);screen=new RangeListScreen(state);mc.gui.setScreen(screen);}
  if(ticks==130||ticks==200)Screenshot.grab(mc,false);
  if(ticks==160)try{var field=RangeListScreen.class.getDeclaredField("dropdown");field.setAccessible(true);field.set(screen,"preview-0");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
  if(ticks==240)mc.stop();
 });}
}
