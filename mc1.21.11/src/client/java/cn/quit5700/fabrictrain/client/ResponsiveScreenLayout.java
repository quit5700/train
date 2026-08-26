package cn.quit5700.fabrictrain.client;

public record ResponsiveScreenLayout(int left, int top, int panelWidth, int panelHeight, int footerY,
                                     int scrollTop, int scrollBottom, boolean scrollRequired,
                                     int desiredWidth, int desiredHeight) {
    private static final int MARGIN = 4;

    public static ResponsiveScreenLayout of(int screenHeight, int desiredHeight, int footerHeight) {
        return of(0, screenHeight, 0, desiredHeight, footerHeight);
    }

    public static ResponsiveScreenLayout of(int screenWidth, int screenHeight, int desiredWidth,
                                            int desiredHeight, int footerHeight) {
        int available = Math.max(1, screenHeight - MARGIN * 2);
        int panelHeight = Math.min(desiredHeight, available);
        int top = Math.max(MARGIN, (screenHeight - panelHeight) / 2);
        int panelWidth = desiredWidth <= 0 ? 0 : Math.min(desiredWidth, Math.max(1, screenWidth - MARGIN * 2));
        int left = desiredWidth <= 0 ? 0 : Math.max(MARGIN, (screenWidth - panelWidth) / 2);
        int bottom = top + panelHeight;
        int footerY = bottom - MARGIN - footerHeight;
        int scrollTop = top + 16;
        int scrollBottom = footerY;
        return new ResponsiveScreenLayout(left, top, panelWidth, panelHeight, footerY, scrollTop, scrollBottom,
                desiredHeight > panelHeight, desiredWidth, desiredHeight);
    }

    public int bottom() { return top + panelHeight; }

    public int right() { return left + panelWidth; }

    public int x(int desiredOffset) {
        if (desiredWidth <= 0 || panelWidth >= desiredWidth) return left + desiredOffset;
        return left + (int) Math.round(desiredOffset * (double) panelWidth / desiredWidth);
    }

    public int width(int desiredSize) {
        if (desiredWidth <= 0 || panelWidth >= desiredWidth) return desiredSize;
        return Math.max(18, (int) Math.round(desiredSize * (double) panelWidth / desiredWidth));
    }
}
