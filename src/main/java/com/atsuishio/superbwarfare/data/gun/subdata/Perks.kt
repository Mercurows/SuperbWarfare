package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.misc.PerkItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraftforge.registries.RegistryObject

class Perks(gun: GunData) {
    private val rootTag: CompoundTag = gun.perk()

    private fun findPerkByName(name: String): Perk? {
        val allEntries = ModPerks.AMMO_PERKS.entries +
                ModPerks.FUNC_PERKS.entries +
                ModPerks.DAMAGE_PERKS.entries

        return allEntries.firstOrNull { it.get().name == name }?.get()
    }

    fun getOrCreateList(type: Perk.Type): ListTag {
        val typeName = type.typeName
        return if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
        } else {
            val tag = rootTag.getCompound(typeName)
            ListTag().also { rootTag.put(typeName, tag) }
        }
    }

    fun has(perk: Perk): Boolean {
        val list = rootTag.getList(perk.type.typeName, Tag.TAG_COMPOUND.toInt())
        return list.any { (it as CompoundTag).getString("Name") == perk.name }
    }

    fun has(type: Perk.Type): Boolean {
        val list = rootTag.getList(type.typeName, Tag.TAG_COMPOUND.toInt())
        return !list.isEmpty()
    }

    fun set(perk: Perk, level: Short) {
        val list = getOrCreateList(perk.type)

        // 查找是否已存在同名 Perk
        val existing = list.firstOrNull { (it as CompoundTag).getString("Name") == perk.name } as? CompoundTag

        if (existing != null) {
            // 更新现有等级
            existing.putShort("Level", level)
        } else {
            // 添加新条目
            val newEntry = CompoundTag().apply {
                putString("Name", perk.name)
                putShort("Level", level)
            }
            list.add(newEntry)
        }
        rootTag.put(perk.type.typeName, list)
    }

    fun set(instance: PerkInstance) {
        set(instance.perk, instance.level)
    }

    fun getLevel(perk: Perk): Short {
        val name = perk.type.typeName
        if (rootTag.contains(name, Tag.TAG_COMPOUND.toInt())) {
            val tag = rootTag.getCompound(name)
            return tag.getShort("Level")
        }
        if (rootTag.contains(name, Tag.TAG_LIST.toInt())) {
            val list = rootTag.getList(name, Tag.TAG_COMPOUND.toInt())
            val entry = list.firstOrNull { (it as CompoundTag).getString("Name") == perk.name } as? CompoundTag
            return entry?.getShort("Level") ?: 0
        }
        return 0
    }

    fun getLevel(registry: RegistryObject<Perk>): Short = getLevel(registry.get())

    fun getLevel(item: PerkItem): Short {
        return getLevel(item.perk)
    }

    fun getInstances(type: Perk.Type): List<PerkInstance> {
        val typeName = type.typeName
        val instances = mutableListOf<PerkInstance>()
        if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            val list = rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val tag = list.getCompound(i)
                val name = tag.getString("Name")
                val level = tag.getShort("Level")

                val perk = findPerkByName(name)
                if (perk != null) {
                    instances.add(PerkInstance(perk, level))
                }
            }
        } else {
            val tag = rootTag.getCompound(typeName)
            val name = tag.getString("Name")
            val level = tag.getShort("Level")

            val perk = findPerkByName(name)
            if (perk != null) {
                instances.add(PerkInstance(perk, level))
            }
        }
        return instances
    }

    fun get(registry: RegistryObject<Perk>): Perk? {
        return get(registry.get())
    }

    fun get(perk: Perk): Perk? {
        return get(perk.type)
    }

    fun get(type: Perk.Type): Perk? {
        val typeName = type.typeName
        if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            val list = rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
            if (list.isEmpty()) return null
            return findPerkByName(list.getCompound(0).getString("Name"))
        } else {
            return findPerkByName(rootTag.getCompound(typeName).getString("Name"))
        }
    }

    fun reduceCooldown(perk: Perk, cooldownKey: String) {
        val list = rootTag.getList(perk.type.typeName, Tag.TAG_COMPOUND.toInt())
        val entry = list.firstOrNull { (it as CompoundTag).getString("Name") == perk.name } as? CompoundTag

        entry?.let { tag ->
            if (!tag.contains(cooldownKey)) return
            val newValue = tag.getInt(cooldownKey) - 1
            if (newValue <= 0) {
                tag.remove(cooldownKey)
            } else {
                tag.putInt(cooldownKey, newValue)
            }
        }
    }

    fun remove(perk: Perk) {
        val typeName = perk.type.typeName
        if (!rootTag.contains(typeName, Tag.TAG_LIST.toInt())) return

        val list = rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
        // 移除所有名称匹配的项
        list.removeIf { (it as CompoundTag).getString("Name") == perk.name }

        // 如果 List 空了，把整个 Type 节点删掉以节省空间
        if (list.isEmpty()) {
            rootTag.remove(typeName)
        }
    }

    fun removeAll(type: Perk.Type) {
        rootTag.remove(type.typeName)
    }

    fun getTag(registry: RegistryObject<Perk>): CompoundTag? {
        return getTag(registry.get())
    }

    fun getTag(perk: Perk): CompoundTag? {
        return getOrCreateList(perk.type).filterIsInstance<CompoundTag>()
            .firstOrNull { perk.name == it.getString("Name") }
    }

    fun getOrCreateTag(perk: Perk): CompoundTag {
        val typeTag: CompoundTag?
        val type = perk.type
        if (!rootTag.contains(type.typeName)) {
            typeTag = CompoundTag()
            rootTag.put(type.typeName, typeTag)
        }
        return rootTag.getCompound(type.typeName)
    }
}
