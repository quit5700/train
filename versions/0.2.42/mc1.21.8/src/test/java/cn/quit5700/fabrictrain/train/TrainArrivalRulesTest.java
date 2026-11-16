package cn.quit5700.fabrictrain.train;
import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.route.TrackPoint;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class TrainArrivalRulesTest {
 private final RailwayDevice end=new RailwayDevice(2,DeviceType.DEPOT,"minecraft:overworld",0,79,20,"terminal",0,0);
 private final RailwayDevice station=new RailwayDevice(1,DeviceType.STATION,"minecraft:overworld",0,79,10,"station",0,0);
 private final RailwayLine line=new RailwayLine(1,"line",3,List.of(1L,2L),1,1000,2,LineStatus.RUNNING,Map.of(),Map.of(0,new StopTrack(0,80,10)));
 private OfflineTrainState state(int at,int node,long last,boolean loop,List<TrackPoint> path){return new OfflineTrainState(UUID.randomUUID(),1,TrainClass.CARGO,1000,at,0D,0D,loop,0,node,last,0,0,20,path,List.of());}
 private List<TrackPoint> path(){var list=new ArrayList<TrackPoint>();for(int i=0;i<=20;i++)list.add(new TrackPoint(0,80,i));return list;}
 @Test void cannotRecoverAtStartEvenWithTerminalMarked(){assertFalse(TrainArrivalRules.canRecover(state(0,1,2,false,path()),line,end));}
 @Test void actualFinalNodeCanRecover(){assertTrue(TrainArrivalRules.canRecover(state(20,1,2,false,path()),line,end));}
 @Test void oldPathEndIsNotArrivalAtRelocatedDepot(){assertFalse(TrainArrivalRules.canRecover(state(10,1,2,false,path().subList(0,11)),line,end));}
 @Test void wrongNodeOrLoopCannotRecover(){assertFalse(TrainArrivalRules.canRecover(state(20,0,2,false,path()),line,end));assertFalse(TrainArrivalRules.canRecover(state(20,1,1,false,path()),line,end));assertFalse(TrainArrivalRules.canRecover(state(20,1,2,true,path()),line,end));}
 @Test void unloadingNeedsExactStationRouteEvent(){assertTrue(TrainArrivalRules.atNode(state(10,0,1,false,path()),line,station));assertFalse(TrainArrivalRules.atNode(state(9,0,1,false,path()),line,station));assertFalse(TrainArrivalRules.atNode(state(10,1,1,false,path()),line,station));}
 @Test void realAccelerationRetainsLowSpeedAtHandoff(){var speed=new LogicalSpeedState();speed.restore(0D);double distance=0D;for(int tick=0;tick<20;tick++)distance+=speed.approach(SpeedRules.blocksPerTick(1000D),0.015D);assertEquals(3.15D,distance,0.000001D);var resumed=new LogicalSpeedState();resumed.restore(speed.speed());assertEquals(0.315D,resumed.approach(SpeedRules.blocksPerTick(1000D),0.015D),0.000001D);}
}
