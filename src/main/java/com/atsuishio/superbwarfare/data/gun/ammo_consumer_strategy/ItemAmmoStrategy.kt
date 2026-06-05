package com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.language.ClientLanguageGetter
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.tools.InventoryTool
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtUtils
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

/**
 * 物品弹药策略（兜底策略）— ammo 字符串形如 "minecraft:arrow"、 "mod:item"、 "mod:item{tag}"
 *
 * match: 非空白字符串均视为可能匹配（最后顺位，init 阶段再作验证）
 * init: 手动解析 id 和可选的 {data}
 */
object ItemAmmoStrategy : AmmoConsumeStrategy() {

    override val defaultType = AmmoConsumer.AmmoConsumeType.ITEM

    override fun match(ammo: String) = ammo.isNotBlank()

    override fun init(consumer: AmmoConsumer, count: Int, matchedString: String) {
        // 手动解析 id 和 data
        // matchedString 形如 "mod:item{tag}" 或 "minecraft:arrow"

        val id: String
        val data: String
        val braceIdx = matchedString.indexOf('{')
        if (braceIdx >= 0) {
            id = matchedString.substring(0, braceIdx).trim()
            data = matchedString.substring(braceIdx)
        } else {
            id = matchedString
            data = ""
        }

        val location = ResourceLocation.tryParse(id)
        if (location == null) {
            Mod.LOGGER.warn("invalid item id: {}", id)
            consumer.type = AmmoConsumer.AmmoConsumeType.INVALID
            return
        }
        val item = BuiltInRegistries.ITEM.get(location)
        if (item === Items.AIR) {
            Mod.LOGGER.warn("invalid item: {}", id)
            consumer.type = AmmoConsumer.AmmoConsumeType.INVALID
            return
        }

        consumer.stack = ItemStack(item)
        if (data.isNotEmpty()) {
            try {
                val tag = NbtUtils.snbtToStructure(data)
                tag.putString("id", location.toString())
                tag.putInt("count", 1)
                ItemStack.parse(RegistryAccess.EMPTY, tag).ifPresent { s -> consumer.stack = s }
            } catch (exception: Exception) {
                Mod.LOGGER.warn("invalid item data {}: {}", data, exception.message)
                consumer.type = AmmoConsumer.AmmoConsumeType.INVALID
                return
            }
        }
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, shooter: Entity, count: Int): Int {
        val handler = shooter.getCapability(Capabilities.ItemHandler.ENTITY)
        if (handler != null) {
            return consume(data, consumer, handler, count)
        } else {
            Mod.LOGGER.warn("consume ammo failed: invalid item handler for entity {}", shooter)
            return 0
        }
    }

    override fun consume(data: GunData, consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int {
        return InventoryTool.consumeItem(
            handler,
            { stack -> consumer.isAmmoItem(stack) },
            count
        )
    }

    override fun count(data: GunData, consumer: AmmoConsumer, entity: Entity?): Int {
        if (entity == null) return 0
        return count(data, consumer, entity.getCapability(Capabilities.ItemHandler.ENTITY))
    }

    override fun count(data: GunData, consumer: AmmoConsumer, handler: IItemHandler?): Int {
        if (handler == null) return 0
        return InventoryTool.countItem(handler) { stack -> consumer.isAmmoItem(stack) }
    }

    override fun withdraw(consumer: AmmoConsumer, ammoSupplier: Entity, count: Int): Int {
        if (ammoSupplier is Player) {
            InventoryTool.insertItem(ammoSupplier, consumer.stack(), count)
            return count
        } else {
            val itemHandler = ammoSupplier.getCapability(Capabilities.ItemHandler.ENTITY)
            if (itemHandler != null) {
                return withdraw(consumer, itemHandler, count)
            } else {
                Mod.LOGGER.warn("withdraw ammo failed: invalid item handler")
            }
        }
        return 0
    }

    override fun withdraw(consumer: AmmoConsumer, handler: IItemHandler, count: Int): Int {
        return InventoryTool.insertItem(handler, consumer.stack(), count)
    }

    @OnlyIn(Dist.CLIENT)
    override fun getDisplayName(consumer: AmmoConsumer): String {
        val stack = consumer.stack
        if (stack.isEmpty) return super.getDisplayName(consumer)
        val nameComponent = consumer.stack().hoverName
        val contents = nameComponent.contents
        if (contents is TranslatableContents) {
            return ClientLanguageGetter.EN_US.getOrDefault(contents.key)
        }

        return ClientLanguageGetter.EN_US.getOrDefault(consumer.stack().descriptionId)
    }
}
