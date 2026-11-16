package cn.quit5700.fabrictrain.compat.jade;

import cn.quit5700.fabrictrain.block.DeviceBlock;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.block.TrainDepotBlock;
import snownee.jade.api.*;

/** 仅由Jade的可选入口加载，未安装Jade时Fabric不加载此兼容层。 */
public final class TrainJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        for(Class<?> type:new Class<?>[]{DeviceBlock.class,DeviceRailBlock.class,TrainDepotBlock.class})
            registration.registerBlockDataProvider(DeviceNameData.INSTANCE,type);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(DeviceNameTooltip.INSTANCE,DeviceBlock.class);
        registration.registerBlockComponent(DeviceNameTooltip.INSTANCE,DeviceRailBlock.class);
        registration.registerBlockComponent(DeviceNameTooltip.INSTANCE,TrainDepotBlock.class);
    }
}
