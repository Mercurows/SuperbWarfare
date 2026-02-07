package com.atsuishio.superbwarfare.item.common.ammo

import com.atsuishio.superbwarfare.component.ModDataComponents
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.init.ModAttachments
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.common.ammo.box.AmmoBoxInfo
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.SoundTool
import com.atsuishio.superbwarfare.tools.getOrCreateTag
import com.atsuishio.superbwarfare.tools.plus
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.LivingEntity
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

        val info = stack.get(ModDataComponents.AMMO_BOX_INFO) ?: AmmoBoxInfo("All", false)
        val selectedType = info.type

        val cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch()
        if (!level.isClientSide()) {
            val types = buildList {
                if (selectedType == "All" || info.isDrop) {
                    addAll(Ammo.entries.toTypedArray())
                } else {
                    add(Ammo.getType(selectedType))
                }
            }.filterNotNull()

            for (type in types) {
                if (player.isCrouching && !info.isDrop) {
                    // 存入弹药
                    val storedCount = type.get(cap)
                    val countToStore = min(storedCount, type.ammoBoxLimit - type.get(stack)).coerceAtLeast(0)

                    type.add(stack, countToStore)
                    type.add(cap, -countToStore)
                } else {
                    // 取出弹药
                    val storedCount = type.get(stack)
                    val countToStore = min(storedCount, type.limit - type.get(cap)).coerceAtLeast(0)

                    type.add(cap, countToStore)
                    type.add(stack, -countToStore)
                }
            }
            player.setData(ModAttachments.PLAYER_VARIABLE, cap)
            cap.sync(player)
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1f, 1f)

            // 取出弹药时，若弹药盒为掉落物版本，则移除弹药盒物品
            if (info.isDrop && Ammo.entries.all { it.get(stack) <= 0 }) {
                stack.shrink(1)
            }
        }
        return InteractionResultHolder.consume(stack)
    }

    @ParametersAreNonnullByDefault
    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity, hand: InteractionHand): Boolean {
        if (entity.isCrouching && entity is ServerPlayer) {
            val info: AmmoBoxInfo = checkNotNull(
                if (stack.get(ModDataComponents.AMMO_BOX_INFO) == null) AmmoBoxInfo(
                    "All",
                    false
                ) else stack.get(ModDataComponents.AMMO_BOX_INFO)
            )
            if (info.isDrop) return false

            val index = max(0, AMMO_TYPE_LIST.indexOf(info.type))
            val typeString = AMMO_TYPE_LIST[(index + 1) % AMMO_TYPE_LIST.size]

            stack.set(ModDataComponents.AMMO_BOX_INFO, AmmoBoxInfo(typeString, false))
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
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        val info = stack.get(ModDataComponents.AMMO_BOX_INFO) ?: AmmoBoxInfo("All", false)
        val type = Ammo.getType(info.type)

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

                    repeat(countToStore) {
                        slot.safeInsert(type.itemStack)
                    }

                    type.add(stack, -countToStore)

                    if (player is ServerPlayer) {
                        SoundTool.playLocalSound(player, ModSounds.FIRE_RATE.get(), SoundSource.PLAYERS, 1f, 1f)
                    }

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

                if (player is ServerPlayer) {
                    SoundTool.playLocalSound(player, ModSounds.BULLET_SUPPLY.get(), SoundSource.PLAYERS, 1f, 1f)
                }

                return true
            }
        }

        return false
    }
}
