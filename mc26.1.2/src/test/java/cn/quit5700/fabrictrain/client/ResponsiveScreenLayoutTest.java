package cn.quit5700.fabrictrain.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponsiveScreenLayoutTest {
    @Test void footerAlwaysFitsSmallLogicalScreen() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(250, 318, 20);
        assertTrue(layout.top() >= 4);
        assertTrue(layout.bottom() <= 246);
        assertTrue(layout.footerY() + 20 <= layout.bottom() - 4);
    }

    @Test void contentScrollsInsteadOfCoveringFooter() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(250, 318, 20);
        assertTrue(layout.scrollBottom() <= layout.footerY());
        assertTrue(layout.scrollRequired());
    }

    @Test void compactDepotActionsAndPinnedSaveBothFitAtGuiScaleFour() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(250, 242, 20);
        int actionY = layout.top() + 198;
        assertTrue(actionY + 20 <= layout.scrollBottom());
        assertTrue(layout.footerY() + 20 <= layout.bottom() - 4);
    }

    @Test void panelWidthNeverExceedsLogicalScreenAtHighGuiScale() {
        ResponsiveScreenLayout layout = ResponsiveScreenLayout.of(320, 240, 900, 500, 20);
        assertTrue(layout.left() >= 4);
        assertTrue(layout.right() <= 316);
        assertTrue(layout.bottom() <= 236);
    }
}
