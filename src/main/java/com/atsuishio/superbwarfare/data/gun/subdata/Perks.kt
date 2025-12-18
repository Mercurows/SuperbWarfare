package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModPerks
import com.atsuishio.superbwarfare.item.PerkItem
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.perk.PerkInstance
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.registries.RegistryObject

class Perks(gun: GunData) {
    private val perks = gun.perk()

    fun has(perk: Perk): Boolean {
        if (!has(perk.type)) return false
        return getTag(perk).getString("Name") == perk.name
    }

    fun has(type: Perk.Type): Boolean {
        return perks.contains(type.getName()) && !perks.getCompound(type.getName()).getString("Name").isEmpty()
    }

    fun set(instance: PerkInstance) {
        set(instance.perk, instance.level)
    }

    fun getTag(registry: RegistryObject<Perk>): CompoundTag {
        return getTag(registry.get().type ?: Perk.Type.AMMO)
    }

    fun getTag(perk: Perk): CompoundTag {
        return getTag(perk.type)
    }

    fun getTag(type: Perk.Type): CompoundTag {
        return perks.getCompound(type.getName())
    }

    fun getOrCreateTag(perk: Perk): CompoundTag {
        val typeTag: CompoundTag?
        val type = perk.type
        if (!perks.contains(type.getName())) {
            typeTag = CompoundTag()
            perks.put(type.getName(), typeTag)
        }
        return perks.getCompound(type.getName())
    }

    fun set(perk: Perk, level: Short) {
        getOrCreateTag(perk).putString("Name", perk.name)
        getOrCreateTag(perk).putShort("Level", level)
    }

    fun getLevel(item: PerkItem): Short {
        return getLevel(item.perk)
    }

    fun getLevel(perk: RegistryObject<Perk>): Short {
        return getLevel(perk.get())
    }

    fun getLevel(perk: Perk): Short {
        val name = perk.name
        val tag = getTag(perk)
        if (tag.getString("Name") != name) return 0
        return getLevel(perk.type)
    }

    fun getLevel(type: Perk.Type): Short {
        return getTag(type).getShort("Level")
    }

    fun get(registry: RegistryObject<Perk>): Perk? {
        return get(registry.get())
    }

    fun get(perk: Perk): Perk? {
        return get(perk.type)
    }

    fun get(type: Perk.Type): Perk? {
        val perksRegistry = mutableListOf<RegistryObject<Perk>>()
        perksRegistry.addAll(ModPerks.AMMO_PERKS.getEntries())
        perksRegistry.addAll(ModPerks.FUNC_PERKS.getEntries())
        perksRegistry.addAll(ModPerks.DAMAGE_PERKS.getEntries())

        for (registry in perksRegistry) {
            val name = getTag(type).getString("Name")
            if (registry.get().name == name) {
                return registry.get()
            }
        }
        return null
    }

    fun getInstance(perk: Perk): PerkInstance? {
        return getInstance(perk.type)
    }

    fun getInstance(type: Perk.Type): PerkInstance? {
        val perk = get(type) ?: return null

        return PerkInstance(perk, getLevel(type))
    }

    fun reduceCooldown(registry: RegistryObject<Perk>, name: String) {
        reduceCooldown(registry.get(), name)
    }

    fun reduceCooldown(perk: Perk, name: String) {
        reduceCooldown(perk.type, name)
    }

    fun reduceCooldown(type: Perk.Type, name: String) {
        val tag = getTag(type)
        var value = tag.getInt(name)
        value--

        if (value <= 0) {
            tag.remove(name)
        } else {
            tag.putInt(name, value)
        }
    }

    fun remove(perk: Perk) {
        remove(perk.type)
    }

    fun remove(type: Perk.Type) {
        perks.remove(type.getName())
    }
}
