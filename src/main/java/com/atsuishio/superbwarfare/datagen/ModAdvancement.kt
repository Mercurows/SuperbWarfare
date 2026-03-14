package com.atsuishio.superbwarfare.datagen

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.Mod.Companion.loc
import net.minecraft.ChatFormatting
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
import net.minecraft.world.level.storage.loot.LootTable
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
    private val group: Group

    init {

        val builtInBuilder = Builder()
        b.apply(builtInBuilder)
        this.group = builtInBuilder.group

        var bg: Optional<ResourceLocation> = Optional.empty()
        if (id == "root") {
            if (group == Group.MAIN) {
                bg = Optional.of(MAIN_BACKGROUND)
            }
            if (group == Group.LEGENDARY) {
                bg = Optional.of(LEGENDARY_BACKGROUND)
            }
        }

        builder.display(
            DisplayInfo(
                builtInBuilder.icon,
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
        return Mod.MODID + ".advancement." + group.path + "." + id
    }

    private fun titleComponent(): Component {
        if (this.group == Group.LEGENDARY && this.id != "root") {
            return Component.translatable(title()).withStyle(ChatFormatting.GOLD)
        }
        return Component.translatable(title())
    }

    private fun description(): String {
        return title() + ".des"
    }

    fun save(t: Consumer<AdvancementHolder>) {
        val parent = parent
        if (parent != null) {
            builder.parent(parent.result)
        }

        val advancementholder = builder.build(loc(group.path + "/" + id))
        t.accept(advancementholder)
        result = advancementholder
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

    enum class Group(val path: String) {
        MAIN("main"),
        LEGENDARY("legendary");
    }

    inner class Builder {
        var type = Type.DEFAULT
        private var keyIndex = 0
        var icon: ItemStack? = null
        var group = Group.MAIN

        fun type(type: Type): Builder {
            this.type = type
            return this
        }

        fun parent(other: ModAdvancement?): Builder {
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

        fun group(group: Group): Builder {
            this.group = group
            return this
        }

        fun rewardExp(exp: Int): Builder {
            builder.rewards(AdvancementRewards.Builder.experience(exp).build())
            return this
        }

        fun rewardLootTable(location: ResourceLocation): Builder {
            builder.rewards(
                AdvancementRewards.Builder.loot(
                    ResourceKey.create<LootTable?>(
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