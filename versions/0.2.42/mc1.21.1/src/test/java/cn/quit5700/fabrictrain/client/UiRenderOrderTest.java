package cn.quit5700.fabrictrain.client;
import org.junit.jupiter.api.Test;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import static org.junit.jupiter.api.Assertions.*;
class UiRenderOrderTest {
 @Test void allSelfDrawnScreensOverrideTheLateNativeBackdrop() throws Exception {
  for(String name:List.of("CargoSettingsScreen","DepotSettingsScreen","DeviceSettingsScreen","FormationScreen","FreightYardScreen","LineSettingsScreen","RailwayManagerScreen","TrainClassScreen","TrainListScreen")) {
   Class<?> type=Class.forName("cn.quit5700.fabrictrain.client."+name,false,getClass().getClassLoader());
   assertNotEquals(Screen.class,type.getMethod("renderBackground",GuiGraphics.class,int.class,int.class,float.class).getDeclaringClass(),name);
  }
 }
}
