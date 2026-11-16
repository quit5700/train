package cn.quit5700.fabrictrain.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Restores already-owned contents before ticking, without requesting the loading chunk again. */
@Mixin(RandomizableContainerBlockEntity.class)
public interface ManagedInventoryAccessor {
    @Invoker("getItems") NonNullList<ItemStack> fabricTrain$items();
}
