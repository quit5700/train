package cn.quit5700.fabrictrain.route;

import cn.quit5700.fabrictrain.data.DeviceType;

import java.util.List;
import java.util.function.LongFunction;

public final class LineLoopRules {
    private LineLoopRules() {}

    public static boolean isLoop(List<Long> nodeIds, LongFunction<DeviceType> typeLookup) {
        if (nodeIds.size() < 2 || !nodeIds.get(0).equals(nodeIds.get(nodeIds.size() - 1))) return false;
        DeviceType anchorType = typeLookup.apply(nodeIds.get(0));
        return anchorType == DeviceType.STATION || anchorType == DeviceType.SWITCH || anchorType == DeviceType.FREIGHT_YARD;
    }
}

