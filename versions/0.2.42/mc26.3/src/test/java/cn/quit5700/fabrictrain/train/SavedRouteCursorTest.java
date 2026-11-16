package cn.quit5700.fabrictrain.train;
import cn.quit5700.fabrictrain.route.TrackPoint;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SavedRouteCursorTest {
 private OfflineTrainState state(List<TrackPoint> path,int index,double progress,boolean loop,int loopStart){return new OfflineTrainState(UUID.randomUUID(),1,TrainClass.CARGO,1000,index,progress,3.66,loop,loopStart,1,0,0,0,20,path,List.of());}
 private List<TrackPoint> straight(){var result=new ArrayList<TrackPoint>();for(int i=0;i<=2048;i++)result.add(new TrackPoint(4096,80,4096+i));return result;}
 @Test void restoresExactAmbiguousBlockBoundaryWithoutWorld(){var cursor=RailMotionCursor.restoreRoute(state(straight(),483,.5,false,0));assertEquals(483,cursor.routeIndex());assertEquals(.5,cursor.segmentProgress(),1e-12);assertEquals(4580,cursor.advance(null,0).position().z,1e-12);}
 @Test void preservesCrossChunkProgressWithoutResidentRails(){var cursor=RailMotionCursor.restoreRoute(state(straight(),15,.9,false,0));assertEquals(15,cursor.routeIndex());assertEquals(4111,cursor.currentRail().getZ());assertEquals(4112,cursor.nextRail().getZ());assertEquals(.9,cursor.segmentProgress(),1e-12);}
 @Test void restoresSlopeDistance(){var path=List.of(new TrackPoint(0,80,0),new TrackPoint(0,81,1),new TrackPoint(0,81,2));var cursor=RailMotionCursor.restoreRoute(state(path,0,.7,false,0));assertEquals(.7,cursor.segmentProgress(),1e-12);assertEquals(81,cursor.nextRail().getY());}
 @Test void clampsOnlyInvalidSegmentDistance(){assertEquals(1,RailMotionCursor.restoreRoute(state(straight(),3,9,false,0)).segmentProgress(),1e-12);assertEquals(0,RailMotionCursor.restoreRoute(state(straight(),3,-9,false,0)).segmentProgress(),1e-12);}
 @Test void restoresTerminalWithoutRequiringLoadedWorld(){var path=List.of(new TrackPoint(0,80,0),new TrackPoint(0,80,1));var cursor=RailMotionCursor.restoreRoute(state(path,1,0,false,0));assertEquals(1,cursor.routeIndex());assertEquals(cursor.currentRail(),cursor.nextRail());}
 @Test void restoresLoopClosingSegment(){var path=List.of(new TrackPoint(0,80,0),new TrackPoint(0,80,1),new TrackPoint(1,80,1),new TrackPoint(1,80,0));var cursor=RailMotionCursor.restoreRoute(state(path,3,.2,true,1));assertEquals(3,cursor.routeIndex());assertEquals(1,cursor.nextRail().getZ());assertEquals(.2,cursor.segmentProgress(),1e-12);}
}
