package cn.quit5700.stationaccess.core;
import java.util.*;
public final class Wallets {
 public static final int TOP_UP=10,FARE=1,MAX=30000;
 public Map<String,Integer> balances=new LinkedHashMap<>();
 public int balance(String id){return id==null?0:balances.getOrDefault(id,0);}
 public boolean issued(String id){return id!=null&&balances.containsKey(id);}
 public boolean recharge(String id){int n=balance(id);if(id==null||id.isBlank()||n>MAX-TOP_UP)return false;balances.put(id,n+TOP_UP);return true;}
 public boolean charge(String id){int n=balance(id);if(n<FARE)return false;balances.put(id,n-FARE);return true;}
}
