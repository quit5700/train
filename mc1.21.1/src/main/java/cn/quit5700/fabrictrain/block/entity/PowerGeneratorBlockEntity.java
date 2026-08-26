package cn.quit5700.fabrictrain.block.entity;


import cn.quit5700.fabrictrain.compat.NbtCompat;
import cn.quit5700.fabrictrain.power.RailPowerService;
import cn.quit5700.fabrictrain.power.GeneratorFuelState;
import cn.quit5700.fabrictrain.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PowerGeneratorBlockEntity extends BaseContainerBlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(PowerGeneratorBlockEntity.class);
    private static final String FUEL_UNTIL_KEY = "FuelUntilGameTime";
    private static final String LEGACY_REMAINING_KEY = "RemainingFuelTicks";
    private static final String RESERVED_FUEL_SLOT_KEY = "ReservedFuelSlot";
    public static final int SIZE = 9;
    public static final long REDSTONE_DUST_TICKS = 72_000L;
    public static final long REDSTONE_BLOCK_TICKS = 504_000L;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private GeneratorFuelState fuelState = GeneratorFuelState.empty();
    private long lastObservedGameTime;
    private boolean active;

    public PowerGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_GENERATOR, pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PowerGeneratorBlockEntity generator) {
        long gameTime = level.getGameTime();
        generator.lastObservedGameTime = gameTime;
        boolean before = generator.active;
        boolean migrated = generator.fuelState.migrateLegacyRemaining(gameTime);
        boolean expired = generator.fuelState.settleExpiredFuel(gameTime, generator::consumeReservedFuel);
        generator.active = generator.fuelState.isActive(gameTime);
        boolean activated = !generator.active && generator.activateFuelIfAvailable(level, gameTime);
        generator.active = generator.fuelState.isActive(gameTime);
        RailPowerService.updateGenerator(level, pos, generator.active);
        if (before != generator.active || activated || expired) {
            RailPowerService.refreshNow(level);
            generator.setChanged();
            level.updateNeighborsAt(pos, state.getBlock());
        } else if (migrated) {
            generator.setChanged();
        }
    }

    private boolean activateFuelIfAvailable(ServerLevel serverLevel, long gameTime) {
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            long fuelTicks = fuelDuration(stack);
            if (fuelTicks <= 0) continue;
            try {
                return fuelState.tryActivate(gameTime, fuelTicks, slot, () -> {
                    RailPowerService.updateGenerator(serverLevel, worldPosition, true);
                    RailPowerService.refreshNow(serverLevel);
                    setChanged();
                    return true;
                });
            } catch (RuntimeException | Error failure) {
                RailPowerService.updateGenerator(serverLevel, worldPosition, false);
                LOGGER.error("铁路发电机燃料提交失败，燃料未扣除：{}", worldPosition, failure);
                return false;
            }
        }
        return false;
    }

    private boolean consumeReservedFuel(int slot) {
        if (slot < 0 || slot >= items.size()) return true;
        ItemStack stack = items.get(slot);
        if (fuelDuration(stack) <= 0L) {
            LOGGER.warn("铁路发电机到期燃料槽位已无有效燃料：{} 槽位 {}", worldPosition, slot);
            return true;
        }
        stack.shrink(1);
        setChanged();
        return true;
    }

    static long fuelDuration(ItemStack stack) {
        if (stack.is(Items.REDSTONE_BLOCK)) return REDSTONE_BLOCK_TICKS;
        if (stack.is(Items.REDSTONE)) return REDSTONE_DUST_TICKS;
        return 0L;
    }

    public boolean active() { return active; }
    public long remainingTicks() { return fuelState.remainingTicks(lastObservedGameTime); }
    @Override public int getContainerSize() { return SIZE; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public ItemStack removeItem(int slot, int amount) {
        return isFuelSlotLocked(slot) ? ItemStack.EMPTY : super.removeItem(slot, amount);
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        return isFuelSlotLocked(slot) ? ItemStack.EMPTY : super.removeItemNoUpdate(slot);
    }
    private boolean isFuelSlotLocked(int slot) {
        return active && fuelState.reservedFuelSlot() == slot;
    }
    @Override protected Component getDefaultName() {
        long remainingTicks = remainingTicks();
        Component status = remainingTicks > 0
                ? Component.translatable("container.fabric_train_simulator.power_generator.remaining", formatRemainingTime())
                : Component.translatable("container.fabric_train_simulator.power_generator.offline");
        return Component.translatable("container.fabric_train_simulator.power_generator", status);
    }
    @Override protected AbstractContainerMenu createMenu(int syncId, Inventory inventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, syncId, inventory, this, 1);
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(Items.REDSTONE) || stack.is(Items.REDSTONE_BLOCK);
    }
    @Override public boolean canTakeItem(Container destination, int slot, ItemStack stack) {
        return !isFuelSlotLocked(slot);
    }

    private String formatRemainingTime() {
        long seconds = remainingTicks() / 20L;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", seconds / 3600L,
                (seconds % 3600L) / 60L, seconds % 60L);
    }

    @Override protected void saveAdditional(CompoundTag output, HolderLookup.Provider registries) {
        super.saveAdditional(output, registries);
        ContainerHelper.saveAllItems(output, items, registries);
        output.putLong(FUEL_UNTIL_KEY, fuelState.activeUntilGameTime());
        output.putLong(LEGACY_REMAINING_KEY, remainingTicks());
        output.putInt(RESERVED_FUEL_SLOT_KEY, fuelState.reservedFuelSlot());
    }

    @Override protected void loadAdditional(CompoundTag input, HolderLookup.Provider registries) {
        super.loadAdditional(input, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items, registries);
        long activeUntil = NbtCompat.getLong(input, FUEL_UNTIL_KEY, 0L);
        long legacyRemaining = NbtCompat.getLong(input, LEGACY_REMAINING_KEY, 0L);
        int reservedFuelSlot = NbtCompat.getInt(input, RESERVED_FUEL_SLOT_KEY, -1);
        fuelState = GeneratorFuelState.restore(activeUntil, legacyRemaining, reservedFuelSlot);
        lastObservedGameTime = 0L;
        active = activeUntil > 0L || legacyRemaining > 0L;
    }
}
