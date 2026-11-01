package com.atsuishio.superbwarfare.perk.functional

import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.entity.player.PlayerXpEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object StrivingForPerfection : Perk("striving_for_perfection", Type.FUNCTIONAL) {

    /** 每 1 点经验能修复的耐久度：0 级 1 点，之后每 4 级再多 1 点 */
    fun repairPerXp(level: Int): Int = 1 + level / 4

    /** 武器经验获取倍率：0 级 +10%，之后每级再 +10% */
    fun expMultiplier(level: Int): Double = 1.0 + 0.1 * (level + 1)

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) = with(GunProp) {
        val level = modifier.data.perk.getLevel(this@StrivingForPerfection).toInt()
        modifier[EXP_MULTIPLIER] *= expMultiplier(level)
    }

    @SubscribeEvent
    fun onXpChange(event: PlayerXpEvent.XpChange) {
        val player = event.entity
        if (player.level().isClientSide) return

        var remaining = event.amount
        if (remaining <= 0) return

        for (stack in repairTargets(player)) {
            if (remaining <= 0) break
            if (stack.item !is GunItem) continue

            val data = GunData.from(stack)
            // getLevel 对"没装这个 perk"也返回 0，所以要先单独判断有没有装
            if (!data.perk.has(this)) continue

            // 修的是"已扣掉的耐久"：没损伤的枪（damageValue = 0）不该消耗经验
            val damage = stack.damageValue
            if (damage <= 0) continue

            val perXp = repairPerXp(data.perk.getLevel(this).toInt())
            val repaired = minOf(damage, remaining * perXp)
            stack.damageValue -= repaired
            // 只扣真正换来耐久的那部分经验（不足 1 点也按 1 点算，避免白修）
            remaining -= (repaired + perXp - 1) / perXp
        }

        event.amount = remaining.coerceAtLeast(0)
    }
    private fun repairTargets(player: Player): List<ItemStack> = buildList {
        add(player.mainHandItem)
        add(player.offhandItem)
        addAll(player.inventory.items)
    }
}
