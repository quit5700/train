package cn.quit5700.fabrictrain.data;

import cn.quit5700.fabrictrain.train.ControlMode;

import java.util.UUID;

public record TrainRecord(UUID trainId, long number, boolean depotGenerated, long lineId,
                          ControlMode controlMode, TrainPresence presence) {
    public boolean present() { return presence != null && presence.visible(); }
}
