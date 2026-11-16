package cn.quit5700.fabrictrain.depot;
import org.junit.jupiter.api.Test;
import cn.quit5700.fabrictrain.data.RailwaySavedData;
import cn.quit5700.fabrictrain.train.ControlMode;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class AutomaticQuotaTest {
 @Test void onlyAutomaticRecordsUseQuota(){var d=new RailwaySavedData();var a=UUID.randomUUID();var m=UUID.randomUUID();d.ensureTrain(a,true,7,ControlMode.ROUTE_AUTOMATIC);d.ensureTrain(m,true,7,ControlMode.MANUAL);d.registerActiveTrain(a,7);d.registerActiveTrain(m,7);assertEquals(1,d.activeAutomaticTrainCount(7));assertEquals(2,d.activeTrainCount(7));}
 @Test void otherLineDoesNotCount(){var d=new RailwaySavedData();var a=UUID.randomUUID();d.ensureTrain(a,true,7,ControlMode.ROUTE_AUTOMATIC);d.registerActiveTrain(a,7);assertEquals(0,d.activeAutomaticTrainCount(8));}
}
