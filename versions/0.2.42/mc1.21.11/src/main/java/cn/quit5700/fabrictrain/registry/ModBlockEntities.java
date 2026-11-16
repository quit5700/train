package cn.quit5700.fabrictrain.registry;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.block.entity.PowerGeneratorBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class ModBlockEntities {
    public static final BlockEntityType<TrainDepotBlockEntity> TRAIN_DEPOT = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            FabricTrainMod.id("train_depot"),
            FabricBlockEntityTypeBuilder.create(TrainDepotBlockEntity::new, ModBlocks.TRAIN_DEPOT).build());
    public static final BlockEntityType<PowerGeneratorBlockEntity> POWER_GENERATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            FabricTrainMod.id("power_generator"),
            FabricBlockEntityTypeBuilder.create(PowerGeneratorBlockEntity::new, ModBlocks.POWER_GENERATOR).build());

    private ModBlockEntities() {}
    public static void initialize() {}
}
