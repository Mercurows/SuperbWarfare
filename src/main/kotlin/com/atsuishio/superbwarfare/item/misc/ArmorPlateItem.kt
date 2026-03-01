package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level

open class ArmorPlateItem : Item(Properties()) {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: Level?,
        pTooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        if (pStack.getOrCreateTag().getBoolean("Infinite")) {
            pTooltipComponents.add(
                Component.translatable("des.superbwarfare.armor_plate.infinite").withStyle(ChatFormatting.GRAY)
            )
        }
    }

    override fun use(worldIn: Level, playerIn: Player, handIn: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = playerIn.getItemInHand(handIn)
        val armor = playerIn.getItemBySlot(EquipmentSlot.CHEST)

        if (armor == ItemStack.EMPTY) return InteractionResultHolder.fail(stack)

        val armorLevel = if (armor.`is`(ModTags.Items.MILITARY_ARMOR)) {
            MiscConfig.MILITARY_ARMOR_LEVEL.get()
        } else if (armor.`is`(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
            MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get()
        } else {
            MiscConfig.DEFAULT_ARMOR_LEVEL.get()
        }

        if (armor.getOrCreateTag().getDouble("ArmorPlate") < armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get()) {
            playerIn.startUsingItem(handIn)
        }

        return InteractionResultHolder.fail(stack)
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.BOW
    }

    override fun finishUsingItem(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity): ItemStack {
        if (!pLevel.isClientSide) {
            val armor = pLivingEntity.getItemBySlot(EquipmentSlot.CHEST)

            val armorLevel = if (armor.`is`(ModTags.Items.MILITARY_ARMOR)) {
                MiscConfig.MILITARY_ARMOR_LEVEL.get()
            } else if (armor.`is`(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
                MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get()
            } else {
                MiscConfig.DEFAULT_ARMOR_LEVEL.get()
            }

            armor.getOrCreateTag().putDouble(
                "ArmorPlate",
                Mth.clamp(
                    armor.getOrCreateTag().getDouble("ArmorPlate") + MiscConfig.ARMOR_POINT_PER_LEVEL.get(),
                    0.0,
                    (armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get()).toDouble()
                )
            )

            if (pLivingEntity is ServerPlayer) {
                pLivingEntity.level().playSound(
                    null,
                    pLivingEntity.onPos,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    SoundSource.PLAYERS,
                    0.5f,
                    1f
                )
            }

            if (pLivingEntity is Player && !pLivingEntity.isCreative
                && !pStack.getOrCreateTag().getBoolean("Infinite")
            ) {
                pStack.shrink(1)
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity)
    }

    override fun getUseDuration(stack: ItemStack): Int {
        return 20
    }

    companion object {
        fun getInfiniteInstance(): ItemStack {
            val stack = ItemStack(ModItems.ARMOR_PLATE.get())
            stack.getOrCreateTag().putBoolean("Infinite", true)
            return stack
        }
    }
}