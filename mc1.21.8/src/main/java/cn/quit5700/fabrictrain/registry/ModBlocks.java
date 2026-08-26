package cn.quit5700.fabrictrain.registry;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.block.SignalRailBlock;
import cn.quit5700.fabrictrain.block.DeviceBlock;
import cn.quit5700.fabrictrain.block.DeviceRailBlock;
import cn.quit5700.fabrictrain.data.DeviceType;
import cn.quit5700.fabrictrain.block.TrainDepotBlock;
import cn.quit5700.fabrictrain.block.PowerGeneratorBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final Block SIGNAL_RAIL = registerRail("signal_rail", Blocks.RAIL);
    public static final Block SWITCH_RAIL = registerDeviceRail("switch_rail", Blocks.RAIL, DeviceType.SWITCH);
    public static final Block TRAIN_DEPOT = registerDepot("train_depot", Blocks.CRAFTING_TABLE);
    public static final Block STATION_MARKER = registerDevice("station_marker", Blocks.IRON_BLOCK, DeviceType.STATION);
    public static final Block POWER_GENERATOR = registerGenerator("power_generator", Blocks.REDSTONE_BLOCK);
    public static final Block LOCOMOTIVE_DISPLAY = register("locomotive_display", Blocks.STONE);
    public static final Block PASSENGER_DISPLAY = register("passenger_display", Blocks.STONE);
    public static final Block CARGO_DISPLAY = register("cargo_display", Blocks.STONE);

    private ModBlocks() {
    }

    private static Block register(String path, Block copiedBlock) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new Block(BlockBehaviour.Properties.ofFullCopy(copiedBlock).setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block registerRail(String path, Block copiedBlock) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new SignalRailBlock(BlockBehaviour.Properties.ofFullCopy(copiedBlock).setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block registerDevice(String path, Block copiedBlock, DeviceType type) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new DeviceBlock(BlockBehaviour.Properties.ofFullCopy(copiedBlock).setId(key), type);
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block registerDepot(String path, Block copiedBlock) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new TrainDepotBlock(BlockBehaviour.Properties.ofFullCopy(copiedBlock).setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block registerGenerator(String path, Block copiedBlock) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new PowerGeneratorBlock(BlockBehaviour.Properties.ofFullCopy(copiedBlock).setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    private static Block registerDeviceRail(String path, Block copiedBlock, DeviceType type) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        Block block = new DeviceRailBlock(BlockBehaviour.Properties.ofFullCopy(copiedBlock).setId(key), type);
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    public static void initialize() {
    }
}
