package cn.quit5700.fabrictrain.depot;
import cn.quit5700.fabrictrain.offline.InfrastructureData;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class DispatchAuthoritySavedTest {
 @Test void operatorAndLastObservedModeSurviveReload(){
  var data=new InfrastructureData();var id=UUID.randomUUID();data.dispatchAuthorities.put("11",new InfrastructureData.DispatchAuthority(id,true));
  var tag=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,data).result().orElseThrow();
  var restored=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,tag).result().orElseThrow();
  assertEquals(id,restored.dispatchAuthorities.get("11").player());assertTrue(restored.dispatchAuthorities.get("11").creative());
  restored.dispatchAuthorities.put("11",new InfrastructureData.DispatchAuthority(id,false));
  tag=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,restored).result().orElseThrow();
  assertFalse(InfrastructureData.CODEC.parse(NbtOps.INSTANCE,tag).result().orElseThrow().dispatchAuthorities.get("11").creative());
 }
 @Test void legacySavedDataDoesNotInventCreativeAuthorization(){
  var data=new InfrastructureData();var tag=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,data).result().orElseThrow();
  assertTrue(InfrastructureData.CODEC.parse(NbtOps.INSTANCE,tag).result().orElseThrow().dispatchAuthorities.isEmpty());
 }
 @Test void loweringQuotaRetainsExistingTrainsAndRefillsOnlyBelowLimit(){
  for(long active:new long[]{4,3,2})assertFalse(AutomaticDispatchSchedule.shouldAttempt(true,true,active,2,1000,0,1));
  assertTrue(AutomaticDispatchSchedule.shouldAttempt(true,true,1,2,1000,0,1));
  assertFalse(AutomaticDispatchSchedule.shouldAttempt(true,true,0,0,1000,0,1));
 }
}
