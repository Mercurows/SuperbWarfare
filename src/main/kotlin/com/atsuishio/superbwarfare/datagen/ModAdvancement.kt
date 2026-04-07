package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import net.minecraft.advancements.*
import net.minecraft.advancements.critereon.*
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import java.util.*
import java.util.function.Consumer
import java.util.function.UnaryOperator

/**
 * Codes Based on @Create
 */
@Suppress("unused")
class ModAdvancement(private val id: String, b: UnaryOperator<Builder>) {
    private val builder: Advancement.Builder = Advancement.Builder.advancement()
    private var parent: ModAdvancement? = null
    var result: AdvancementHolder? = null

    init {
        val builtInBuilder = Builder()
        b.apply(builtInBuilder)

        var bg: Optional<ResourceLocation> = Optional.empty()
        if (id == "root") {
            bg = Optional.of(MAIN_BACKGROUND)
        }

        builder.display(
            DisplayInfo(
                builtInBuilder.icon!!,
                titleComponent(),
                Component.translatable(description()),
                bg,
                builtInBuilder.type.frame,
                builtInBuilder.type.toast,
                builtInBuilder.type.announce,
                builtInBuilder.type.hide
            )
        )
    }

    private fun title(): String {
        return "${Mod.MODID}.advancement.main.$id"
    }

    private fun titleComponent(): Component {
        return Component.translatable(title())
    }

    private fun description(): String {
        return "${title()}.des"
    }

    fun save(t: Consumer<AdvancementHolder>) {
        if (parent != null) {
            builder.parent(parent!!.result!!)
        }

        val holder = builder.build(loc("main/$id"))
        t.accept(holder)
        result = holder
    }

    enum class Type(
        val frame: AdvancementType,
        val toast: Boolean,
        val announce: Boolean,
        val hide: Boolean
    ) {
        DEFAULT(AdvancementType.TASK, true, true, false),
        DEFAULT_NO_ANNOUNCE(AdvancementType.TASK, true, false, false),
        DEFAULT_CHALLENGE(AdvancementType.CHALLENGE, true, true, false),
        SILENT(AdvancementType.TASK, false, false, false),
        GOAL(AdvancementType.GOAL, true, true, false),
        SECRET(AdvancementType.TASK, true, true, true),
        SECRET_CHALLENGE(AdvancementType.CHALLENGE, true, true, true)
    }

    inner class Builder {
        var type = Type.DEFAULT
        private var keyIndex = 0
        var icon: ItemStack? = null

        fun type(type: Type): Builder {
            this.type = type
            return this
        }

        fun parent(other: ModAdvancement): Builder {
            this@ModAdvancement.parent = other
            return this
        }

        fun icon(item: ItemLike): Builder {
            return icon(ItemStack(item))
        }

        fun icon(stack: ItemStack): Builder {
            icon = stack
            return this
        }

        fun whenBlockPlaced(block: Block): Builder {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block))
        }

        fun whenIconCollected(): Builder {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(icon!!.item))
        }

        fun whenItemCollected(itemProvider: ItemLike): Builder {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider))
        }

        fun whenItemCollected(tag: TagKey<Item>): Builder {
            return externalTrigger(
                InventoryChangeTrigger.TriggerInstance
                    .hasItems(ItemPredicate.Builder.item().of(tag).build())
            )
        }

        fun whenItemConsumed(itemProvider: ItemLike): Builder {
            return externalTrigger(ConsumeItemTrigger.TriggerInstance.usedItem(itemProvider))
        }

        fun whenIconConsumed(): Builder {
            return externalTrigger(ConsumeItemTrigger.TriggerInstance.usedItem(icon!!.item))
        }

        fun awardedForFree(): Builder {
            return externalTrigger(PlayerTrigger.TriggerInstance.tick())
        }

        fun whenEffectChanged(predicate: MobEffectsPredicate.Builder): Builder {
            return externalTrigger(EffectsChangedTrigger.TriggerInstance.hasEffects(predicate))
        }

        fun externalTrigger(trigger: Criterion<*>): Builder {
            builder.addCriterion(keyIndex.toString(), trigger)
            keyIndex++
            return this
        }

        fun requirement(requirements: AdvancementRequirements): Builder {
            builder.requirements(requirements)
            return this
        }

        fun rewardExp(exp: Int): Builder {
            builder.rewards(AdvancementRewards.Builder.experience(exp).build())
            return this
        }

        fun rewardLootTable(location: ResourceLocation): Builder {
            builder.rewards(
                AdvancementRewards.Builder.loot(
                    ResourceKey.create(
                        Registries.LOOT_TABLE,
                        location
                    )
                ).build()
            )
            return this
        }
    }

    companion object {
        val MAIN_BACKGROUND: ResourceLocation = loc("textures/block/sandbag.png")
        val LEGENDARY_BACKGROUND: ResourceLocation = loc("textures/block/steel_block.png")
    }
}