package com.atsuishio.superbwarfare.init

import com.google.common.base.Suppliers
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootContext
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraftforge.common.loot.IGlobalLootModifier
import net.minecraftforge.common.loot.LootModifier
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.function.Supplier

@Mod.EventBusSubscriber(modid = com.atsuishio.superbwarfare.Mod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModLootModifier {
    val LOOT_MODIFIERS: DeferredRegister<Codec<out IGlobalLootModifier>> =
        DeferredRegister.create(
            ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
            com.atsuishio.superbwarfare.Mod.MODID
        )

    @JvmField
    val LOOT_MODIFIER: RegistryObject<Codec<TargetModLootTableModifier>> =
        LOOT_MODIFIERS.register<Codec<TargetModLootTableModifier>>(
            com.atsuishio.superbwarfare.Mod.MODID + "_loot_modifier", TargetModLootTableModifier.CODEC
        )

    @SubscribeEvent
    fun register(event: FMLConstructModEvent) {
        event.enqueueWork { LOOT_MODIFIERS.register(MOD_BUS) }
    }

    // 为什么还叫Target呢
    class TargetModLootTableModifier(conditions: Array<LootItemCondition>, private val lootTable: ResourceLocation) :
        LootModifier(conditions) {
        override fun doApply(
            generatedLoot: ObjectArrayList<ItemStack>,
            context: LootContext
        ): ObjectArrayList<ItemStack> {
            if (context.level.gameRules.getBoolean(ModGameRules.MOD_RULE_DO_GENERATE_LOOTS)) {
                context.resolver.getLootTable(lootTable).getRandomItemsRaw(context) { generatedLoot.add(it) }
            }
            return generatedLoot
        }

        override fun codec(): Codec<out IGlobalLootModifier> {
            return CODEC.get()
        }

        companion object {
            val CODEC: Supplier<Codec<TargetModLootTableModifier>> = Suppliers
                .memoize<Codec<TargetModLootTableModifier>> {
                    RecordCodecBuilder.create {
                        codecStart(it)
                            .and(
                                ResourceLocation.CODEC.fieldOf("lootTable")
                                    .forGetter { modifier -> modifier.lootTable })
                            .apply(it) { conditions, lootTable -> TargetModLootTableModifier(conditions, lootTable) }
                    }
                }
        }
    }
}