package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.NuclearBombItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.NuclearBombEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.Tag
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent

open class NuclearBombItem(
    private val entityType: () -> EntityType<NuclearBombEntity>,
    private val modelId: String
) : Item(Properties().stacksTo(1).rarity(Rarity.EPIC)) {
    @EventBusSubscriber(value = [Dist.CLIENT])
    companion object {
        @SubscribeEvent
        fun registerItemExtensions(event: RegisterClientExtensionsEvent) {
            for (item in listOf(ModItems.FOOD_RAIN_ATOM_BOMB.get(), ModItems.SMELLY_BOY.get())) {
                event.registerItem(object : IClientItemExtensions {
                    private val renderer by lazy {
                        NuclearBombItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels, item.modelId)
                    }
                    override fun getCustomRenderer() = renderer
                }, item)
            }
        }
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!level.isClientSide) {
            val bomb = entityType().create(level) ?: return InteractionResultHolder.fail(stack)
            stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.let { tag ->
                if (tag.contains("Radius", Tag.TAG_ANY_NUMERIC.toInt())) bomb.setExplosionRadius(tag.getFloat("Radius"))
                if (tag.contains("ExplosionDamage", Tag.TAG_ANY_NUMERIC.toInt())) bomb.setExplosionDamage(tag.getFloat("ExplosionDamage"))
                if (tag.contains("ExplosionDestroy")) bomb.setExplosionDestroy(tag.getBoolean("ExplosionDestroy"))
                bomb.readNuclearEffectData(tag)
            }
            bomb.owner = player
            bomb.setPos(player.x, player.eyeY - 0.1, player.z)
            bomb.shootFromRotation(player, player.xRot, player.yRot, 0f, 2.5f, 0f)
            if (level.addFreshEntity(bomb)) {
                level.playSound(null, player.onPos, ModSounds.GRENADE_THROW.get(), SoundSource.PLAYERS, 1f, 1f)
                player.cooldowns.addCooldown(this, 10)
                player.awardStat(Stats.ITEM_USED.get(this))
                if (!player.isCreative) stack.shrink(1)
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
    }
}
