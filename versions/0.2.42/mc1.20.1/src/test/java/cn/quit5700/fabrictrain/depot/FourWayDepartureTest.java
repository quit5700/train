package cn.quit5700.fabrictrain.depot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FourWayDepartureTest {
 @Test void allFourBranchPositionsCanBeSelected(){for(int i=0;i<4;i++){int[] d={-1,-1,-1,-1};d[i]=5;assertEquals(i,DepotDirectionRules.closestReachableIndex(d));}}
 @Test void noRouteAndEqualShortestBranchesStayRejected(){assertEquals(-1,DepotDirectionRules.closestReachableIndex(new int[]{-1,-1,-1,-1}));assertEquals(-1,DepotDirectionRules.closestReachableIndex(new int[]{3,8,3,-1}));assertEquals(2,DepotDirectionRules.closestReachableIndex(new int[]{12,-1,4,8}));}
}
