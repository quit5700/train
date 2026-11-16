package cn.quit5700.fabrictrain.compat;

import cn.quit5700.fabrictrain.offline.InfrastructureData;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.nbt.*;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionalItemStackCodecTest {
    @BeforeAll static void boot(){SharedConstants.tryDetectVersion();Bootstrap.bootStrap();}
    @Test void emptySlotAcceptsOnlyEmptyMap(){
        assertTrue(OptionalItemStackCodec.CODEC.parse(NbtOps.INSTANCE,new CompoundTag()).result().orElseThrow().isEmpty());
        assertTrue(OptionalItemStackCodec.CODEC.parse(NbtOps.INSTANCE,StringTag.valueOf("bad")).error().isPresent());
        CompoundTag invalid=new CompoundTag();invalid.putString("bad","data");
        assertTrue(OptionalItemStackCodec.CODEC.parse(NbtOps.INSTANCE,invalid).error().isPresent());
    }
    @Test void adjacentNonemptySlotsSurviveAnEmptySlotAndDiskRoundTrip(){
        CompoundTag root=new CompoundTag(),stock=new CompoundTag(),pos=new CompoundTag();
        pos.putString("dimension","minecraft:overworld");pos.putInt("x",14);pos.putInt("y",64);pos.putInt("z",18);
        stock.put("position",pos);stock.putString("block","minecraft:chest");stock.putInt("size",3);stock.putInt("limit",64);stock.putBoolean("owned",true);
        ListTag slots=new ListTag();for(int i=0;i<3;i++){CompoundTag slot=new CompoundTag(),stack=new CompoundTag();slot.putInt("slot",i);if(i!=1){stack.putString("id","minecraft:diamond");stack.putByte("Count",(byte)(i==0?64:12));}slot.put("stack",stack);slots.add(slot);}stock.put("slots",slots);ListTag stores=new ListTag();stores.add(stock);root.put("containers",stores);
        var parsed=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,root).result().orElseThrow();assertEquals(1,parsed.stocks.size());
        var values=parsed.stocks.values().iterator().next().slots();assertEquals(3,values.size());assertTrue(values.get(1).stack().isEmpty());assertEquals(76,values.stream().mapToInt(s->s.stack().getCount()).sum());assertTrue(values.get(0).stack().is(Items.DIAMOND));
        var encoded=InfrastructureData.CODEC.encodeStart(NbtOps.INSTANCE,parsed).result().orElseThrow();var again=InfrastructureData.CODEC.parse(NbtOps.INSTANCE,encoded).result().orElseThrow();assertEquals(76,again.stocks.values().iterator().next().slots().stream().mapToInt(s->s.stack().getCount()).sum());
    }
}
