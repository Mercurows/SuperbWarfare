package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public class VehicleAssemblingResult {
    @SerializedName("Item")
    public String itemString = "";
    @SerializedName("EntityType")
    public String entityTypeString = "";
    @SerializedName("Count")
    public int count = 1;

    public static final Codec<VehicleAssemblingResult> CODEC = RecordCodecBuilder.<VehicleAssemblingResult>mapCodec(builder -> builder.group(
            Codec.STRING.fieldOf("item").forGetter(r -> r.itemString),
            Codec.STRING.fieldOf("entity_type").forGetter(r -> r.entityTypeString),
            Codec.INT.fieldOf("count").forGetter(r -> r.count)
    ).apply(builder, VehicleAssemblingResult::new)).codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleAssemblingResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, r -> r.itemString,
            ByteBufCodecs.STRING_UTF8, r -> r.entityTypeString,
            ByteBufCodecs.VAR_INT, r -> r.count,
            VehicleAssemblingResult::new
    );

    public VehicleAssemblingResult() {
    }

    public VehicleAssemblingResult(String itemString, String entityTypeString, int count) {
        this.itemString = itemString;
        this.entityTypeString = entityTypeString;
        this.count = count;
    }

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
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemString));
            this.result = new ItemStack(item, count);
        } else {
            this.result = ItemStack.EMPTY;
        }

        return this.result;
    }
}
