package com.atsuishio.superbwarfare.item

import com.atsuishio.superbwarfare.client.renderer.item.LungeMineRenderer
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.localPlayer
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import software.bernie.geckolib.animatable.GeoItem
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.core.animation.AnimatableManager
import software.bernie.geckolib.core.animation.AnimationController
import software.bernie.geckolib.core.animation.AnimationState
import software.bernie.geckolib.core.animation.RawAnimation
import software.bernie.geckolib.core.`object`.PlayState
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.function.Consumer

// 不要改这个东西，会肘击 YSM
open class LungeMine : Item(Properties().stacksTo(4)), GeoItem {
    private val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    override fun initializeClient(consumer: Consumer<IClientItemExtensions>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private val renderer: BlockEntityWithoutLevelRenderer = LungeMineRenderer()

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                return renderer
            }

            private val LUNGE_MINE_POSE: HumanoidModel.ArmPose = HumanoidModel.ArmPose.create(
                "LungeMine",
                false
            ) { model, _, arm ->
                if (arm != HumanoidArm.LEFT) {
                    model!!.rightArm.xRot = 20f * Mth.DEG_TO_RAD + model.head.xRot
                    model.rightArm.yRot = -12f * Mth.DEG_TO_RAD
                    model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot
                    model.leftArm.yRot = 40f * Mth.DEG_TO_RAD
                }
            }

            override fun getArmPose(
                entityLiving: LivingEntity,
                hand: InteractionHand?,
                itemStack: ItemStack
            ): HumanoidModel.ArmPose {
                return if (!itemStack.isEmpty && entityLiving.usedItemHand == hand) {
                    LUNGE_MINE_POSE
                } else {
                    HumanoidModel.ArmPose.EMPTY
                }
            }
        })
    }

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

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) {
        val idleController = AnimationController<LungeMine>(
            this,
            "idleController",
            2
        ) { this.idlePredicate(it) }
        data.add(idleController)
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return this.cache
    }

    override fun onEntitySwing(stack: ItemStack?, entity: LivingEntity?): Boolean {
        return false
    }

    override fun shouldCauseReequipAnimation(
        oldStack: ItemStack?,
        newStack: ItemStack?,
        slotChanged: Boolean
    ): Boolean {
        return false
    }

    override fun use(level: Level, playerIn: Player, handIn: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = playerIn.getItemInHand(handIn)
        if (playerIn is ServerPlayer) {
            level.playSound(null, playerIn.onPos, ModSounds.LUNGE_MINE_GROWL.get(), SoundSource.PLAYERS, 2f, 1f)
        }
        if (!playerIn.level().isClientSide()) {
            playerIn.addEffect(
                MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    100,
                    (if (playerIn.hasEffect(MobEffects.MOVEMENT_SPEED)) {
                        playerIn.getEffect(MobEffects.MOVEMENT_SPEED)!!.amplifier
                    } else 0) + 2
                )
            )
        } else {
            ClientEventHandler.lungeSprint = 180
        }
        playerIn.cooldowns.addCooldown(stack.item, 300)
        return InteractionResultHolder.consume(stack)
    }

    override fun canAttackBlock(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean {
        return false
    }

    companion object {
        var transformType: ItemDisplayContext? = null
    }
}