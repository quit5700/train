package cn.quit5700.fabrictrain.train;

import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import java.util.UUID;

public final class TrainDiagnosticLabel {
    private TrainDiagnosticLabel() {}

    public static Component of(ServerLevel level, TrainLink link) {
        var record=RailwayDataManager.state(level).ensureTrain(link.trainId(),link.depotGenerated(),
                link.lineId(),link.controlMode());
        String key=record.controlMode()==ControlMode.MANUAL
                ?"message.fabric_train_simulator.train.label.manual"
                :"message.fabric_train_simulator.train.label.route_automatic";
        return Component.translatable(key,record.number(),record.trainId().toString().substring(0,8));
    }

    public static Component of(ServerLevel level, UUID trainId) {
        var data=RailwayDataManager.state(level);
        var record=data.trainRecord(trainId);
        if(record==null){
            for(var entity:level.getAllEntities())if(entity instanceof LinkedTrainCar linked
                    &&linked.trainLink().trainId().equals(trainId))return of(level,linked.trainLink());
            record=data.ensureTrain(trainId,false,0L,ControlMode.MANUAL);
        }
        String key=record.controlMode()==ControlMode.MANUAL
                ?"message.fabric_train_simulator.train.label.manual"
                :"message.fabric_train_simulator.train.label.route_automatic";
        return Component.translatable(key,record.number(),record.trainId().toString().substring(0,8));
    }
}
