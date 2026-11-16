package cn.quit5700.stationaccess.client;
import com.mojang.blaze3d.platform.InputConstants;
public final class PointerRules {
 private PointerRules(){}
 public static boolean primary(int button){return button==InputConstants.MOUSE_BUTTON_LEFT;}
}
