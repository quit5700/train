package cn.quit5700.stationaccess.client;
import cn.quit5700.stationaccess.*;
import cn.quit5700.stationaccess.core.Box;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class AccessScreen extends Screen {
 private JsonObject state;private final List<Hit> hits=new ArrayList<>();private int x,y,mx,my;private float scale=1;
 private record Hit(int x,int y,int w,int h,Runnable action){boolean has(double a,double b){return a>=x&&a<x+w&&b>=y&&b<y+h;}}
 public AccessScreen(JsonObject snapshot){super(t("title"));state=snapshot;}
 public String nonce(){return state.get("nonce").getAsString();}public void update(JsonObject snapshot){state=snapshot;}
 private static Component t(String key,Object... args){return Component.translatable("ui.station_access."+key,args);}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){scale=Math.min(1f,Math.min(width/350f,height/325f));x=(int)(width/scale-330)/2;y=(int)(height/scale-305)/2;}
 private void request(String op,int slot){var o=new JsonObject();o.addProperty("nonce",nonce());o.addProperty("op",op);o.addProperty("slot",slot);AccessClient.sendRequest(o.toString());}
 @Override public void onClose(){request("close",-1);super.onClose();}
 private static void outline(GuiGraphics g,int x,int y,int w,int h,int color){g.hLine(x,x+w-1,y,color);g.hLine(x,x+w-1,y+h-1,color);g.vLine(x,y,y+h-1,color);g.vLine(x+w-1,y,y+h-1,color);}
 private void text(GuiGraphics g,Component text,int px,int py){g.drawString(font,text,px,py,0xff171717,false);}
 private void lines(GuiGraphics g,Component text,int px,int py,int width){int row=0;for(var line:font.split(text,width)){g.drawString(font,line,px,py+row++*font.lineHeight,0xff171717,false);}}
 private void button(GuiGraphics g,String key,int px,int py,int w,String op){g.fill(px,py,px+w,py+23,0xff252525);g.fill(px+1,py+1,px+w-1,py+22,0xffc6c6c6);g.hLine(px+1,px+w-2,py+1,0xffeeeeee);g.vLine(px+1,py+1,py+21,0xffeeeeee);g.hLine(px+1,px+w-2,py+21,0xff555555);g.vLine(px+w-2,py+1,py+21,0xff555555);var label=t(key);int row=0;var ll=font.split(label,w-8);for(var line:ll)g.drawString(font,line,px+(w-font.width(line))/2,py+(23-ll.size()*font.lineHeight)/2+row++*font.lineHeight,0xff171717,false);hits.add(new Hit(px,py,w,23,()->{if(op.equals("close"))onClose();else request(op,-1);}));}
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
  g.pose().pushPose();g.pose().scale(scale,scale,1);mx=Math.round(mouseX/scale);my=Math.round(mouseY/scale);hits.clear();g.fill(0,0,(int)(width/scale),(int)(height/scale),0x88000000);g.fill(x,y,x+330,y+305,0xffc6c6c6);outline(g,x,y,330,305,0xff252525);g.hLine(x+1,x+328,y+1,0xffeeeeee);g.vLine(x+1,y+1,y+303,0xffeeeeee);
  boolean terminal=state.get("mode").getAsString().equals("terminal");text(g,t(terminal?"terminal":"title"),x+12,y+12);
  if(terminal){lines(g,t("recharge_rule"),x+12,y+34,306);text(g,t("balance",state.get("balance").getAsInt()),x+12,y+63);button(g,"charge",x+177,y+58,140,"charge");lines(g,t("choose_ticket"),x+12,y+89,306);}
  else {if(state.has("box")){Box b=AccessData.GSON.fromJson(state.get("box"),Box.class);lines(g,t("box",b.width(),b.height(),b.depth()),x+12,y+34,306);if(b.large())lines(g,t("large"),x+12,y+51,306);}else lines(g,t("select_hint"),x+12,y+34,306);text(g,t("durability",state.get("durability").getAsInt()),x+12,y+75);button(g,"create",x+12,y+88,145,"create");button(g,"clear",x+173,y+88,145,"clear");if(state.get("editing").getAsBoolean()){button(g,"reselect",x+12,y+116,145,"reselect");button(g,"delete",x+173,y+116,145,"delete");}}
  if(state.has("message")&&!state.get("message").getAsString().isEmpty())lines(g,Component.translatable("message.station_access."+state.get("message").getAsString()),x+12,y+145,306);
  var inventory=state.getAsJsonArray("inventory");for(int i=0;i<36;i++){int row=i<9?3:(i-9)/9, col=i<9?i:(i-9)%9;int px=x+84+col*18,py=y+183+row*18;g.fill(px,py,px+18,py+18,0xff373737);g.fill(px+1,py+1,px+17,py+17,0xff8b8b8b);g.hLine(px+1,px+16,py+16,0xffeeeeee);g.vLine(px+16,py+1,py+16,0xffeeeeee);ItemStack item=AccessClient.stack(inventory.get(i).getAsString());if(!item.isEmpty()){g.renderItem(item,px+1,py+1);g.renderItemDecorations(font,item,px+1,py+1);}if(terminal){int slot=i;hits.add(new Hit(px,py,18,18,()->request("select",slot)));if(state.get("selected").getAsInt()==i)outline(g,px,py,18,18,0xff315e92);}if(mx>=px&&mx<px+18&&my>=py&&my<py+18&&!item.isEmpty())g.renderTooltip(font,item,mx,my);}
  button(g,"close",x+115,y+272,100,"close");g.pose().popPose();
 }
 @Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(button==0){double a=mouseX/scale,b=mouseY/scale;for(Hit h:List.copyOf(hits))if(h.has(a,b)){h.action.run();return true;}}return super.mouseClicked(mouseX,mouseY,button);}
}
