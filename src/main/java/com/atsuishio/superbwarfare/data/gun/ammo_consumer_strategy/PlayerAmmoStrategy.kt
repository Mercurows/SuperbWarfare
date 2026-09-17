package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.Ammo
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.tools.InventoryTool
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
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

    override fun init(source: AmmoSource, count: Int, matchedString: String) {
        // 手动解析: matchedString 形如 "@RifleAmmo"
        val id = matchedString.substringAfter("@").trim()
        val ammoType = Ammo.getType(id)
        if (ammoType == null) {
            Mod.LOGGER.warn("invalid player ammo type: {}", id)
            source.type = AmmoConsumer.AmmoConsumeType.INVALID
            return
        }
        source.playerAmmoType = ammoType
        source.stack = ammoType.itemStack
    }

    override fun consume(data: GunData, source: AmmoSource, shooter: Entity?, count: Int): Int {
        var remaining = count
        var consumed = 0

        // 优先消耗玩家身上的弹药数据
        if (shooter is Player) {
            val ammoType = source.playerAmmoType
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
        val handler = shooter?.getCapability(Capabilities.ItemHandler.ENTITY)
        if (handler != null) {
            return consumed + consume(data, source, handler, remaining)
        } else {
            Mod.LOGGER.warn("consume ammo failed: invalid item handler for entity {}", shooter)
            return consumed
        }
    }

    override fun consume(data: GunData, source: AmmoSource, handler: IItemHandler, count: Int): Int {
        val consumed = InventoryTool.consumeAmmoItem(handler, source.playerAmmoType, count)
        val rest = consumed - count
        data.virtualAmmo.add(rest)
        return count
    }

    override fun count(data: GunData, source: AmmoSource, entity: Entity?): Int {
        if (entity == null) return 0
        var playerAmmoCount = 0
        if (entity is Player) {
            playerAmmoCount = source.playerAmmoType?.get(entity) ?: 0
        }
        return playerAmmoCount + count(data, source, entity.getCapability(Capabilities.ItemHandler.ENTITY))
    }

    override fun count(data: GunData, source: AmmoSource, handler: IItemHandler?): Int {
        if (handler == null) return 0
        return InventoryTool.countAmmoItem(handler, source.playerAmmoType)
    }

    override fun withdraw(source: AmmoSource, ammoSupplier: Entity, count: Int): Int {
        if (ammoSupplier is Player) {
            val ammoType = source.playerAmmoType
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
            val itemHandler = ammoSupplier.getCapability(Capabilities.ItemHandler.ENTITY)
            if (itemHandler != null) {
                return withdraw(source, itemHandler, count)
            } else {
                Mod.LOGGER.warn("withdraw ammo failed: invalid item handler")
            }
        }
        return 0
    }

    override fun withdraw(source: AmmoSource, handler: IItemHandler, count: Int): Int {
        val ammoType = source.playerAmmoType ?: return 0
        return InventoryTool.insertItem(handler, ammoType.itemStack, count)
    }

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(source: AmmoSource): String {
        return source.playerAmmoType?.displayName ?: super.getDisplayName(source)
    }
}
