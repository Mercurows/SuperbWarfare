package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.registerToEventBus
import com.atsuishio.superbwarfare.tools.getOrCreateTag
import com.atsuishio.superbwarfare.tools.tag
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent

@RegistryName("sonic_absorber")
object SonicAbsorberItem : Item(Properties().rarity(Rarity.EPIC)) {

    init {
        registerToEventBus(this)
    }

    const val PARRY_TICK_TAG = "SonicParryTicks"
    const val PARRY_TICKS = 6
    const val PARRY_FAIL_COOLDOWN_TICKS = 80
    const val PARRY_SUCCESS_COOLDOWN_TICKS = 20

    override fun use(
        level: Level,
        player: Player,
        usedHand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
        val stack = player.getItemInHand(usedHand)
        if (level.isClientSide) return InteractionResultHolder.fail(stack)
        if (usedHand != InteractionHand.MAIN_HAND) return InteractionResultHolder.fail(stack)
        if (stack.item != this) return InteractionResultHolder.fail(stack)

        if ((stack.tag?.getInt(PARRY_TICK_TAG) ?: 0) > 0) return InteractionResultHolder.fail(stack)

        val tag = stack.getOrCreateTag()
        tag.putInt(PARRY_TICK_TAG, PARRY_TICKS)
        stack.tag = tag

        player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP)

        return super.use(level, player, usedHand)
    }

    override fun inventoryTick(
        stack: ItemStack,
        level: Level,
        entity: Entity,
        slotId: Int,
        isSelected: Boolean
    ) {
        if (level.isClientSide) return

        val tag = stack.tag
        val parryTicks = tag?.getInt(PARRY_TICK_TAG)?.takeIf { it > 0 } ?: return

        if (parryTicks == 1) {
            tag.remove(PARRY_TICK_TAG)
            stack.tag = tag
            (entity as? Player)?.cooldowns?.addCooldown(ModItems.SONIC_ABSORBER.get(), PARRY_FAIL_COOLDOWN_TICKS)
        } else {
            (entity as? Player)?.cooldowns?.addCooldown(ModItems.SONIC_ABSORBER.get(), PARRY_SUCCESS_COOLDOWN_TICKS)
            tag.putInt(PARRY_TICK_TAG, parryTicks - 1)
            stack.tag = tag
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected)
    }

    @SubscribeEvent
    fun onEntityAttackedBySonicBoom(event: LivingIncomingDamageEvent) {
        val source = event.source
        if (!source.`is`(DamageTypes.SONIC_BOOM)) return

        val entity = event.entity
        val stack = entity.mainHandItem
        val tag = stack.tag
        if (stack.item != this || tag == null || tag.getInt(PARRY_TICK_TAG) <= 0) return

        event.isCanceled = true
        tag.remove(PARRY_TICK_TAG)
        stack.tag = tag

        entity.playSound(SoundEvents.ARROW_HIT)
        source.entity?.hurt(entity.damageSources().sonicBoom(entity), event.originalAmount * 15)
    }
}
