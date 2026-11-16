package cn.quit5700.fabrictrain.client;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.objectweb.asm.*;
import static org.junit.jupiter.api.Assertions.*;

class UiRenderOrderTest {
 @Test void allSelfDrawnScreensOverrideTheLateNativeBackdrop() throws Exception {
  for(String name:List.of("CargoSettingsScreen","DepotSettingsScreen","DeviceSettingsScreen","FormationScreen","FreightYardScreen","LineSettingsScreen","RailwayManagerScreen","TrainClassScreen","TrainListScreen")) {
   String resource="cn/quit5700/fabrictrain/client/"+name+".class";
   String baseline=System.getProperty("q5.ui.baselineClasses","");
   AtomicBoolean override=new AtomicBoolean(),nativeBackdrop=new AtomicBoolean();
   try(InputStream input=baseline.isEmpty()?getClass().getClassLoader().getResourceAsStream(resource):Files.newInputStream(Path.of(baseline,resource))) {
    assertNotNull(input,name);
    new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
     @Override public MethodVisitor visitMethod(int access,String method,String descriptor,String signature,String[] exceptions) {
      if(!method.equals("renderBackground")||!descriptor.equals("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))return null;
      override.set(true);
      return new MethodVisitor(Opcodes.ASM9) {
       @Override public void visitMethodInsn(int opcode,String owner,String target,String desc,boolean itf) {
        if(owner.equals("net/minecraft/client/gui/screens/Screen")&&target.equals("renderBackground"))nativeBackdrop.set(true);
       }
      };
     }
    },0);
   }
   assertTrue(override.get(),name+" must prevent Screen.render from appending the native backdrop");
   assertFalse(nativeBackdrop.get(),name+" must not cover its already drawn content");
  }
 }
}
