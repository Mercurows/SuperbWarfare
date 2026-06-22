package com.atsuishio.superbwarfare.item.armor

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.renderer.armor.HandsomeGogglesRenderer
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.resource.model.ArmorModelReloadListener
import com.atsuishio.superbwarfare.tiers.ModArmorMaterial
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.ChatFormatting
import net.minecraft.client.model.HumanoidModel
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer
import kotlin.math.cos
import kotlin.math.sin

class HandsomeGogglesItem :
    ArmorItem(ModArmorMaterial.STEEL, Type.HELMET, Properties().rarity(Rarity.EPIC).fireResistant()) {
    override fun isDamageable(stack: ItemStack) = false

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        consumer.accept(object : IClientItemExtensions {
            private var renderer: HandsomeGogglesRenderer? = null

            override fun getHumanoidArmorModel(
                livingEntity: LivingEntity?,
                itemStack: ItemStack?,
                equipmentSlot: EquipmentSlot?,
                original: HumanoidModel<*>?
            ): HumanoidModel<*> {
                if (this.renderer == null) {
                    this.renderer = HandsomeGogglesRenderer(ArmorModelReloadListener.getModel(MODEL)!!)
                }

                this.renderer!!.preparePose(livingEntity, itemStack, equipmentSlot, original)
                return this.renderer!!
            }
        })
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.handsome_goggles").withStyle(ChatFormatting.GRAY)
        )
        pTooltipComponents.add(
            Component.translatable("des.superbwarfare.handsome_goggles.warn").withStyle(ChatFormatting.RED)
        )
    }

    override fun use(pLevel: Level, pPlayer: Player, pUsedHand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = pPlayer.getItemInHand(pUsedHand)
        if (findGhastInSight(pPlayer) != null) {
            pPlayer.startUsingItem(pUsedHand)
            return InteractionResultHolder.consume(stack)
        }
        return InteractionResultHolder.pass(stack)
    }

    override fun onUseTick(pLevel: Level, pLivingEntity: LivingEntity, pStack: ItemStack, pRemainingUseDuration: Int) {
        if (pLivingEntity !is Player) return
        val target = findGhastInSight(pLivingEntity)
        if (target == null) {
            pLivingEntity.stopUsingItem()
            return
        }

        val useTick = pStack.useDuration - pRemainingUseDuration
        val requiredTicks = 100 // 5 seconds

        if (!pLevel.isClientSide) {
            val remainingSeconds = ((requiredTicks - useTick).coerceAtLeast(0)) / 20.0
            pLivingEntity.displayClientMessage(
                Component.literal("%.1fs".format(remainingSeconds)).withStyle(ChatFormatting.GREEN),
                true
            )
        }

        if (useTick >= requiredTicks && pLevel is ServerLevel) {
            pLivingEntity.stopUsingItem()
            val pos = target.position()

            pLevel.playSound(
                null, target.blockPosition(),
                ModSounds.DPS_GENERATOR_EVOLVE.get(), SoundSource.PLAYERS,
                1.5f, 1.2f
            )

            repeat(49) {
                val angle = Math.random() * Math.PI * 2
                val radius = 1.0 + Math.random() * 2.5
                val dx = cos(angle) * radius
                val dz = sin(angle) * radius
                val dy = (Math.random() - 0.5) * 5.0
                ParticleTool.sendParticle(
                    pLevel, ParticleTypes.REVERSE_PORTAL,
                    pos.x + dx, pos.y + dy + 2.0, pos.z + dz,
                    0, 0.0, 0.0, 0.0, 0.3, true
                )
            }

            ParticleTool.sendParticle(
                pLevel, ParticleTypes.FLASH,
                pos.x, pos.y + 2.0, pos.z,
                3, 0.2, 0.2, 0.2, 1.0, true
            )

            ParticleTool.sendParticle(
                pLevel, ParticleTypes.END_ROD,
                pos.x, pos.y + 1.5, pos.z,
                40, 1.5, 2.5, 1.5, 0.15, true
            )

            val happiestGhast = ModEntities.HAPPIEST_GHAST.get().create(pLevel) ?: return
            happiestGhast.moveTo(target.x, target.y, target.z, target.yRot, target.xRot)
            target.discard()
            pLevel.addFreshEntity(happiestGhast)

            if (!pLivingEntity.isCreative) {
                pStack.shrink(1)
            }
        }
    }

    override fun getUseDuration(pStack: ItemStack): Int {
        return 72000
    }

    companion object {
        val MODEL = loc("models/bedrock/armor/handsome_goggles.geo.json")

        private fun findGhastInSight(player: Player): Ghast? {
            val target = TraceTool.findLookingEntity(player, 5.0)
            return target as? Ghast
        }
    }
}