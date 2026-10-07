package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.client.renderer.item.Type88ClusterGrenadesRenderer
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.projectile.Type88ClusterGrenadesEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.BlockSource
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
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
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer
import kotlin.math.min

@RegistryName("type_88_cluster_grenades")
open class Type88ClusterGrenadesItem : Item(Properties().rarity(Rarity.RARE)), DispenserLaunchable {
    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer = Type88ClusterGrenadesRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }

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
                val usingTime = this.getUseDuration(stack) - timeLeft
                if (usingTime > 3) {
                    living.cooldowns.addCooldown(stack.item, 25)
                    val power = min(usingTime / 10.0f, 1.5f)

                    val handGrenade = Type88ClusterGrenadesEntity(living, level)
                    handGrenade.setLife(ExplosionConfig.TYPE_88_CLUSTER_GRENADE_FUSE.get() - usingTime)
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
            val handGrenade = Type88ClusterGrenadesEntity(pLivingEntity, pLevel)

            CustomExplosion.Builder(handGrenade)
                .attacker(pLivingEntity)
                .damage(ExplosionConfig.TYPE_88_CLUSTER_GRENADE_EXPLOSION_DAMAGE.get().toFloat())
                .radius(ExplosionConfig.TYPE_88_CLUSTER_GRENADE_EXPLOSION_RADIUS.get().toFloat())
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

    override fun getUseDuration(stack: ItemStack): Int {
        return 100
    }

    override fun getLaunchBehavior(): DispenseItemBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getProjectile(pLevel: Level, pPosition: Position, pStack: ItemStack): Projectile {
                return Type88ClusterGrenadesEntity(
                    ModEntities.TYPE_88_CLUSTER_GRENADES.get(),
                    pPosition.x(),
                    pPosition.y(),
                    pPosition.z(),
                    pLevel
                )
            }

            override fun playSound(pSource: BlockSource) {
                pSource.level.playSound(null, pSource.pos, ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}