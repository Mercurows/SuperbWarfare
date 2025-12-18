package com.atsuishio.superbwarfare.menu;

import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity;
import com.atsuishio.superbwarfare.init.ModBlocks;
import com.atsuishio.superbwarfare.init.ModMenuTypes;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.FinishAssemblingVehicleMessage;
import com.atsuishio.superbwarfare.recipe.vehicle.VehicleAssemblingRecipe;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VehicleAssemblingMenu extends AbstractContainerMenu {

    private final boolean isVehicleMenu;
    protected final ContainerLevelAccess access;

    public VehicleAssemblingMenu(int pContainerId, Inventory inventory) {
        this(pContainerId, inventory, ContainerLevelAccess.NULL);
    }

    public VehicleAssemblingMenu(int pContainerId, Inventory inventory, ContainerLevelAccess access) {
        this(pContainerId, inventory, access, false);
    }

    public VehicleAssemblingMenu(int pContainerId, Inventory inventory, ContainerLevelAccess access, boolean isVehicleMenu) {
        super(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), pContainerId);
        this.isVehicleMenu = isVehicleMenu;
        this.access = access;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return (pPlayer.isAlive() && !this.isVehicleMenu &&
                this.access.evaluate((level, pos) -> level.getBlockState(pos).is(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get())
                        && pPlayer.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64, true))
                || (this.isVehicleMenu && pPlayer.getVehicle() instanceof VehicleAssemblingTableVehicleEntity);
    }

    /**
     * Code based on TaC-Z
     */
    public void assembleVehicle(ResourceLocation id, ServerPlayer player) {
        var recipe = this.getRecipeById(id, player.level().getRecipeManager());
        if (recipe == null) return;
        player.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            if (!player.isCreative()) {
                Int2IntArrayMap recordCount = new Int2IntArrayMap();
                var ingredients = recipe.getInputs();

                for (var ingredient : ingredients) {
                    int count = 0;

                    for (int i = 0; i < handler.getSlots(); ++i) {
                        ItemStack stack = handler.getStackInSlot(i);
                        int stackCount = stack.getCount();
                        if (!stack.isEmpty() && ingredient.getIngredient().test(stack)) {
                            count += stackCount;
                            if (count > ingredient.getCount()) {
                                int remaining = count - ingredient.getCount();
                                recordCount.put(i, stackCount - remaining);
                                break;
                            }
                            recordCount.put(i, stackCount);
                        }
                    }

                    if (count < ingredient.getCount()) {
                        return;
                    }
                }

                for (int slotIndex : recordCount.keySet()) {
                    handler.extractItem(slotIndex, recordCount.get(slotIndex), false);
                }
            }

            Level level = player.level();
            if (!level.isClientSide) {
                ItemEntity itemEntity = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), recipe.getResultItem(player.level().registryAccess()).copy());
                itemEntity.setPickUpDelay(0);
                level.addFreshEntity(itemEntity);
            }

            player.inventoryMenu.broadcastFullState();
            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new FinishAssemblingVehicleMessage(this.containerId));
        });
    }

    @Nullable
    public VehicleAssemblingRecipe getRecipeById(ResourceLocation id, RecipeManager recipeManager) {
        Recipe<?> recipe = recipeManager.byKey(id).orElse(null);
        if (recipe instanceof VehicleAssemblingRecipe assemblingRecipe) {
            return assemblingRecipe;
        }
        return null;
    }
}
