package com.atsuishio.superbwarfare.perk.damage

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import com.atsuishio.superbwarfare.tools.FormatTool
import com.atsuishio.superbwarfare.tools.playLocalSound
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber
object BattleOfWits : Perk("battle_of_wits", Type.DAMAGE) {
    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        super.modifyProperty(modifier)
        val tag = modifier.data.perk.getTag(this) ?: return
        val rate = tag.getFloat("DamageRate")
        if (rate > 0f) {
            modifier[GunProp.DAMAGE] *= 1 + rate
        }
    }

    override fun onHit(
        attacker: LivingEntity,
        data: GunData,
        instance: PerkInstance,
        target: Entity
    ) {
        val tag = data.perk.getTag(this) ?: return
        tag.remove("DamageRate")
        super.onHit(attacker, data, instance, target)
    }

    override fun tick(
        data: GunData,
        instance: PerkInstance,
        entity: Entity?
    ) {
        super.tick(data, instance, entity)
        val tag = data.perk.getTag(this) ?: return
        if (tag.contains("DamageRate")) return

        data.perk.reduceCooldown(this, "Cooldown")

        if (entity is ServerPlayer && tag.getInt("Cooldown") == 1) {
            entity.displayClientMessage(
                Component.translatable("tips.superbwarfare.battle_of_wits.cooldown").withStyle(ChatFormatting.YELLOW),
                true
            )
            entity.playLocalSound(SoundEvents.ARROW_HIT_PLAYER)
        }
    }

    @SubscribeEvent
    fun onChatEvent(event: ServerChatEvent) {
        val player = event.player
        val stack = player.mainHandItem
        if (stack.item !is GunItem) return

        val text = event.rawText
        if (text.isEmpty()) return

        val data = GunData.from(stack)
        val level = data.perk.getLevel(this@BattleOfWits)
        if (level <= 0) return

        val tag = data.perk.getTag(this@BattleOfWits) ?: return
        if (tag.getInt("Cooldown") > 0) return

        val sub = text.take(20 + level * 4)
        val (count, length) = sub.toSet().size to sub.length

        val rate = (count * 8.5f + length * 7.5f) / 100f
        tag.putFloat("DamageRate", rate)
        tag.putInt("Cooldown", 100)

        player.displayClientMessage(
            Component.translatable(
                "tips.superbwarfare.battle_of_wits.active",
                "${FormatTool.format1DZ(rate * 100.0)}%"
            ).withStyle(ChatFormatting.LIGHT_PURPLE),
            false
        )
    }
}