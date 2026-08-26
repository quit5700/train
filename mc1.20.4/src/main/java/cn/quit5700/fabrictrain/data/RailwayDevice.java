package cn.quit5700.fabrictrain.data;

public record RailwayDevice(long id, DeviceType type, String dimension, int x, int y, int z, String name,
                            int dwellSeconds, int cargoDwellSeconds, int switchShapeOrdinal, int switchEntryOrdinal,
                            String announcement, boolean manualUnload) {
    public RailwayDevice(long id, DeviceType type, String dimension, int x, int y, int z, String name,
                         int dwellSeconds, int switchShapeOrdinal) {
        this(id,type,dimension,x,y,z,name,dwellSeconds,120,switchShapeOrdinal,-1,"",false);
    }
    public RailwayDevice(long id, DeviceType type, String dimension, int x, int y, int z, String name,
                         int dwellSeconds, int switchShapeOrdinal, String announcement) {
        this(id,type,dimension,x,y,z,name,dwellSeconds,120,switchShapeOrdinal,-1,announcement,false);
    }
    public RailwayDevice(long id, DeviceType type, String dimension, int x, int y, int z, String name,
                         int dwellSeconds, int switchShapeOrdinal, String announcement, boolean manualUnload) {
        this(id,type,dimension,x,y,z,name,dwellSeconds,120,switchShapeOrdinal,-1,announcement,manualUnload);
    }
    public RailwayDevice(long id, DeviceType type, String dimension, int x, int y, int z, String name,
                         int dwellSeconds, int cargoDwellSeconds, int switchShapeOrdinal,
                         String announcement, boolean manualUnload) {
        this(id,type,dimension,x,y,z,name,dwellSeconds,cargoDwellSeconds,switchShapeOrdinal,-1,announcement,manualUnload);
    }
}
