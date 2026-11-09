package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.DecoyGrenadeItemRenderer
import com.atsuishio.superbwarfare.entity.projectile.DecoyGrenadeEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

@RegistryName("decoy_grenade")
class DecoyGrenadeItem : Item(Properties().rarity(Rarity.UNCOMMON)), DispenserLaunchable {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        consumer.accept(object : IClientItemExtensions {
            private val renderer by lazy { DecoyGrenadeItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels) }
            override fun getCustomRenderer() = renderer
        })
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        player.startUsingItem(hand)
        if (!level.isClientSide) level.playSound(null, player.onPos, ModSounds.GRENADE_PULL.get(), SoundSource.PLAYERS, 1f, 1f)
        return InteractionResultHolder.consume(player.getItemInHand(hand))
    }

    override fun getUseAnimation(stack: ItemStack) = UseAnim.SPEAR
    override fun getUseDuration(stack: ItemStack) = 72000

    override fun releaseUsing(stack: ItemStack, level: Level, living: LivingEntity, timeLeft: Int) {
        if (level.isClientSide || living !is Player) return
        val held = getUseDuration(stack) - timeLeft
        if (held <= 3) return
        val grenade = DecoyGrenadeEntity(ModEntities.DECOY_GRENADE.get(), level)
        grenade.owner = living
        grenade.setPos(living.x, living.eyeY - 0.1, living.z)
        grenade.shootFromRotation(living, living.xRot, living.yRot, 0f, (held / 8f).coerceAtMost(1.6f), 0f)
        level.addFreshEntity(grenade)
        level.playSound(null, living.onPos, ModSounds.GRENADE_THROW.get(), SoundSource.PLAYERS, 1f, 1f)
        living.cooldowns.addCooldown(this, 20)
        if (!living.isCreative) stack.shrink(1)
    }

    override fun getLaunchBehavior() = object : AbstractProjectileDispenseBehavior() {
        override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile =
            DecoyGrenadeEntity(ModEntities.DECOY_GRENADE.get(), level).apply { setPos(position.x(), position.y(), position.z()) }
    }
}
