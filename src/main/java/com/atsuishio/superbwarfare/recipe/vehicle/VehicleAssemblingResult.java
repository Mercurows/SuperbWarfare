package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class VehicleAssemblingResult {
    @SerializedName("item")
    public String itemString = "";
    @SerializedName("entity")
    public String entityTypeString = "";
    @SerializedName("count")
    public int count = 1;

    public transient ItemStack result = null;

    public ItemStack getResult() {
        if (this.result != null) return this.result;

        if (!entityTypeString.isEmpty()) {
            var type = EntityType.byString(entityTypeString).orElse(null);
            if (type == null) {
                Mod.LOGGER.warn("invalid entity type: {}", entityTypeString);
                this.result = ItemStack.EMPTY;
            } else {
                this.result = ContainerBlockItem.createInstance(type).copyWithCount(count);
            }
        } else if (!itemString.isEmpty()) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemString));
            if (item == null) {
                Mod.LOGGER.warn("invalid item: {}", itemString);
                this.result = ItemStack.EMPTY;
            } else {
                this.result = new ItemStack(item, count);
            }
        } else {
            this.result = ItemStack.EMPTY;
        }

        return this.result;
    }
}
