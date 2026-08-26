package cn.quit5700.fabrictrain.registry;

import cn.quit5700.fabrictrain.FabricTrainMod;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroup {
    public static final CreativeModeTab TRAINS = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            FabricTrainMod.id("trains"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fabric_train_simulator.trains"))
                    .icon(() -> new ItemStack(ModItems.LOCOMOTIVE))
                    .displayItems((context, entries) -> {
                        entries.accept(ModItems.SIGNAL_RAIL);
                        entries.accept(ModItems.SWITCH_RAIL);
                        entries.accept(ModItems.POWER_GENERATOR);
                        entries.accept(ModItems.TRAIN_DEPOT);
                        entries.accept(ModItems.STATION_MARKER);
                        entries.accept(ModItems.LOCOMOTIVE);
                        entries.accept(ModItems.PASSENGER_CAR);
                        entries.accept(ModItems.CARGO_CAR);
                        entries.accept(ModItems.LINE_SETTER);
                        entries.accept(ModItems.TRAIN_CONNECTOR);
                    })
                    .build()
    );

    private ModItemGroup() {
    }

    public static void initialize() {
    }
}
