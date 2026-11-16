package cn.quit5700.stationaccess;
import cn.quit5700.stationaccess.client.CursorActions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class CursorActionsTest {
 @Test void explicitCreativeToolGestureOpensGap(){assertTrue(CursorActions.breach(true,true,true));}
 @Test void ordinaryCreativeAttacksAreNotGapGestures(){assertFalse(CursorActions.breach(true,false,false));}
 @Test void sneakingWithoutSetterKeepsNormalInteraction(){assertFalse(CursorActions.breach(true,false,true));}
 @Test void setterWithoutSneakingKeepsPointSelection(){assertFalse(CursorActions.breach(true,true,false));}
 @Test void survivalNeverGetsCreativeGapGesture(){assertFalse(CursorActions.breach(false,true,true));}
}
