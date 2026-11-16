package cn.quit5700.fabrictrain;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalRailTextureSymmetryTest {
    @Test
    void bothStraightRailsUseTheCyanSignalPalette() throws Exception {
        BufferedImage image = ImageIO.read(Path.of(
                "src/main/resources/assets/fabric_train_simulator/textures/block/signal_rail.png").toFile());

        assertTrue(cyanPixels(image, 2, 3) >= 20, "left rail must be cyan");
        assertTrue(cyanPixels(image, 12, 13) >= 20, "right rail must also be cyan");
    }

    private static int cyanPixels(BufferedImage image, int fromX, int toX) {
        int count = 0;
        for (int x = fromX; x <= toX; x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;
                int red = argb >>> 16 & 255;
                int green = argb >>> 8 & 255;
                int blue = argb & 255;
                if (alpha > 0 && green > 150 && blue > 150 && red < 180) count++;
            }
        }
        return count;
    }
}
