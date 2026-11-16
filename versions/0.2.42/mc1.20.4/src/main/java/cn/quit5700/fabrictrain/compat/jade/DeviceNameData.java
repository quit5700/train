package cn.quit5700.fabrictrain.compat.jade;

import cn.quit5700.fabrictrain.data.RailwayDataManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.*;

public enum DeviceNameData implements IServerDataProvider<BlockAccessor> {
    INSTANCE;
    public static final String NAME_KEY="fts_device_name";
    public static final ResourceLocation ID=new ResourceLocation("fabric_train_simulator","device_name");
    @Override public ResourceLocation getUid(){return ID;}
    @Override public void appendServerData(CompoundTag tag,BlockAccessor accessor){
        if(!(accessor.getLevel() instanceof ServerLevel world))return;
        var pos=accessor.getPosition();
        var device=RailwayDataManager.state(world).find(RailwayDataManager.dimension(world),pos.getX(),pos.getY(),pos.getZ());
        if(device!=null&&!device.name().isBlank())tag.putString(NAME_KEY,device.name());
    }
}
