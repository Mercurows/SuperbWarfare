package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.FoodRainAtomBombItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.NuclearBombEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.nbt.Tag
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("food_rain_atom_bomb")
class FoodRainAtomBombItem : Item(Properties().stacksTo(1).rarity(Rarity.EPIC)) {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { FoodRainAtomBombItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels) }
            override fun getCustomRenderer() = renderer
        })
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)
        if (!level.isClientSide) {
            val bomb = NuclearBombEntity(ModEntities.FOOD_RAIN_ATOM_BOMB.get(), level)
            stack.tag?.let { tag ->
                if (tag.contains("Radius", Tag.TAG_ANY_NUMERIC.toInt())) bomb.setExplosionRadius(tag.getFloat("Radius"))
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
