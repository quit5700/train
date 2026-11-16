package cn.quit5700.fabrictrain.registry;

import cn.quit5700.fabrictrain.FabricTrainMod;
import cn.quit5700.fabrictrain.entity.CargoCarEntity;
import cn.quit5700.fabrictrain.entity.LocomotiveEntity;
import cn.quit5700.fabrictrain.entity.PassengerCarEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<LocomotiveEntity> LOCOMOTIVE = register(
            "locomotive", LocomotiveEntity::new);
    public static final EntityType<PassengerCarEntity> PASSENGER_CAR = register(
            "passenger_car", PassengerCarEntity::new);
    public static final EntityType<CargoCarEntity> CARGO_CAR = register(
            "cargo_car", CargoCarEntity::new);

    private ModEntities() {
    }

    private static <T extends Entity> EntityType<T> register(String path, EntityType.EntityFactory<T> factory) {
        ResourceLocation id = FabricTrainMod.id(path);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        EntityType<T> type = EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.98F, 0.7F)
                .clientTrackingRange(10)
                .updateInterval(1)
                .build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type);
    }

    public static void initialize() {
    }
}
