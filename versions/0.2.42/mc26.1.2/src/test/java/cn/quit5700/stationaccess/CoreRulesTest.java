package cn.quit5700.stationaccess;

import cn.quit5700.stationaccess.core.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class CoreRulesTest {
    @Test void reversedCornersMatch(){assertEquals(new Box(-2,0,3,6,8,10),Box.between(new Box.Cell(6,8,3),new Box.Cell(-2,0,10)));}
    @Test void existingInteriorIsNotBoundary(){Box b=new Box(0,0,0,6,6,6);assertFalse(b.shell(new Box.Cell(2,2,2)));assertTrue(b.shell(new Box.Cell(0,2,2)));}
    @Test void crossingFindsBothWallsEvenWhenEndOutside(){var hits=new Box(0,0,0,6,6,6).crossings(new Box.Point(-10,3,3),new Box.Point(20,3,3));assertEquals(List.of(new Box.Cell(0,3,3),new Box.Cell(6,3,3)),hits);}
    @Test void movingInsideDoesNotCrossAgain(){assertTrue(new Box(0,0,0,6,6,6).crossings(new Box.Point(2,3,3),new Box.Point(4,3,3)).isEmpty());}
    @Test void tangentMotionDoesNotCount(){assertTrue(new Box(0,0,0,6,6,6).crossings(new Box.Point(-1,7,3),new Box.Point(8,7,3)).isEmpty());}
    @Test void worldScaleAreaDoesNotAllocateWorldScaleSurface(){assertTimeoutPreemptively(Duration.ofSeconds(1),()->{Box b=new Box(-29999984,-64,-29999984,29999983,319,29999983);assertTrue(b.valid());assertTrue(b.large());var patches=SurfacePatches.forChunk(b,0,0);assertEquals(2,patches.size());assertEquals(512,patches.stream().mapToLong(SurfacePatches.Patch::size).sum());});}
    @Test void sizesAboveFormerLimitRemainValid(){assertTrue(new Box(0,0,0,1023,100,2047).valid());}
    @Test void tooThinVolumeCannotBeHollow(){assertFalse(new Box(0,0,0,1,10,10).valid());}
    @Test void chunkPatchesHaveNoMissingOrDuplicateCells(){for(int seed=0;seed<12;seed++){var r=new Random(seed);Box b=new Box(-18,0,-17,r.nextInt(15)+3,r.nextInt(9)+3,r.nextInt(15)+3);var expected=new HashSet<Box.Cell>();for(int x=b.x0();x<=b.x1();x++)for(int y=b.y0();y<=b.y1();y++)for(int z=b.z0();z<=b.z1();z++){var c=new Box.Cell(x,y,z);if(b.shell(c))expected.add(c);}var actual=new HashSet<Box.Cell>();for(int cx=-2;cx<=1;cx++)for(int cz=-2;cz<=1;cz++)for(var patch:SurfacePatches.forChunk(b,cx,cz))for(long i=0;i<patch.size();i++)assertTrue(actual.add(patch.at(i)),"Duplicate surface cell");assertEquals(expected,actual);}}
    @Test void distantChunksAreEmpty(){assertTrue(SurfacePatches.forChunk(new Box(0,0,0,6,6,6),1000000,-1000000).isEmpty());}
    @Test void tenEntriesConsumeOnlyTenCredits(){var w=new Wallets();assertTrue(w.recharge("card"));for(int i=0;i<10;i++)assertTrue(w.charge("card"));assertFalse(w.charge("card"));assertEquals(0,w.balance("card"));}
    @Test void clonedIdsShareOneAuthoritativeBalance(){var w=new Wallets();w.recharge("same-id");w.charge("same-id");assertEquals(9,w.balance("same-id"));}
    @Test void unknownIdDoesNotBecomeValidOnRead(){var w=new Wallets();assertEquals(0,w.balance("invented"));assertFalse(w.issued("invented"));assertFalse(w.charge("invented"));}
    @Test void topUpCapCannotOverflow(){var w=new Wallets();w.balances.put("card",Wallets.MAX-5);assertFalse(w.recharge("card"));assertEquals(Wallets.MAX-5,w.balance("card"));}
    @ParameterizedTest @CsvSource({"station_access:setter,true","station_access:terminal,true","fabric_train_simulator:locomotive,true","minecraft:diamond,false","other:tool,false","mystation_access:tool,false"})
    void onlyExplicitNamespacesPass(String item,boolean expected){assertEquals(expected,Rules.trusted(item));}
    @Test void toolDoesNotReplacePlayersTicket(){assertFalse(Rules.crossing(false,true,true,true,false,100));}
    @Test void zeroBalanceCardCanExit(){assertTrue(Rules.crossing(false,true,false,false,true,0));}
    @Test void zeroBalanceCannotEnter(){assertFalse(Rules.crossing(false,true,false,true,true,0));}
    @Test void spectatorDoesNotNeedTicket(){assertTrue(Rules.crossing(true,true,false,true,false,0));}
    @Test void survivalToolWearOnlyAfterSuccess(){assertEquals(1,Rules.wear(false,true));assertEquals(0,Rules.wear(false,false));assertEquals(0,Rules.wear(true,true));}
}
