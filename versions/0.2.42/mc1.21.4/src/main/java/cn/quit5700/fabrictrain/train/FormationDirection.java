package cn.quit5700.fabrictrain.train;

import net.minecraft.core.Direction;

import java.util.Optional;

public final class FormationDirection {
    private FormationDirection() {}

    public static Optional<Direction> fromPositions(double locomotiveX, double locomotiveZ,
                                                    double carriageX, double carriageZ) {
        double x = locomotiveX - carriageX;
        double z = locomotiveZ - carriageZ;
        if (x * x + z * z < 0.000001D) return Optional.empty();
        if (Math.abs(x) >= Math.abs(z)) return Optional.of(x >= 0 ? Direction.EAST : Direction.WEST);
        return Optional.of(z >= 0 ? Direction.SOUTH : Direction.NORTH);
    }
}
