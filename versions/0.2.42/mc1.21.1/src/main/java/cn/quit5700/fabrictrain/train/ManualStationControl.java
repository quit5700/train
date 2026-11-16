package cn.quit5700.fabrictrain.train;
/** 纯状态机：到站后必须先释放前进键，再重新按下才能离站。 */
public final class ManualStationControl {
 private boolean enabled,waiting,released,lastForward=true;
 public boolean enabled(){return enabled;}
 public boolean waiting(){return waiting;}
 public void restore(boolean mode,boolean stopped){enabled=mode;waiting=mode&&stopped;released=false;lastForward=true;}
 public boolean arrive(){if(!enabled)return false;waiting=true;released=!lastForward;return true;}
 public boolean acceptInput(boolean forward){
  lastForward=forward;
  if(!waiting)return true;
  if(!forward){released=true;return false;}
  if(!released)return false;
  waiting=false;released=false;return true;
 }
}
