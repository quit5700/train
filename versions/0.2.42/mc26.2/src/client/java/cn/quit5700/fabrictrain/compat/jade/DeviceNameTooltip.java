package cn.quit5700.fabrictrain.compat.jade;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@Environment(EnvType.CLIENT)
public enum DeviceNameTooltip implements IBlockComponentProvider {
    INSTANCE;
    @Override public Identifier getUid(){return DeviceNameData.ID;}
    @Override public int getDefaultPriority(){return -10099;}
    @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
        String name=accessor.getServerData().getStringOr(DeviceNameData.NAME_KEY,"");
        if(!name.isBlank())tooltip.replace(Identifier.fromNamespaceAndPath("jade","object_name"),Component.literal(name));
    }
}
