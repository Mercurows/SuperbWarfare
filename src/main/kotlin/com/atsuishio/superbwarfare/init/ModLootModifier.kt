package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent
import net.neoforged.neoforge.common.loot.IGlobalLootModifier
import net.neoforged.neoforge.common.loot.LootModifier
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import java.util.function.Supplier
import javax.annotation.ParametersAreNonnullByDefault

@EventBusSubscriber(modid = Mod.MODID)
object ModLootModifier {
    @JvmStatic
    val LOOT_MODIFIERS: DeferredRegister<MapCodec<out IGlobalLootModifier>> =
        DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Mod.MODID)

    @JvmField
    val LOOT_MODIFIER: DeferredHolder<MapCodec<out IGlobalLootModifier>, MapCodec<TargetModLootTableModifier>> =
        LOOT_MODIFIERS.register(Mod.MODID + "_loot_modifier", Supplier { TargetModLootTableModifier.CODEC })

    @SubscribeEvent
    fun register(event: FMLConstructModEvent) {
        event.enqueueWork { LOOT_MODIFIERS.register(MOD_BUS) }
    }

    // 为什么还叫Target呢
    class TargetModLootTableModifier(
        conditions: Array<LootItemCondition>,
        private val lootTable: ResourceKey<LootTable>
    ) :
        LootModifier(conditions) {
        @ParametersAreNonnullByDefault
        override fun doApply(
            generatedLoot: ObjectArrayList<ItemStack>,
            context: LootContext
        ): ObjectArrayList<ItemStack> {
            if (context.level.gameRules.getBoolean(ModGameRules.MOD_RULE_DO_GENERATE_LOOTS)) {
                context.resolver.get(Registries.LOOT_TABLE, this.lootTable).ifPresent { table ->
                    table.value().getRandomItemsRaw(
                        context,
                        LootTable.createStackSplitter(context.level) { generatedLoot.add(it) }
                    )
                }
            }
            return generatedLoot
        }

        override fun codec(): MapCodec<out IGlobalLootModifier> {
            return LOOT_MODIFIER.get()
        }

        fun table(): ResourceKey<LootTable> {
            return this.lootTable
        }

        companion object {
            @JvmStatic
            val CODEC: MapCodec<TargetModLootTableModifier> = RecordCodecBuilder.mapCodec { instance ->
                instance.group(
                    LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter { it.conditions },
                    ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("table").forGetter { it.table() }
                ).apply(instance) { conditions, lootTable -> TargetModLootTableModifier(conditions, lootTable) }
            }

        }
    }
}