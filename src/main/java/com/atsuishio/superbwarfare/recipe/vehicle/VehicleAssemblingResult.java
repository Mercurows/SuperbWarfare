package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VehicleAssemblingResult {

    private ItemStack result = ItemStack.EMPTY;
    @Nullable
    public ItemStack item = null;
    @Nullable
    public EntityType<?> entityType = null;

    public VehicleAssemblingResult(ItemStack item) {
        this.item = item;
    }

    public VehicleAssemblingResult(EntityType<?> entityType) {
        this.entityType = entityType;
    }

    public ItemStack getResult() {
        if (this.item != null) {
            this.result = this.item.copy();
            this.item = null;
        } else if (this.entityType != null) {
            this.result = ContainerBlockItem.createInstance(this.entityType);
            this.entityType = null;
        }
        return this.result;
    }
}
