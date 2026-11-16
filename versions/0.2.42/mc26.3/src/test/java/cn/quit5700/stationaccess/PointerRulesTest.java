package cn.quit5700.stationaccess;
import org.junit.jupiter.api.Test;
import cn.quit5700.stationaccess.client.PointerRules;
import com.mojang.blaze3d.platform.InputConstants;
import static org.junit.jupiter.api.Assertions.*;
final class PointerRulesTest {
 @Test void sdlPrimaryClickMatchesGameConstant(){assertTrue(PointerRules.primary(InputConstants.MOUSE_BUTTON_LEFT));assertFalse(PointerRules.primary(InputConstants.MOUSE_BUTTON_RIGHT));assertFalse(PointerRules.primary(0));}
}
