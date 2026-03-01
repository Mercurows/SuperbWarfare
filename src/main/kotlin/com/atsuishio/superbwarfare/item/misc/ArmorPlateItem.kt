package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.getOrCreateTag
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import javax.annotation.ParametersAreNonnullByDefault

open class ArmorPlateItem : Item(Properties()) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        if (NBTTool.getTag(stack).getBoolean("Infinite")) {
            tooltipComponents.add(
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

    @ParametersAreNonnullByDefault
    override fun finishUsingItem(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity): ItemStack {
        if (!pLevel.isClientSide) {
            val armor = pLivingEntity.getItemBySlot(EquipmentSlot.CHEST)

            var armorLevel = MiscConfig.DEFAULT_ARMOR_LEVEL.get()
            if (armor.`is`(ModTags.Items.MILITARY_ARMOR)) {
                armorLevel = MiscConfig.MILITARY_ARMOR_LEVEL.get()
            } else if (armor.`is`(ModTags.Items.MILITARY_ARMOR_HEAVY)) {
                armorLevel = MiscConfig.HEAVY_MILITARY_ARMOR_LEVEL.get()
            }

            val tag = NBTTool.getTag(armor)
            tag.putDouble(
                "ArmorPlate",
                Mth.clamp(
                    tag.getDouble("ArmorPlate") + MiscConfig.ARMOR_POINT_PER_LEVEL.get(),
                    0.0,
                    (armorLevel * MiscConfig.ARMOR_POINT_PER_LEVEL.get()).toDouble()
                )
            )
            NBTTool.saveTag(armor, tag)

            if (pLivingEntity is ServerPlayer) {
                pLivingEntity.level().playSound(
                    null as Entity?,
                    pLivingEntity.onPos,
                    SoundEvents.ARMOR_EQUIP_IRON.value(),
                    SoundSource.PLAYERS,
                    0.5f,
                    1f
                )
            }

            if (pLivingEntity is Player && !pLivingEntity.isCreative && !NBTTool.getTag(pStack)
                    .getBoolean("Infinite")
            ) {
                pStack.shrink(1)
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity)
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return 20
    }

    companion object {
        fun getInfiniteInstance(): ItemStack {
            val stack = ItemStack(ModItems.ARMOR_PLATE.get())
            val tag = NBTTool.getTag(stack)
            tag.putBoolean("Infinite", true)
            NBTTool.saveTag(stack, tag)
            return stack
        }
    }
}