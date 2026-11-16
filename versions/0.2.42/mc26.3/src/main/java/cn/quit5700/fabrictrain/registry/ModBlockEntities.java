package cn.quit5700.fabrictrain.registry;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.block.entity.TrainDepotBlockEntity;
import cn.quit5700.fabrictrain.block.entity.PowerGeneratorBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
    public static final BlockEntityType<TrainDepotBlockEntity> TRAIN_DEPOT = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            FabricTrainMod.id("train_depot"),
            new BlockEntityType<>(TrainDepotBlockEntity::new, Set.of(ModBlocks.TRAIN_DEPOT)));
    public static final BlockEntityType<PowerGeneratorBlockEntity> POWER_GENERATOR = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            FabricTrainMod.id("power_generator"),
            new BlockEntityType<>(PowerGeneratorBlockEntity::new, Set.of(ModBlocks.POWER_GENERATOR)));

    private ModBlockEntities() {}
    public static void initialize() {}
}
