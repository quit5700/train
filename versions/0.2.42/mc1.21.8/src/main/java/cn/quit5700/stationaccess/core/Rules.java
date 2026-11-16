package cn.quit5700.stationaccess.core;
public final class Rules {
 public static boolean trusted(String id){int i=id.indexOf(':');String namespace=i<0?"":id.substring(0,i);return namespace.equals("station_access")||namespace.equals("fabric_train_simulator");}
 public static boolean crossing(boolean spectator,boolean player,boolean trusted,boolean entering,boolean ticket,int balance){if(spectator)return true;if(!player)return trusted;return !entering||ticket&&balance>=Wallets.FARE;}
 public static int wear(boolean creative,boolean success){return !creative&&success?1:0;}
}
