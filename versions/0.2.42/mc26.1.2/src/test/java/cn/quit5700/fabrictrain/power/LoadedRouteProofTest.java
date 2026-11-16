package cn.quit5700.fabrictrain.power;
import cn.quit5700.fabrictrain.route.TrackPoint;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LoadedRouteProofTest {
 static final class View implements LoadedRouteProof.View {
  final Map<Integer,Object> loaded=new HashMap<>();final Set<BlockPos> broken=new HashSet<>();int reads;
  public Object chunk(int x,int z){return loaded.get(x);}
  public boolean signal(Object chunk,BlockPos pos){reads++;return !broken.contains(pos);}
 }
 private LoadedRouteProof route(int size){var path=new ArrayList<TrackPoint>();for(int i=0;i<size;i++)path.add(new TrackPoint(i,64,0));return new LoadedRouteProof(path);}
 @Test void loadedSourceAndDestinationWithUnloadedMiddleUseSavedConnectivity(){var view=new View();view.loaded.put(0,new Object());view.loaded.put(2,new Object());var proof=route(48);assertTrue(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());assertEquals(32,view.reads);}
 @Test void fullyLoadedNetworkMustUsePhysicalPower(){var view=new View();for(int i=0;i<3;i++)view.loaded.put(i,new Object());assertFalse(route(48).inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());}
 @Test void residentBreakStopsBridgeAndRepairNeedsInvalidation(){var view=new View();view.loaded.put(0,new Object());view.loaded.put(2,new Object());var broken=new BlockPos(3,64,0);view.broken.add(broken);var proof=route(48);assertFalse(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());view.broken.clear();assertFalse(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());assertTrue(proof.inspect(view,1,new LoadedRouteProof.Budget(2048)).allowsBridge());}
 @Test void newlyLoadedChunkIsInspectedAndCannotReuseOldUnloadedResult(){var view=new View();view.loaded.put(0,new Object());var proof=route(64);assertTrue(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());view.loaded.put(1,new Object());view.broken.add(new BlockPos(20,64,0));assertFalse(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());}
 @Test void sharedTickBudgetBoundsMultiplePlansAndProgressResumes(){var view=new View();for(int i=0;i<200;i++)view.loaded.put(i,new Object());var a=route(4000);var b=route(4000);var budget=new LoadedRouteProof.Budget(2048);assertFalse(a.inspect(view,0,budget).complete());assertFalse(b.inspect(view,0,budget).complete());assertEquals(2048,view.reads);assertEquals(0,budget.remaining());assertTrue(a.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());assertTrue(b.inspect(view,0,new LoadedRouteProof.Budget(4096)).allowsBridge());int previous=view.reads;assertTrue(a.inspect(view,0,new LoadedRouteProof.Budget(0)).allowsBridge());assertEquals(previous,view.reads);}
 @Test void replacedChunkIdentityCannotReuseCachedProof(){var view=new View();view.loaded.put(0,new Object());var proof=route(32);assertTrue(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());view.loaded.put(0,new Object());view.broken.add(new BlockPos(1,64,0));assertFalse(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());}
 @Test void duplicateRouteCoordinatesAreCheckedOnce(){var view=new View();view.loaded.put(0,new Object());var point=new TrackPoint(0,64,0);var proof=new LoadedRouteProof(List.of(point,point,new TrackPoint(32,64,0)));assertTrue(proof.inspect(view,0,new LoadedRouteProof.Budget(1)).allowsBridge());assertEquals(1,view.reads);}
 @Test void knownBrokenSegmentCannotBecomeValidJustBecauseItsChunkUnloads(){var view=new View();view.loaded.put(0,new Object());view.loaded.put(2,new Object());var point=new BlockPos(3,64,0);view.broken.add(point);var proof=route(48);assertFalse(proof.inspect(view,0,new LoadedRouteProof.Budget(2048)).allowsBridge());view.loaded.remove(0);assertFalse(proof.inspect(view,1,new LoadedRouteProof.Budget(2048)).allowsBridge());view.loaded.put(0,new Object());view.broken.clear();assertTrue(proof.inspect(view,2,new LoadedRouteProof.Budget(2048)).allowsBridge());}
}
