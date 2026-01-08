package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.PerkItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.neoforged.neoforge.registries.DeferredHolder

class Perks(gun: GunData) {
    private val rootTag: CompoundTag = gun.perk()

    private fun getOrCreateList(type: Perk.Type): ListTag {
        val typeName = type.getName()
        return if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
        } else {
            ListTag().also { rootTag.put(typeName, it) }
        }
    }

    private fun findPerkByName(name: String): Perk? {
        val allEntries = ModPerks.AMMO_PERKS.entries +
                ModPerks.FUNC_PERKS.entries +
                ModPerks.DAMAGE_PERKS.entries

        return allEntries.firstOrNull { it.get().name == name }?.get()
    }

    fun has(perk: Perk): Boolean {
        val list = rootTag.getList(perk.type.getName(), Tag.TAG_COMPOUND.toInt())
        return list.any { (it as CompoundTag).getString("Name") == perk.name }
    }

    fun has(type: Perk.Type): Boolean {
        val list = rootTag.getList(type.getName(), Tag.TAG_COMPOUND.toInt())
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
    }

    fun set(instance: PerkInstance) {
        set(instance.perk, instance.level)
    }

    fun getLevel(perk: Perk): Short {
        val list = rootTag.getList(perk.type.getName(), Tag.TAG_COMPOUND.toInt())
        val entry = list.firstOrNull { (it as CompoundTag).getString("Name") == perk.name } as? CompoundTag
        return entry?.getShort("Level") ?: 0
    }

    fun getLevel(registry: DeferredHolder<Perk, out Perk>): Short = getLevel(registry.get())

    fun getLevel(item: PerkItem<*>): Short {
        return getLevel(item.perk)
    }

    fun getInstances(type: Perk.Type): List<PerkInstance> {
        val list = rootTag.getList(type.getName(), Tag.TAG_COMPOUND.toInt())
        val instances = mutableListOf<PerkInstance>()

        for (i in 0 until list.size) {
            val tag = list.getCompound(i)
            val name = tag.getString("Name")
            val level = tag.getShort("Level")

            // 这里的 findPerkByName 建议参考上一轮回复中的缓存优化
            val perk = findPerkByName(name)
            if (perk != null) {
                instances.add(PerkInstance(perk, level))
            }
        }
        return instances
    }

    fun get(registry: DeferredHolder<Perk, Perk>): Perk? {
        return get(registry.get())
    }

    fun get(perk: Perk): Perk? {
        return get(perk.type)
    }

    fun get(type: Perk.Type): Perk? {
        val list = rootTag.getList(type.getName(), Tag.TAG_COMPOUND.toInt())
        if (list.isEmpty()) return null
        return findPerkByName(list.getCompound(0).getString("Name"))
    }

    fun reduceCooldown(perk: Perk, cooldownKey: String) {
        val list = rootTag.getList(perk.type.getName(), Tag.TAG_COMPOUND.toInt())
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
        val typeName = perk.type.getName()
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
        rootTag.remove(type.getName())
    }

    // -----------Old--------------
//    fun has(perk: Perk): Boolean {
//        if (!has(perk.type)) return false
//        return getTag(perk).getString("Name") == perk.name
//    }
//
//    fun has(type: Perk.Type): Boolean {
//        return rootTag.contains(type.getName()) && !rootTag.getCompound(type.getName()).getString("Name").isEmpty()
//    }
//
//    fun set(instance: PerkInstance) {
//        set(instance.perk, instance.level)
//    }

    fun getTag(registry: DeferredHolder<Perk, out Perk>): CompoundTag {
        return getTag(registry.get().type ?: Perk.Type.AMMO)
    }

    fun getTag(perk: Perk): CompoundTag {
        return getTag(perk.type)
    }

    fun getTag(type: Perk.Type): CompoundTag {
        return rootTag.getCompound(type.getName())
    }

    fun getOrCreateTag(perk: Perk): CompoundTag {
        val typeTag: CompoundTag?
        val type = perk.type
        if (!rootTag.contains(type.getName())) {
            typeTag = CompoundTag()
            rootTag.put(type.getName(), typeTag)
        }
        return rootTag.getCompound(type.getName())
    }

//    fun set(perk: Perk, level: Short) {
//        getOrCreateTag(perk).putString("Name", perk.name)
//        getOrCreateTag(perk).putShort("Level", level)
//    }

//    fun getLevel(item: PerkItem): Short {
//        return getLevel(item.perk)
//    }
//
//    fun getLevel(perk: RegistryObject<Perk>): Short {
//        return getLevel(perk.get())
//    }
//
//    fun getLevel(perk: Perk): Short {
//        val name = perk.name
//        val tag = getTag(perk)
//        if (tag.getString("Name") != name) return 0
//        return getLevel(perk.type)
//    }
//
//    fun getLevel(type: Perk.Type): Short {
//        return getTag(type).getShort("Level")
//    }

//    fun get(type: Perk.Type): Perk? {
//        val perksRegistry = mutableListOf<RegistryObject<Perk>>()
//        perksRegistry.addAll(ModPerks.AMMO_PERKS.getEntries())
//        perksRegistry.addAll(ModPerks.FUNC_PERKS.getEntries())
//        perksRegistry.addAll(ModPerks.DAMAGE_PERKS.getEntries())
//
//        for (registry in perksRegistry) {
//            val name = getTag(type).getString("Name")
//            if (registry.get().name == name) {
//                return registry.get()
//            }
//        }
//        return null
//    }

//    fun getInstance(perk: Perk): PerkInstance? {
//        return getInstance(perk.type)
//    }
//
//    fun getInstance(type: Perk.Type): PerkInstance? {
//        val perk = get(type) ?: return null
//
//        return PerkInstance(perk, getLevel(type))
//    }

//    fun reduceCooldown(registry: RegistryObject<Perk>, name: String) {
//        reduceCooldown(registry.get(), name)
//    }
//
//    fun reduceCooldown(perk: Perk, name: String) {
//        reduceCooldown(perk.type, name)
//    }

//    fun reduceCooldown(type: Perk.Type, name: String) {
//        val tag = getTag(type)
//        var value = tag.getInt(name)
//        value--
//
//        if (value <= 0) {
//            tag.remove(name)
//        } else {
//            tag.putInt(name, value)
//        }
//    }

//    fun remove(perk: Perk) {
//        remove(perk.type)
//    }

//    fun remove(type: Perk.Type) {
//        rootTag.remove(type.getName())
//    }
}
