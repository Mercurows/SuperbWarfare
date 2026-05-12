package com.atsuishio.superbwarfare.item.ammo

import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ClickAction
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import kotlin.math.min

var ItemStack.ammoBoxData: AmmoBoxItem.AmmoBoxData
    get() {
        val tag = getOrCreateTag()
        val map = Ammo.entries.associateWith { it.get(tag) }.filterValues { it > 0 }
        return AmmoBoxItem.AmmoBoxData(Ammo.getType(tag.getString("Type")), tag.getBoolean("IsDrop"), map)
    }
    set(value) {
        if (value == this) return

        val tag = getOrCreateTag()
        tag.putBoolean("IsDrop", value.isDrop)
        tag.putString("Type", value.type?.serializationName ?: "All")

        value.storedAmmo.forEach { (ammo, count) ->
            ammo.set(tag, count)
        }
    }

open class AmmoBoxItem : Item(Properties().stacksTo(1)) {
    data class AmmoBoxData(
        val selectedType: Ammo? = null,
        val isDrop: Boolean = false,
        val storedAmmo: Map<Ammo, Int> = mapOf(),
    ) {
        val type = if (isDrop) null else selectedType
        val selectedTypes get() = if (type == null) Ammo.entries.toTypedArray() else arrayOf(type)

        val selectedAmmoCount get() = storedAmmo[type] ?: 0
        fun restCount(type: Ammo) = type.ammoBoxLimit - (storedAmmo[type] ?: 0)

        fun switchToNextType(): AmmoBoxData {
            if (isDrop) return this

            if (type == null) {
                return this.copy(selectedType = Ammo.entries[0])
            }

            if (type.ordinal == Ammo.entries.size - 1) {
                return this.copy(selectedType = null)
            }

            return this.copy(selectedType = Ammo.entries[type.ordinal + 1])
        }

        fun asDrop(): AmmoBoxData = copy(selectedType = null, isDrop = true)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.fail(stack)

        player.cooldowns.addCooldown(this, 10)

        val info = stack.ammoBoxData

        if (!level.isClientSide()) {
            for (type in info.selectedTypes) {
                if (player.isCrouching && !info.isDrop) {
                    // 存入弹药
                    val storedCount = type.get(player)
                    val countToStore = min(storedCount, type.ammoBoxLimit - type.get(stack)).coerceAtLeast(0)

                    type.add(stack, countToStore)
                    type.add(player, -countToStore)
                } else {
                    // 取出弹药
                    val storedCount = type.get(stack)
                    val countToStore = min(storedCount, type.limit - type.get(player)).coerceAtLeast(0)

                    type.add(player, countToStore)
                    type.add(stack, -countToStore)
                }
            }

            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1f, 1f)

            // 取出弹药时，若弹药盒为掉落物版本，则移除弹药盒物品
            if (info.isDrop && Ammo.entries.all { it.get(stack) <= 0 }) {
                stack.shrink(1)
            }
        }
        return InteractionResultHolder.consume(stack)
    }

    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity): Boolean {
        if (entity.isCrouching && entity is ServerPlayer) {
            stack.ammoBoxData = stack.ammoBoxData.switchToNextType()

            SoundTool.playLocalSound(entity, ModSounds.FIRE_RATE.get(), SoundSource.PLAYERS, 1f, 1f)
            val type = stack.ammoBoxData.type
            if (type == null) {
                entity.displayClientMessage(
                    Component.translatable("des.superbwarfare.ammo_box.type.all").withStyle(ChatFormatting.WHITE), true
                )
                return true
            }

            entity.displayClientMessage(
                Component.translatable("des.superbwarfare.ammo_box.type." + type.name).withStyle(type.color),
                true
            )
        }

        return true
    }

    override fun appendHoverText(
        stack: ItemStack,
        level: Level?,
        tooltipComponents: MutableList<Component>,
        pIsAdvanced: TooltipFlag
    ) {
        val type = stack.ammoBoxData.type

        tooltipComponents.add(Component.translatable("des.superbwarfare.ammo_box").withStyle(ChatFormatting.GRAY))

        for (ammo in Ammo.entries) {
            tooltipComponents.add(
                Component.translatable("des.superbwarfare.ammo_box." + ammo.name).withStyle(ammo.color)
                        + Component.empty().withStyle(ChatFormatting.RESET)
                        + Component.literal(format0D(ammo.get(stack).toDouble()) + (if (type != null && type != ammo) " " else " ←-"))
                    .withStyle(ChatFormatting.BOLD)
            )
        }
    }

    // 直接在物品栏右键时切换选中的弹种
    override fun overrideOtherStackedOnMe(
        stack: ItemStack,
        other: ItemStack,
        slot: Slot,
        action: ClickAction,
        player: Player,
        access: SlotAccess
    ): Boolean {
        val info = stack.ammoBoxData
        if (!info.isDrop && other.isEmpty && action == ClickAction.SECONDARY) {
            stack.ammoBoxData = stack.ammoBoxData.switchToNextType()
            player.playSound(ModSounds.FIRE_RATE.get())
            return true
        }
        return super.overrideOtherStackedOnMe(stack, other, slot, action, player, access)
    }

    override fun overrideStackedOnOther(stack: ItemStack, slot: Slot, action: ClickAction, player: Player): Boolean {
        val slotStack = slot.item
        val slotItem = slotStack.item
        val info = stack.ammoBoxData
        val count = min(slotStack.maxStackSize, slotStack.count)

        // 右键放弹药
        if (action == ClickAction.SECONDARY &&
            (slotStack.isEmpty || slotItem is AmmoSupplierItem && (slotItem.type == info.type || info.type == null))
        ) {
            val type = info.type ?: (slotItem as? AmmoSupplierItem)?.type ?: return false
            val newItem = if (slotStack.isEmpty) type.item else slotItem as AmmoSupplierItem
            val newStack = if (slotStack.isEmpty) type.itemStack else slotStack.copy()
            val currentStackCount = if (slotStack.isEmpty) 0 else count

            val currentCount = info.selectedAmmoCount
            val countToStore = min(newStack.maxStackSize - currentStackCount, currentCount / newItem.ammoToAdd)

            if (countToStore > 0) {
                slot.safeInsert(newStack.copyWithCount(countToStore))
                type.add(stack, -countToStore * newItem.ammoToAdd)

                player.playSound(ModSounds.FIRE_RATE.get())
                return true
            }
        }

        // 左键收弹药
        if (!info.isDrop && action == ClickAction.PRIMARY && slotItem is AmmoSupplierItem) {
            val type = slotItem.type
            val addCount = (info.restCount(type) / slotItem.ammoToAdd).coerceAtMost(count)
            if (addCount < 0) return true

            type.add(stack, addCount * slotItem.ammoToAdd)
            slot.safeTake(count, addCount, player)

            player.playSound(ModSounds.BULLET_SUPPLY.get())
            return true
        }

        return false
    }
}
