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
public final class RangeListScreen extends Screen {
 private static final net.minecraft.resources.ResourceLocation UI_FONT = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("station_access","range_ui");
 private static Component ui(Component text){return text.copy().withStyle(style->style.withFont(UI_FONT));}
 private JsonObject state;private final List<Hit> hits=new ArrayList<>();private int x,y,mx,my;private float scale=1;private int page;private String dropdown="";
 private record Hit(int x,int y,int w,int h,Runnable action){boolean has(double a,double b){return a>=x&&a<x+w&&b>=y&&b<y+h;}}
 public RangeListScreen(JsonObject snapshot){super(t("title"));state=snapshot;}
 public String nonce(){return state.get("nonce").getAsString();}public void update(JsonObject snapshot){state=snapshot;}
 private static Component t(String key,Object... args){return ui(Component.translatable("ui.station_access."+key,args));}
 @Override public boolean isPauseScreen(){return false;}
 @Override protected void init(){scale=Math.min(1f,Math.min(width/650f,height/365f));x=(int)(width/scale-630)/2;y=(int)(height/scale-345)/2;}
 private void request(String op,int slot){var o=new JsonObject();o.addProperty("nonce",nonce());o.addProperty("op",op);o.addProperty("slot",slot);ClientPlayNetworking.send(new AccessNetwork.Request(o.toString()));}
 private void action(String op,String area,Object value){var o=new JsonObject();o.addProperty("nonce",nonce());o.addProperty("op",op);o.addProperty("area",area);if(value instanceof Boolean b)o.addProperty("value",b);else if(value instanceof Number n)o.addProperty("value",n);ClientPlayNetworking.send(new AccessNetwork.Request(o.toString()));}
 private void control(GuiGraphics g,String label,int px,int py,int w,Runnable action){g.fill(px,py,px+w,py+23,0xff252525);g.fill(px+1,py+1,px+w-1,py+22,0xffd0d0d0);g.drawString(font,ui(Component.literal(label)),px+(w-font.width(ui(Component.literal(label))))/2,py+8,0xff171717,false);hits.add(new Hit(px,py,w,23,action));}
 private void stationNames(GuiGraphics g,Component label,int px,int py){var wrapped=font.split(ui(label),330);float fit=Math.min(1f,38f/Math.max(1,wrapped.size()*font.lineHeight));g.pose().pushPose();g.pose().scale(fit,fit,1);int row=0;for(var line:wrapped)g.drawString(font,line,Math.round(px/fit),Math.round(py/fit)+row++*font.lineHeight,0xff171717,false);g.pose().popPose();}
 private List<Hit> reverseHits(){var result=new ArrayList<>(hits);Collections.reverse(result);return result;}
 @Override public void onClose(){request("close",-1);super.onClose();}
 private static void outline(GuiGraphics g,int x,int y,int w,int h,int color){g.hLine(x,x+w-1,y,color);g.hLine(x,x+w-1,y+h-1,color);g.vLine(x,y,y+h-1,color);g.vLine(x+w-1,y,y+h-1,color);}
 private void text(GuiGraphics g,Component text,int px,int py){g.drawString(font,ui(text),px,py,0xff171717,false);}
 private void lines(GuiGraphics g,Component text,int px,int py,int width){int row=0;for(var line:font.split(ui(text),width)){g.drawString(font,line,px,py+row++*font.lineHeight,0xff171717,false);}}
 private void button(GuiGraphics g,String key,int px,int py,int w,String op){g.fill(px,py,px+w,py+23,0xff252525);g.fill(px+1,py+1,px+w-1,py+22,0xffc6c6c6);g.hLine(px+1,px+w-2,py+1,0xffeeeeee);g.vLine(px+1,py+1,py+21,0xffeeeeee);g.hLine(px+1,px+w-2,py+21,0xff555555);g.vLine(px+w-2,py+1,py+21,0xff555555);var label=t(key);int row=0;var ll=font.split(label,w-8);for(var line:ll)g.drawString(font,line,px+(w-font.width(line))/2,py+(23-ll.size()*font.lineHeight)/2+row++*font.lineHeight,0xff171717,false);hits.add(new Hit(px,py,w,23,()->{if(op.equals("close"))onClose();else request(op,-1);}));}
 @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta){
  g.pose().pushPose();g.pose().scale(scale,scale,1);mx=Math.round(mouseX/scale);my=Math.round(mouseY/scale);hits.clear();g.fill(0,0,(int)(width/scale),(int)(height/scale),0x88000000);g.fill(x,y,x+630,y+345,0xffc6c6c6);outline(g,x,y,630,345,0xff252525);g.hLine(x+1,x+628,y+1,0xffeeeeee);g.vLine(x+1,y+1,y+343,0xffeeeeee);
  text(g,t("ranges"),x+12,y+12);
  var rows=state.getAsJsonArray("rows");boolean all=rows.size()>0;for(var r:rows)all&=r.getAsJsonObject().get("shown").getAsBoolean();final boolean desired=!all;
  control(g,all?"☑":"☐",x+10,y+32,22,()->action("all_visible","",desired));text(g,t("all"),x+38,y+40);text(g,t("stations"),x+80,y+40);
  int total=Math.max(1,(rows.size()+3)/4);page=Math.max(0,Math.min(page,total-1));
  for(int i=page*4;i<Math.min(rows.size(),page*4+4);i++){var r=rows.get(i).getAsJsonObject();String id=r.get("id").getAsString();int py=y+62+(i-page*4)*54;g.fill(x+8,py,x+622,py+50,0xffb3b3b3);boolean show=r.get("shown").getAsBoolean();control(g,show?"☑":"☐",x+10,py+10,22,()->action("visible",id,!show));String names=r.get("names").getAsString();stationNames(g,names.isEmpty()?t("no_station"):Component.literal(names),x+42,py+8);
   int color=Math.floorMod(r.get("color").getAsInt(),16);g.fill(x+381,py+14,x+394,py+27,0xff000000|RangeRenderer.COLORS[color]);control(g,"▼",x+398,py+10,25,()->dropdown=dropdown.equals(id)?"":id);control(g,t("reselect").getString(),x+436,py+10,78,()->action("reselect",id,0));control(g,t("delete").getString(),x+525,py+10,86,()->action("delete",id,0));
  }
  text(g,t("page",page+1,total),x+270,y+285);control(g,"◀",x+220,y+278,35,()->{page=Math.max(0,page-1);dropdown="";});control(g,"▶",x+376,y+278,35,()->{page=Math.min(total-1,page+1);dropdown="";});
  if(!dropdown.isEmpty()){String id=dropdown;g.fill(x+380,y+58,x+434,y+278,0xff252525);for(int c=0;c<16;c++){final int color=c;int cx=x+383+(c%2)*24,cy=y+61+(c/2)*26;g.fill(cx,cy,cx+20,cy+20,0xff000000|RangeRenderer.COLORS[c]);hits.add(new Hit(cx,cy,20,20,()->{action("color",id,color);dropdown="";}));}}
  if(state.has("message")&&!state.get("message").getAsString().isEmpty())lines(g,Component.translatable("message.station_access."+state.get("message").getAsString()),x+12,y+308,430);

  button(g,"close",x+515,y+310,100,"close");g.pose().popPose();
 }
 @Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(button==0){double a=mouseX/scale,b=mouseY/scale;for(Hit h:reverseHits())if(h.has(a,b)){h.action.run();return true;}}return super.mouseClicked(mouseX,mouseY,button);}
}
