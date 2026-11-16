package cn.quit5700.fabrictrain.depot;

public final class DepotDirectionRules {
    private DepotDirectionRules() {}
    public static int closestReachableIndex(int[] distances){
        int chosen=-1,best=Integer.MAX_VALUE;boolean tied=false;
        for(int i=0;i<distances.length;i++)if(distances[i]>=0){
            if(distances[i]<best){best=distances[i];chosen=i;tied=false;}
            else if(distances[i]==best)tied=true;
        }
        return tied?-1:chosen;
    }


    public static int frontIndex(boolean firstHasNode, boolean secondHasNode) {
        if (firstHasNode == secondHasNode) return -1;
        return firstHasNode ? 0 : 1;
    }

    public static int frontIndex(int firstDistance, int secondDistance) {
        if (firstDistance < 0 && secondDistance < 0) return -1;
        if (firstDistance < 0) return 1;
        if (secondDistance < 0) return 0;
        if (firstDistance == secondDistance) return -1;
        return firstDistance < secondDistance ? 0 : 1;
    }
}
