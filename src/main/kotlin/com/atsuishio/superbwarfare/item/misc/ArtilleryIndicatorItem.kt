package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.client.TooltipTool
import com.atsuishio.superbwarfare.client.screens.ArtilleryIndicatorScreen
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.NBTTool
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.*
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

open class ArtilleryIndicatorItem : Item(Properties().stacksTo(1).rarity(Rarity.UNCOMMON)), ItemScreenProvider {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        TooltipTool.addScreenProviderText(tooltipComponents)
        if (NBTTool.getTag(stack).contains(TAG_TYPE)) {
            tooltipComponents.add(
                Component.translatable(
                    "des.superbwarfare.artillery_indicator.type",
                    Component.translatable(
                        NBTTool.getTag(stack).getString(TAG_TYPE)
                    )
                )
                    .withStyle(ChatFormatting.WHITE)
            )
        }
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.artillery_indicator_1").withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.artillery_indicator_2").withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.artillery_indicator_3").withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.artillery_indicator_4").withStyle(ChatFormatting.GRAY)
        )
        tooltipComponents.add(Component.literal(" ").withStyle(ChatFormatting.GRAY))
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.artillery_indicator_5").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun getUseDuration(stack: ItemStack, entity: LivingEntity): Int {
        return 72000
    }

    override fun getUseAnimation(stack: ItemStack): UseAnim {
        return UseAnim.SPYGLASS
    }

    override fun use(pLevel: Level, pPlayer: Player, pHand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (pHand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(pPlayer.getItemInHand(pHand))
        }
        pPlayer.playSound(SoundEvents.SPYGLASS_USE, 1f, 1f)
        pPlayer.startUsingItem(pHand)
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pHand))
    }

    override fun finishUsingItem(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity): ItemStack {
        pLivingEntity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1f, 1f)
        return pStack
    }

    override fun releaseUsing(pStack: ItemStack, pLevel: Level, pLivingEntity: LivingEntity, pTimeCharged: Int) {
        pLivingEntity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1f, 1f)
    }

    fun checkFull(stack: ItemStack): Boolean {
        val tags = NBTTool.getTag(stack).getList(TAG_CANNON, Tag.TAG_COMPOUND.toInt())
        return tags.size >= MiscConfig.ARTILLERY_INDICATOR_LIST_SIZE.get()
    }

    fun addCannon(stack: ItemStack, entity: Entity): Boolean {
        val uuid = entity.getStringUUID()
        val tag = NBTTool.getTag(stack)
        val tags = tag.getList(TAG_CANNON, Tag.TAG_COMPOUND.toInt())
        if (tags.isEmpty()) {
            tag.putString(TAG_TYPE, entity.type.descriptionId)
        } else {
            if (tag.getString(TAG_TYPE) != entity.type.descriptionId) {
                return false
            }
        }

        val list: MutableList<CompoundTag> = arrayListOf()
        for (i in tags.indices) {
            list.add(tags.getCompound(i))
        }
        for (t in list) {
            if (t.getString("UUID") == uuid) {
                return false
            }
        }
        val uuidTag = CompoundTag()
        uuidTag.putString("UUID", uuid)
        list.add(uuidTag)

        val listTag = ListTag()
        listTag.addAll(list)
        tag.put(TAG_CANNON, listTag)
        NBTTool.saveTag(stack, tag)

        return true
    }

    fun removeCannon(stack: ItemStack, uuid: String?): Boolean {
        val tag = NBTTool.getTag(stack)
        val tags = tag.getList(TAG_CANNON, Tag.TAG_COMPOUND.toInt())
        val list: MutableList<CompoundTag> = arrayListOf()
        var flag = false
        for (i in tags.indices) {
            val tag = tags.getCompound(i)
            if (tag.getString("UUID") == uuid) {
                flag = true
                continue
            }
            list.add(tags.getCompound(i))
        }
        if (flag) {
            val listTag = ListTag()
            listTag.addAll(list)
            tag.put(TAG_CANNON, listTag)
            if (listTag.isEmpty()) {
                tag.remove(TAG_TYPE)
            }
            NBTTool.saveTag(stack, tag)
        }

        return flag
    }

    @OnlyIn(Dist.CLIENT)
    override fun getItemScreen(stack: ItemStack, player: Player, hand: InteractionHand): Screen {
        return ArtilleryIndicatorScreen(stack, hand)
    }

    fun setTarget(stack: ItemStack, player: Player) {
        val mainTag = NBTTool.getTag(stack)
        val tags = mainTag.getList(TAG_CANNON, Tag.TAG_COMPOUND.toInt())
        val list: MutableList<CompoundTag> = arrayListOf()

        for (i in tags.indices) {
            val tag = tags.getCompound(i)
            val entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"))

            if (entity is ArtilleryEntity) {
                list.add(tag)
                entity.setTarget(stack, player, "Main")
            }
        }

        if (list.size != tags.size) {
            val listTag = ListTag()
            listTag.addAll(list)
            mainTag.put(TAG_CANNON, listTag)
            if (listTag.isEmpty()) {
                mainTag.remove(TAG_TYPE)
            }
            NBTTool.saveTag(stack, mainTag)
        }
    }

    fun bind(stack: ItemStack, player: Player, entity: Entity): InteractionResult {
        if (this.checkFull(stack)) {
            player.displayClientMessage(
                Component.translatable("des.superbwarfare.artillery_indicator.full").withStyle(ChatFormatting.RED), true
            )
            return InteractionResult.FAIL
        }

        if (this.addCannon(stack, entity)) {
            if (player is ServerPlayer) {
                player.level()
                    .playSound(null, player.onPos, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 1f)
            }
            player.displayClientMessage(
                Component.translatable("des.superbwarfare.artillery_indicator.add", entity.displayName)
                    .withStyle(ChatFormatting.GREEN), true
            )
            return InteractionResult.SUCCESS
        } else if (this.removeCannon(stack, entity.getStringUUID())) {
            if (player is ServerPlayer) {
                player.level()
                    .playSound(null, player.onPos, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5f, 1f)
            }
            player.displayClientMessage(
                Component.translatable("des.superbwarfare.artillery_indicator.remove", entity.displayName)
                    .withStyle(ChatFormatting.RED), true
            )
            return InteractionResult.SUCCESS
        } else {
            player.displayClientMessage(
                Component.translatable("des.superbwarfare.artillery_indicator.fail", entity.displayName)
                    .withStyle(ChatFormatting.RED), true
            )
            return InteractionResult.FAIL
        }
    }

    companion object {
        const val TAG_CANNON: String = "Cannons"
        const val TAG_TYPE: String = "Type"
    }
}