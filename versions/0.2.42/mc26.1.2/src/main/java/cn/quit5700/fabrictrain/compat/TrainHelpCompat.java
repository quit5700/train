package cn.quit5700.fabrictrain.compat;
import java.util.*;
public final class TrainHelpCompat {
 public static final List<String> ITEMS=List.of("signal_rail","switch_rail","power_generator","train_depot","freight_yard","station_marker","locomotive","passenger_car","cargo_car","line_setter","train_connector");
 public static boolean register(){if(!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("quick_build"))return false;try{var api=Class.forName("cn.quit5700.quickbuild.api.Q5HelpPage",false,TrainHelpCompat.class.getClassLoader());return register(api);}catch(ReflectiveOperationException|LinkageError|SecurityException|IllegalArgumentException absent){return false;}}
 public static boolean register(Class<?> api){try{if(api.getField("API_VERSION").getInt(null)!=1)return false;var method=api.getMethod("register",String.class,int.class,String.class,String.class,String.class,List.class);for(String id:ITEMS)method.invoke(null,"fabric_train_simulator",100,id,"fabric_train_simulator:"+id,(id.equals("freight_yard") ? "block.fabric_train_simulator." : "item.fabric_train_simulator.")+id,pages(id));return true;}catch(ReflectiveOperationException|LinkageError|SecurityException|IllegalArgumentException absent){return false;}}

 public static List<List<String>> pages(String id){
  List<String> operations=switch(id){
   case "signal_rail" -> List.of("rail");
   case "switch_rail" -> List.of("rail","settings","lines");
   case "power_generator" -> List.of("generator");
   case "train_depot" -> List.of("depot","manual");
   case "freight_yard" -> List.of("freight_yard");
   case "station_marker" -> List.of("station","settings");
   case "locomotive" -> List.of("manual","locomotive","connector_select","connector_manage");
   case "passenger_car" -> List.of("manual","connector_select","connector_manage");
   case "cargo_car" -> List.of("cargo","connector_select","connector_manage");
   case "line_setter" -> List.of("lines");
   case "train_connector" -> List.of("connector_select","connector_manage");
   default -> List.of();
  };
  var pages=new ArrayList<List<String>>();var first=new ArrayList<String>();first.add("help.fabric_train_simulator."+id);
  if(!operations.isEmpty())first.add("help.fabric_train_simulator.operation."+operations.get(0));pages.add(List.copyOf(first));
  for(int i=1;i<operations.size();i++)pages.add(List.of("help.fabric_train_simulator.operation."+operations.get(i)));
  return List.copyOf(pages);
 }
}
