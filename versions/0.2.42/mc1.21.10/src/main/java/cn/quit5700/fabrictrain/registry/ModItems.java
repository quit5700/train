package cn.quit5700.fabrictrain.registry;

import cn.quit5700.fabrictrain.FabricTrainMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MinecartItem;
import cn.quit5700.fabrictrain.item.LineSetterItem;
import cn.quit5700.fabrictrain.item.TrainConnectorItem;

public final class ModItems {
    public static final Item SIGNAL_RAIL = registerBlock("signal_rail", ModBlocks.SIGNAL_RAIL);
    public static final Item SWITCH_RAIL = registerBlock("switch_rail", ModBlocks.SWITCH_RAIL);
    public static final Item FREIGHT_YARD = registerBlock("freight_yard", ModBlocks.FREIGHT_YARD);
    public static final Item TRAIN_DEPOT = registerBlock("train_depot", ModBlocks.TRAIN_DEPOT);
    public static final Item STATION_MARKER = registerBlock("station_marker", ModBlocks.STATION_MARKER);
    public static final Item POWER_GENERATOR = registerBlock("power_generator", ModBlocks.POWER_GENERATOR);
    public static final Item LOCOMOTIVE = registerLocomotive("locomotive", ModEntities.LOCOMOTIVE);
    public static final Item PASSENGER_CAR = registerMinecart("passenger_car", ModEntities.PASSENGER_CAR);
    public static final Item CARGO_CAR = registerMinecart("cargo_car", ModEntities.CARGO_CAR);
    public static final Item LINE_SETTER = register("line_setter");
    public static final Item TRAIN_CONNECTOR = register("train_connector");

    private ModItems() {
    }

    private static Item registerBlock(String path, net.minecraft.world.level.block.Block block) {
        ResourceLocation id = FabricTrainMod.id(path);
        Item.Properties properties = properties(id);
        return Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, properties));
    }

    private static Item register(String path) {
        ResourceLocation id = FabricTrainMod.id(path);
        Item.Properties properties = properties(id);
        if (path.equals("line_setter") || path.equals("train_connector")) {
            properties.stacksTo(1);
        } else {
            properties.stacksTo(64);
        }
        Item item = path.equals("line_setter") ? new LineSetterItem(properties)
                : path.equals("train_connector") ? new TrainConnectorItem(properties) : new Item(properties);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    private static Item registerMinecart(String path, net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.vehicle.AbstractMinecart> type) {
        ResourceLocation id = FabricTrainMod.id(path);
        return Registry.register(BuiltInRegistries.ITEM, id, new MinecartItem(type, properties(id).stacksTo(64)));
    }
    private static Item registerLocomotive(String path,net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.vehicle.AbstractMinecart> type){ResourceLocation id=FabricTrainMod.id(path);return Registry.register(BuiltInRegistries.ITEM,id,new cn.quit5700.fabrictrain.item.LocomotiveItem(type,properties(id).stacksTo(64)));}

    private static Item.Properties properties(ResourceLocation id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    public static void initialize() {
    }
}
