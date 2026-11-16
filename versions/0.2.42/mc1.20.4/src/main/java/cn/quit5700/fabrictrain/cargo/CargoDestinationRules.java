package cn.quit5700.fabrictrain.cargo;

public final class CargoDestinationRules {
    private CargoDestinationRules() {}

    public static long afterConnecting(long cargoDestination, long locomotiveDestination) {
        return cargoDestination > 0L ? cargoDestination : Math.max(0L,locomotiveDestination);
    }

    public static boolean atStation(double cargoX,double cargoY,double cargoZ,
                                    double stationCenterX,double stationCenterY,double stationCenterZ){
        return Math.abs(cargoX-stationCenterX)<=2D&&Math.abs(cargoY-stationCenterY)<=2.5D
                &&Math.abs(cargoZ-stationCenterZ)<=2D;
    }
}
