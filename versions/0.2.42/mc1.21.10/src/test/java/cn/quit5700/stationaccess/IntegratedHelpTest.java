package cn.quit5700.stationaccess;
import cn.quit5700.fabrictrain.compat.TrainHelpCompat;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class IntegratedHelpTest {
 @Test void allFourteenItemsAndBoundaryShareOneHelpOwner()throws Exception{try(var loader=new URLClassLoader(new java.net.URL[]{Path.of(System.getProperty("station_access.helpApiJar")).toUri().toURL()},ClassLoader.getPlatformClassLoader())){Class<?> api=loader.loadClass("cn.quit5700.quickbuild.api.Q5HelpPage");assertTrue(HelpCompat.register(api));assertTrue(TrainHelpCompat.register(api));assertEquals(15,((List<?>)api.getMethod("entries").invoke(null)).size());assertTrue(TrainHelpCompat.register(api));assertEquals(15,((List<?>)api.getMethod("entries").invoke(null)).size());}}
 @Test void incompatibleOptionalTrainHelpCannotCrash(){assertFalse(TrainHelpCompat.register(HelpCompatTest.Old.class));assertFalse(TrainHelpCompat.register(HelpCompatTest.Broken.class));}
 @Test void originalTrainHelpIsTranslatedInEightLanguages()throws Exception{for(String lang:ResourcesTest.LANG){var data=ResourcesTest.json("assets/fabric_train_simulator/lang/"+lang+".json");for(String id:TrainHelpCompat.ITEMS)assertFalse(data.get("help.fabric_train_simulator."+id).getAsString().isBlank());}}
}
