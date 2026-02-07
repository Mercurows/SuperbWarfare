package com.atsuishio.superbwarfare.item.common.ammo

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
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.max
import kotlin.math.min

class AmmoBoxItem : Item(Properties().stacksTo(1)) {
    // TODO 优化这一坨反人类逻辑
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(hand)

        if (hand == InteractionHand.OFF_HAND) return InteractionResultHolder.fail(stack)

        player.cooldowns.addCooldown(this, 10)

        val tag = stack.getOrCreateTag()
        val selectedType = tag.getString("Type").ifEmpty { "All" }

        if (!level.isClientSide()) {
            val types = buildList {
                if (selectedType == "All" || tag.getBoolean("IsDrop")) {
                    addAll(Ammo.entries.toTypedArray())
                } else {
                    add(Ammo.getType(selectedType))
                }
            }.filterNotNull()

            for (type in types) {
                if (player.isCrouching && !tag.getBoolean("IsDrop")) {
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
            if (tag.getBoolean("IsDrop") && Ammo.entries.all { it.get(stack) <= 0 }) {
                stack.shrink(1)
            }
        }
        return InteractionResultHolder.consume(stack)
    }

    @ParametersAreNonnullByDefault
    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity): Boolean {
        if (entity.isCrouching && entity is ServerPlayer) {
            val tag = stack.getOrCreateTag()
            if (tag.getBoolean("IsDrop")) return false

            val index = max(0, AMMO_TYPE_LIST.indexOf(tag.getString("Type")))
            val typeString = AMMO_TYPE_LIST[(index + 1) % AMMO_TYPE_LIST.size]

            tag.putString("Type", typeString)
            SoundTool.playLocalSound(entity, ModSounds.FIRE_RATE.get(), SoundSource.PLAYERS, 1f, 1f)
            val type = Ammo.getType(typeString)
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
        val type = Ammo.getType(stack.getOrCreateTag().getString("Type"))

        tooltipComponents.add(Component.translatable("des.superbwarfare.ammo_box").withStyle(ChatFormatting.GRAY))

        for (ammo in Ammo.entries) {
            tooltipComponents.add(
                Component.translatable("des.superbwarfare.ammo_box." + ammo.name).withStyle(ammo.color)
                        + Component.empty().withStyle(ChatFormatting.RESET)
                        + Component.literal(format0D(ammo.get(stack).toDouble()) + (if (type != ammo) " " else " ←-"))
                    .withStyle(ChatFormatting.BOLD)
            )
        }
    }

    companion object {
        private val AMMO_TYPE_LIST = generateAmmoTypeList()

        private fun generateAmmoTypeList() = buildList {
            add("All")

            for (ammoType in Ammo.entries) {
                add(ammoType.serializationName)
            }
        }
    }

    override fun overrideOtherStackedOnMe(stack: ItemStack, pOther: ItemStack, slot: Slot, action: ClickAction, player: Player, access: SlotAccess): Boolean {
        return super.overrideOtherStackedOnMe(stack, pOther, slot, action, player, access)
    }

    override fun overrideStackedOnOther(stack: ItemStack, slot: Slot, action: ClickAction, player: Player): Boolean {
        if (action == ClickAction.SECONDARY) {
            val slotItem = slot.item
            val tag = stack.getOrCreateTag()
            val selectedType = tag.getString("Type").ifEmpty { "All" }

            val types = buildList {
                if (selectedType == "All" || tag.getBoolean("IsDrop")) {
                    addAll(Ammo.entries.toTypedArray())
                } else {
                    add(Ammo.getType(selectedType))
                }
            }.filterNotNull()

            if (slotItem.isEmpty) {
                for (type in types) {
                    val storedCount = type.get(stack)
                    if (storedCount == 0) return false

                    val countToStore = storedCount.coerceAtMost(type.itemStack.maxStackSize)

                    for (i in 0..<countToStore) {
                        slot.safeInsert(type.itemStack)
                    }

                    type.add(stack, -countToStore)

                    player.playSound(ModSounds.FIRE_RATE.get())

                    return true
                }
                return false
            }

            val ammo = slotItem.item
            if (ammo is AmmoSupplierItem) {
                val ammoType = ammo.type
                val addCount = slotItem.count.coerceAtMost(ammoType.limit - ammoType.get(stack))
                ammoType.add(stack, addCount)
                slot.safeTake(slotItem.count, addCount, player)

                player.playSound(ModSounds.BULLET_SUPPLY.get())

                return true
            }
        }

        return false
    }
}
