package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.*;
import cn.quit5700.fabrictrain.route.TrackPoint;

/** Arrival is a route event at a verified coordinate, never just an expired estimate. */
public final class TrainArrivalRules {
    private static final double EPSILON=0.00001D;
    private TrainArrivalRules() {}

    public static boolean atNode(OfflineTrainState state,RailwayLine line,RailwayDevice device) {
        if(state==null||line==null||device==null||line.id()!=state.lineId()||state.path().size()<2
                ||state.nextNodeIndex()<0||state.nextNodeIndex()>=line.nodeIds().size()
                ||line.nodeIds().get(state.nextNodeIndex())!=device.id()||state.lastDeviceId()!=device.id())return false;
        StopTrack stop=line.stationStopTrack(state.nextNodeIndex());
        TrackPoint expected=stop==null?new TrackPoint(device.x(),device.y()+
                (device.type()==DeviceType.DEPOT||device.type()==DeviceType.FREIGHT_YARD?1:0),device.z()):
                new TrackPoint(stop.x(),stop.y(),stop.z());
        return atCoordinate(state,expected);
    }

    public static boolean canRecover(OfflineTrainState state,RailwayLine line,RailwayDevice device) {
        if(device==null||device.type()!=DeviceType.DEPOT||state.loop()||!atNode(state,line,device)
                ||state.nextNodeIndex()!=line.nodeIds().size()-1)return false;
        TrackPoint end=state.path().get(state.path().size()-1);
        return end.x()==device.x()&&end.y()==device.y()+1&&end.z()==device.z();
    }

    private static boolean atCoordinate(OfflineTrainState state,TrackPoint target) {
        int next=OfflineRouteMath.nextIndex(state.path(),state.loop(),state.loopStartIndex(),state.pathIndex());
        double length=next<0?0D:OfflineRouteMath.distance(state.path().get(state.pathIndex()),state.path().get(next));
        if(!Double.isFinite(state.segmentProgress())||state.segmentProgress()<-EPSILON||state.segmentProgress()>length+EPSILON)return false;
        var actual=OfflineRouteMath.pose(state.path(),state.loop(),state.loopStartIndex(),state.pathIndex(),state.segmentProgress()).position();
        return Math.abs(actual.x-(target.x()+0.5D))<EPSILON&&Math.abs(actual.y-(target.y()+0.1D))<EPSILON&&Math.abs(actual.z-(target.z()+0.5D))<EPSILON;
    }
}
