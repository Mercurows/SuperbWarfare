package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.tools.InventoryTool
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.items.IItemHandler
import kotlin.math.min

/**
 * 玩家弹药策略 — ammo 字符串形如 "@RifleAmmo"、 "@HandgunAmmo"
 *
 * match: 以 "@" 开头且后续内容非空
 * init: 从 "@RifleAmmo" 中手动提取 id "RifleAmmo"
 */
object PlayerAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.PLAYER_AMMO

    override fun match(ammo: String) = ammo.startsWith("@") && Ammo.getType(ammo.substringAfter("@")) != null

    override fun init(consumer: AmmoConsumer, count: Int, matchedString: String) {
        // 手动解析: matchedString 形如 "@RifleAmmo"
        val id = matchedString.substringAfter("@").trim()
        val ammoType = Ammo.getType(id)
        if (ammoType == null) {
            Mod.LOGGER.warn("invalid player ammo type: {}", id)
            consumer.type = AmmoConsumer.AmmoConsumeType.INVALID
            return
        }
        consumer.playerAmmoType = ammoType
        consumer.stack = ammoType.itemStack
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        var remaining = count
        var consumed = 0

        // 优先消耗玩家身上的弹药数据
        if (shooter is Player) {
            val ammoType = consumer.playerAmmoType
            if (ammoType != null) {
                val current = ammoType.get(shooter)
                consumed = min(current, remaining)
                remaining -= consumed
                ammoType.add(shooter, -consumed)
            } else {
                Mod.LOGGER.warn("consume player ammo failed: invalid player ammo type")
            }
        }

        // 如果还有剩余需要消耗的数量，从物品栏消耗
        val handler = shooter.getCapability(ForgeCapabilities.ITEM_HANDLER)
        if (handler.isPresent) {
            return consumed + consume(data, consumer, handler.resolve().get(), remaining)
        } else {
            Mod.LOGGER.warn("consume ammo failed: invalid item handler for entity {}", shooter)
            return consumed
        }
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int {
        val consumed = InventoryTool.consumeAmmoItem(handler, consumer.playerAmmoType, count)
        val rest = consumed - count
        data.virtualAmmo.add(rest)
        return count
    }

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?): Int {
        if (entity == null) return 0
        var playerAmmoCount = 0
        if (entity is Player) {
            playerAmmoCount = consumer.playerAmmoType?.get(entity) ?: 0
        }
        return playerAmmoCount + count(
            data,
            consumer,
            entity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null)
        )
    }

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?): Int {
        if (handler == null) return 0
        return InventoryTool.countAmmoItem(handler, consumer.playerAmmoType)
    }

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int): Int {
        if (ammoSupplier is Player) {
            val ammoType = consumer.playerAmmoType
            if (ammoType != null) {
                val countToWithdraw = min(count, ammoType.limit - ammoType.get(ammoSupplier))
                ammoType.add(ammoSupplier, countToWithdraw)

                val restItemCount = count - countToWithdraw
                if (restItemCount > 0) {
                    InventoryTool.insertItem(ammoSupplier, ammoType.itemStack, restItemCount)
                }
                return count
            } else {
                Mod.LOGGER.warn("withdraw player ammo failed: invalid player ammo type")
            }
        } else {
            val itemHandler = ammoSupplier.getCapability(ForgeCapabilities.ITEM_HANDLER)
            if (itemHandler.isPresent) {
                return withdraw(consumer, itemHandler.resolve().get(), count)
            } else {
                Mod.LOGGER.warn("withdraw ammo failed: invalid item handler")
            }
        }
        return 0
    }

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int {
        val ammoType = consumer.playerAmmoType ?: return 0
        return InventoryTool.insertItem(handler, ammoType.itemStack, count)
    }

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer): String {
        return consumer.playerAmmoType?.displayName ?: super.getDisplayName(consumer)
    }
}
