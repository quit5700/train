package cn.quit5700.stationaccess;
import java.util.*;
public final class HelpCompat {
 public static boolean register(){return register(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("quick_build"),HelpCompat.class.getClassLoader());}
 public static boolean register(boolean loaded,ClassLoader loader){if(!loaded)return false;try{return register(Class.forName("cn.quit5700.quickbuild.api.Q5HelpPage",false,loader));}catch(ReflectiveOperationException|LinkageError|SecurityException|IllegalArgumentException failure){return false;}}
 public static boolean register(Class<?> api){try{if(api.getField("API_VERSION").getInt(null)!=1)return false;var method=api.getMethod("register",String.class,int.class,String.class,String.class,String.class,List.class);for(String entry:List.of("setter","ticket","terminal","boundary")){String prefix="help.station_access."+entry;method.invoke(null,"fabric_train_simulator",100,entry,"station_access:"+(entry.equals("boundary")?"setter":entry),prefix+".title",List.of(List.of(prefix+".line1",prefix+".line2",prefix+".line3")));}return true;}catch(ReflectiveOperationException|LinkageError|SecurityException|IllegalArgumentException failure){return false;}}
}
