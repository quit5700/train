package cn.quit5700.stationaccess;
import org.junit.jupiter.api.Test;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class HelpCompatTest {
 public static class Good {public static int API_VERSION=1;static List<List<Object>> calls=new ArrayList<>();public static void register(String mod,int order,String id,String icon,String title,List<List<String>> pages){calls.add(List.of(mod,order,id,icon,title,pages));}}
 public static class Old {public static int API_VERSION=2;}
 public static class Broken {public static int API_VERSION=1;public static void register(String a,int b,String c,String d,String e,List<?> f){throw new IllegalStateException("incompatible");}}
 @Test void absentHelpNeverLoadsAnyClass(){assertFalse(HelpCompat.register(false,new ClassLoader(null){public Class<?> loadClass(String name){throw new AssertionError("Must not load absent API");}}));}
 @Test void missingOldApiDoesNotCrash(){assertFalse(HelpCompat.register(true,new ClassLoader(null){}));}
 @Test void unsupportedVersionDoesNotCrash(){assertFalse(HelpCompat.register(Old.class));}
 @Test void brokenApiDoesNotCrash(){assertFalse(HelpCompat.register(Broken.class));}
 @Test void allPublicObjectsAndBoundaryHaveHelp(){Good.calls.clear();assertTrue(HelpCompat.register(Good.class));assertEquals(4,Good.calls.size());for(var call:Good.calls){assertEquals("fabric_train_simulator",call.get(0));assertTrue(call.get(4).toString().startsWith("help.station_access."));assertFalse(((List<?>)call.get(5)).isEmpty());}}
 @Test void actualInstalledApiAcceptsAndDeduplicatesEntries() throws Exception {String path=System.getProperty("station_access.helpApiJar");assertNotNull(path);try(var loader=new URLClassLoader(new java.net.URL[]{Path.of(path).toUri().toURL()},ClassLoader.getPlatformClassLoader())){assertTrue(HelpCompat.register(true,loader));Class<?> api=loader.loadClass("cn.quit5700.quickbuild.api.Q5HelpPage");assertEquals(4,((List<?>)api.getMethod("entries").invoke(null)).size());assertTrue(HelpCompat.register(true,loader));assertEquals(4,((List<?>)api.getMethod("entries").invoke(null)).size());}}
}
