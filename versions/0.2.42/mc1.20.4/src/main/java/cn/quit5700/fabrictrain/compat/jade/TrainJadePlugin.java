package cn.quit5700.fabrictrain.compat.jade;

import cn.quit5700.fabrictrain.block.DeviceBlock;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.block.TrainDepotBlock;
import snownee.jade.api.*;

/** 仅由Jade的可选入口加载，未安装Jade时Fabric不加载此兼容层。 */
public final class TrainJadePlugin implements IWailaPlugin {
    @Override public void registerClient(IWailaClientRegistration registration) {
        try{Class.forName("cn.quit5700.fabrictrain.compat.jade.DeviceNameClient").getMethod("register").invoke(null);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        registration.registerBlockComponent(provider(),DeviceBlock.class);
        registration.registerBlockComponent(provider(),DeviceRailBlock.class);
        registration.registerBlockComponent(provider(),TrainDepotBlock.class);
    }
    private static IBlockComponentProvider provider(){try{return (IBlockComponentProvider)Class.forName("cn.quit5700.fabrictrain.compat.jade.DeviceNameTooltip").getField("INSTANCE").get(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
}

