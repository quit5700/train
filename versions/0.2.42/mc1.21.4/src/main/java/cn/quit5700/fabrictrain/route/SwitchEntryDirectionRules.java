package cn.quit5700.fabrictrain.route;

import net.minecraft.core.Direction;

public final class SwitchEntryDirectionRules {
    private SwitchEntryDirectionRules() {}

    public static Direction resolve(int savedOrdinal, Direction placementEntry) {
        if (placementEntry == null || placementEntry.getAxis() == Direction.Axis.Y) placementEntry = Direction.NORTH;
        if (savedOrdinal >= 0 && savedOrdinal < Direction.values().length) {
            Direction saved = Direction.values()[savedOrdinal];
            if (saved.getAxis() != Direction.Axis.Y) return saved;
        }
        return placementEntry;
    }
}
