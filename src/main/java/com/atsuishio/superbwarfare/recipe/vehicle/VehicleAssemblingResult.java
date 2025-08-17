package com.atsuishio.superbwarfare.recipe.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.TagDataParser;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public class VehicleAssemblingResult {
    @SerializedName("item")
    public String itemString = "";
    @SerializedName("entity")
    public String entityTypeString = "";
    @SerializedName("count")
    public int count = 1;
    @SerializedName("nbt")
    public JsonObject nbt;

    public static final Codec<VehicleAssemblingResult> CODEC = RecordCodecBuilder.<VehicleAssemblingResult>mapCodec(builder -> builder.group(
            Codec.STRING.optionalFieldOf("item", BuiltInRegistries.ITEM.getKey(ModItems.CONTAINER.value()).toString()).forGetter(r -> r.itemString),
            Codec.STRING.optionalFieldOf("entity", "").forGetter(r -> r.entityTypeString),
            Codec.INT.optionalFieldOf("count", 1).forGetter(r -> r.count)
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
            var location = ResourceLocation.parse(itemString);
            var item = BuiltInRegistries.ITEM.get(location);
            if (nbt != null) {
                var tag = TagDataParser.parse(nbt);
                tag.putString("id", location.toString());
                tag.putInt("count", 1);
                ItemStack.parse(RegistryAccess.EMPTY, tag).ifPresent(stack -> this.result = stack);
            } else {
                this.result = new ItemStack(item, count);
            }
        } else {
            this.result = ItemStack.EMPTY;
        }

        return this.result;
    }
}
