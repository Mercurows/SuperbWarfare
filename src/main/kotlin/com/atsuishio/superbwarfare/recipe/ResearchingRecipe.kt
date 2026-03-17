package com.atsuishio.superbwarfare.recipe

import com.atsuishio.superbwarfare.init.ModRecipes
import com.atsuishio.superbwarfare.tools.TagDataParser
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.neoforged.neoforge.items.wrapper.RecipeWrapper
import kotlin.jvm.optionals.getOrNull

class ResearchingRecipe(
    val input: Ingredient,
    val base: Ingredient,
    val addition: Ingredient,
    val special: Ingredient,
    val selectable: Boolean,
    val color: Int,
    val time: Int,
    val result: Result
) : Recipe<RecipeWrapper> {
    companion object {
        fun create(
            input: Ingredient,
            base: Ingredient,
            addition: Ingredient,
            special: Ingredient,
            selectable: Boolean,
            color: Int,
            time: Int,
            count: Int,
            result: Item
        ): ResearchingRecipe {
            return ResearchingRecipe(
                input,
                base,
                addition,
                special,
                selectable,
                color,
                time,
                Result(item = BuiltInRegistries.ITEM.getKey(result).toString(), count = count)
            )
        }

        fun create(
            input: Ingredient,
            base: Ingredient,
            addition: Ingredient,
            special: Ingredient,
            selectable: Boolean,
            color: Int,
            time: Int,
            count: Int,
            tag: TagKey<Item>
        ): ResearchingRecipe {
            return ResearchingRecipe(
                input,
                base,
                addition,
                special,
                selectable,
                color,
                time,
                Result(tag = tag.location.toString(), count = count)
            )
        }
    }

    override fun matches(
        container: RecipeWrapper,
        level: Level
    ): Boolean {
        if (container.size() < 4) {
            return false
        }
        return input.test(container.getItem(0))
                && base.test(container.getItem(1))
                && addition.test(container.getItem(2))
                && special.test(container.getItem(3))
    }

    override fun assemble(input: RecipeWrapper, registries: HolderLookup.Provider): ItemStack =
        this.result.getResult().copy()

    override fun isSpecial() = true

    override fun canCraftInDimensions(pWidth: Int, pHeight: Int) = true

    override fun getResultItem(registries: HolderLookup.Provider): ItemStack = this.result.getResult().copy()

    override fun getSerializer(): RecipeSerializer<*> = ModRecipes.RESEARCHING_SERIALIZER.get()

    override fun getType(): RecipeType<*> = ModRecipes.RESEARCHING_TYPE.get()

    class Result(
        @SerializedName("item") var item: String = "",
        @SerializedName("tag") var tag: String = "",
        @SerializedName("count") var count: Int = 1,
        @SerializedName("nbt") var nbt: JsonObject? = null,
    ) {
        companion object {
            val CODEC: Codec<Result> = RecordCodecBuilder.mapCodec<Result> { builder ->
                builder.group(
                    Codec.STRING.optionalFieldOf("item", "")
                        .forGetter { it.item },
                    Codec.STRING.optionalFieldOf("tag", "")
                        .forGetter { it.tag },
                    Codec.INT.optionalFieldOf("count", 1)
                        .forGetter { it.count }
                ).apply(builder, ::Result)
            }.codec()

            val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, Result> = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, { r: Result -> r.item },
                ByteBufCodecs.STRING_UTF8, { r: Result -> r.tag },
                ByteBufCodecs.VAR_INT, { r: Result -> r.count },
                ::Result
            )
        }

        @Transient
        var resultStack: ItemStack? = null

        @Transient
        var list: MutableList<Item>? = null

        fun getResult(): ItemStack {
            if (this.resultStack != null) return this.resultStack!!
            if (!item.isEmpty()) {
                val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(item))
                if (nbt != null) {
                    val tag = TagDataParser.parse(nbt)
                    val tmp = CompoundTag()

                    tmp.put("components", tag)
                    tmp.putString("id", this.item)
                    tmp.putInt("count", count)
                    this.resultStack = ItemStack.parseOptional(RegistryAccess.EMPTY, tmp)
                } else {
                    this.resultStack = ItemStack(item, count)
                }
            } else if (!this.getResultList().isEmpty()) {
                this.resultStack = ItemStack(this.getResultList().random(), count)
            } else {
                this.resultStack = ItemStack.EMPTY
            }

            return this.resultStack!!
        }

        fun getResultList(): MutableList<Item> {
            if (this.list != null && !this.list!!.isEmpty()) return this.list!!
            if (this.tag.isEmpty()) return mutableListOf()

            val tagKey = ItemTags.create(ResourceLocation.parse(this.tag))
            val itemTag = BuiltInRegistries.ITEM.getTag(tagKey)
                .map { items -> items.map { it.value() } }.getOrNull() ?: return mutableListOf()

            val list = mutableListOf<Item>()
            itemTag.forEach { list.add(it) }
            list.sortBy { it.descriptionId }
            this.list = list
            return this.list!!
        }

        fun getItemByIndex(index: Int): ItemStack {
            if (this.isRandom() && this.getResultList().size > index) {
                return ItemStack(this.getResultList()[index], count)
            }
            return this.getResult()
        }

        fun isRandom() = this.tag.isNotEmpty()

        fun rollItem(): ItemStack {
            if (this.isRandom() && !this.getResultList().isEmpty()) {
                return ItemStack(this.getResultList().random(), count)
            }
            return this.getResult()
        }
    }

    object Serializer : RecipeSerializer<ResearchingRecipe> {
        val CODEC: MapCodec<ResearchingRecipe> = RecordCodecBuilder.mapCodec { builder ->
            builder.group(
                Ingredient.CODEC.fieldOf("input").forGetter { it.input },
                Ingredient.CODEC.optionalFieldOf("base", Ingredient.EMPTY).forGetter { it.base },
                Ingredient.CODEC.optionalFieldOf("addition", Ingredient.EMPTY).forGetter { it.addition },
                Ingredient.CODEC.optionalFieldOf("special", Ingredient.EMPTY).forGetter { it.special },
                Codec.BOOL.optionalFieldOf("selectable", false).forGetter { it.selectable },
                Codec.INT.optionalFieldOf("color", 0).forGetter { it.color },
                Codec.INT.optionalFieldOf("time", 1200).forGetter { it.time },
                Result.CODEC.fieldOf("result").forGetter { it.result }
            ).apply(builder, ::ResearchingRecipe)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, ResearchingRecipe> =
            StreamCodec.of(this::toNetwork, this::fromNetwork)

        fun fromNetwork(
            buffer: RegistryFriendlyByteBuf
        ): ResearchingRecipe {
            val input = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
            val base = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
            val addition = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
            val special = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer)
            val selectable = buffer.readBoolean()
            val color = buffer.readInt()
            val time = buffer.readInt()

            val res = Result()
            val flag = buffer.readBoolean()
            if (flag) {
                res.tag = buffer.readUtf()
            } else {
                res.resultStack = ItemStack.STREAM_CODEC.decode(buffer)
            }

            return ResearchingRecipe(input, base, addition, special, selectable, color, time, res)
        }

        fun toNetwork(
            buffer: RegistryFriendlyByteBuf,
            recipe: ResearchingRecipe
        ) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.input)
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.base)
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.addition)
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.special)
            buffer.writeBoolean(recipe.selectable)
            buffer.writeInt(recipe.color)
            buffer.writeInt(recipe.time)

            val res = recipe.result
            val flag = res.isRandom()
            buffer.writeBoolean(flag)
            if (flag) {
                buffer.writeUtf(res.tag)
            } else {
                ItemStack.STREAM_CODEC.encode(buffer, res.getResult())
            }
        }

        override fun codec(): MapCodec<ResearchingRecipe> {
            return CODEC
        }

        override fun streamCodec(): StreamCodec<RegistryFriendlyByteBuf, ResearchingRecipe> {
            return STREAM_CODEC
        }
    }
}