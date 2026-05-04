package com.atsuishio.superbwarfare.item

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.renderer.item.LungeMineRenderer
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModEnumExtensions
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.util.GeckoLibUtil

// 不要改这个东西，会肘击 YSM
open class LungeMine : Item(Properties().stacksTo(4)), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    fun getTransformType(type: ItemDisplayContext?) {
        transformType = type
    }

    private fun idlePredicate(event: AnimationState<LungeMine?>): PlayState? {
        val player = localPlayer ?: return PlayState.STOP
        if (ClientEventHandler.lungeSprint > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lunge_mine.sprint"))
        }

        if (ClientEventHandler.lungeDraw > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lunge_mine.draw"))
        }

        if (ClientEventHandler.lungeAttack > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.lunge_mine.fire"))
        }

        if (player.isSprinting && player.onGround() && ClientEventHandler.lungeDraw == 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lunge_mine.run"))
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lunge_mine.idle"))
    }

    override fun registerControllers(data: ControllerRegistrar) {
        val idleController = AnimationController<LungeMine>(
            this,
            "idleController",
            2
        ) { this.idlePredicate(it) }
        data.add(idleController)
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache? {
        return this.cache
    }

    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity, hand: InteractionHand): Boolean {
        return false
    }

    override fun shouldCauseReequipAnimation(oldStack: ItemStack, newStack: ItemStack, slotChanged: Boolean): Boolean {
        return false
    }

    override fun use(worldIn: Level, playerIn: Player, handIn: InteractionHand): InteractionResultHolder<ItemStack?> {
        val stack = playerIn.getItemInHand(handIn)
        if (playerIn is ServerPlayer) {
            playerIn.level()
                .playSound(null, playerIn.onPos, ModSounds.LUNGE_MINE_GROWL.get(), SoundSource.PLAYERS, 2f, 1f)
        }
        if (!playerIn.level().isClientSide()) {
            playerIn.addEffect(
                MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    100,
                    (if (playerIn.hasEffect(MobEffects.MOVEMENT_SPEED)) playerIn.getEffect(MobEffects.MOVEMENT_SPEED)!!
                        .amplifier else 0) + 2
                )
            )
        } else {
            ClientEventHandler.lungeSprint = 180
        }
        playerIn.cooldowns.addCooldown(stack.item, 300)
        return InteractionResultHolder.consume(stack)
    }

    override fun canAttackBlock(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player
    ): Boolean {
        return false
    }

    @EventBusSubscriber(modid = Mod.MODID)
    companion object {
        var transformType: ItemDisplayContext? = null

        @SubscribeEvent
        private fun registerItemExtensions(event: RegisterClientExtensionsEvent) {
            event.registerItem(object : IClientItemExtensions {
                private val renderer: BlockEntityWithoutLevelRenderer = LungeMineRenderer()

                override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                    return renderer
                }

                override fun getArmPose(
                    entityLiving: LivingEntity,
                    hand: InteractionHand,
                    itemStack: ItemStack
                ): ArmPose {
                    if (!itemStack.isEmpty) {
                        if (entityLiving.usedItemHand == hand) {
                            return ModEnumExtensions.Client.lungeMinePose
                        }
                    }
                    return ArmPose.EMPTY
                }
            }, ModItems.LUNGE_MINE)
        }
    }
}