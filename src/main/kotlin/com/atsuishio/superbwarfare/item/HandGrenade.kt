package com.atsuishio.superbwarfare.item

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.projectile.HandGrenadeEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.projectile.AbstractProjectileDispenseBehavior
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.core.dispenser.DispenseItemBehavior
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
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
import kotlin.math.min

open class HandGrenade : Item(Properties().rarity(Rarity.UNCOMMON)), DispenserLaunchable {
    override fun use(worldIn: Level, playerIn: Player, handIn: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = playerIn.getItemInHand(handIn)
        playerIn.startUsingItem(handIn)
        if (playerIn is ServerPlayer) {
            playerIn.level().playSound(null, playerIn.onPos, ModSounds.GRENADE_PULL.get(), SoundSource.PLAYERS, 1f, 1f)
        }
        return InteractionResultHolder.consume(stack)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.SPEAR
    }

    override fun releaseUsing(stack: ItemStack, level: Level, living: LivingEntity, timeLeft: Int) {
        if (!level.isClientSide) {
            if (living is Player) {
                val usingTime = this.getUseDuration(stack, living) - timeLeft
                if (usingTime > 3) {
                    living.cooldowns.addCooldown(stack.item, 25)
                    val power = min(usingTime / 10.0f, 1.5f)

                    val handGrenade = HandGrenadeEntity(living, level)
                    handGrenade.setLife(100 - usingTime)
                    handGrenade.shootFromRotation(
                        living,
                        living.xRot,
                        living.yRot,
                        0.0f,
                        power,
                        0.0f
                    )
                    level.addFreshEntity(handGrenade)

                    if (level is ServerLevel) {
                        level.playSound(
                            null,
                            living.onPos,
                            ModSounds.GRENADE_THROW.get(),
                            SoundSource.PLAYERS,
                            1f,
                            1f
                        )
                    }

                    if (!living.isCreative) {
                        stack.shrink(1)
                    }
                }
            }
        }
    }

    override fun finishUsingItem(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity): ItemStack {
        if (!pLevel.isClientSide) {
            val handGrenade = HandGrenadeEntity(pLivingEntity, pLevel)

            CustomExplosion.Builder(handGrenade)
                .attacker(pLivingEntity)
                .damage(ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get().toFloat())
                .radius(ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get().toFloat())
                .damageMultiplier(1.25f)
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .explode()

            if (pLivingEntity is Player) {
                pLivingEntity.cooldowns.addCooldown(pStack.item, 25)
            }

            if (pLivingEntity is Player && !pLivingEntity.isCreative) {
                pStack.shrink(1)
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity)
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return 100
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                return HandGrenadeEntity(
                    ModEntities.HAND_GRENADE.get(),
                    position.x(),
                    position.y(),
                    position.z(),
                    level
                )
            }

            override fun playSound(source: BlockSource) {
                source.level.playSound(null, source.pos, ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}