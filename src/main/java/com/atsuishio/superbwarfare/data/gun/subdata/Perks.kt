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

/**
 * Manages perk storage for a single [GunData] instance.
 *
 * Perk data is persisted in a [CompoundTag] partitioned by [Perk.Type].
 * Uses a static O(1) map lookup for resolving perks by name.
 *
 * @param gun the owning [GunData] instance.
 */
class Perks(private val gun: GunData) {

    private val rootTag: CompoundTag = gun.perk()

    /** Structural invalidation callback — clears the PMC when perk state changes. */
    private val invalidateStructural: () -> Unit = gun.nbtVersion::invalidateStructural

    companion object {
        /**
         * Flat map of perk name -> [Perk] instance, built once on class loading.
         * Replaces O(n) linear scans and list merge allocations during runtime tick calls.
         */
        @JvmStatic
        private val PERK_BY_NAME: Map<String, Perk> by lazy {
            val all: List<RegistryObject<out Perk>> =
                ModPerks.AMMO_PERKS.entries.toList() +
                ModPerks.FUNC_PERKS.entries.toList() +
                ModPerks.DAMAGE_PERKS.entries.toList()

            buildMap(all.size) {
                for (entry in all) {
                    val perk = entry.get()
                    put(perk.name, perk)
                }
            }
        }
    }

    /**
     * Looks up a [Perk] by its registry name in O(1) time.
     *
     * @param name the perk name string.
     * @return the matching [Perk], or `null` if not found.
     */
    private fun findPerkByName(name: String): Perk? = PERK_BY_NAME[name]

    /**
     * Returns the [ListTag] for [type], creating it if absent.
     *
     * @param type the perk type category.
     * @return existing or new [ListTag].
     */
    fun getOrCreateList(type: Perk.Type): ListTag {
        val typeName = type.typeName
        return if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
        } else {
            val tag = rootTag.getCompound(typeName)
            ListTag().also { rootTag.put(typeName, tag) }
        }
    }

    /**
     * Checks if [perk] is currently applied on the gun.
     *
     * @param perk the perk to check.
     * @return `true` if active.
     */
    fun has(perk: Perk): Boolean {
        val list = rootTag.getList(perk.type.typeName, Tag.TAG_COMPOUND.toInt())
        return list.any { (it as CompoundTag).getString("Name") == perk.name }
    }

    /**
     * Checks if any perk of [type] is applied.
     *
     * @param type the perk category.
     * @return `true` if any perk of this type exists.
     */
    fun has(type: Perk.Type): Boolean {
        val list = rootTag.getList(type.typeName, Tag.TAG_COMPOUND.toInt())
        return !list.isEmpty()
    }

    /**
     * Gets the level of [perk], returning 0 if not present.
     *
     * @param perk the target perk.
     * @return the perk level.
     */
    fun getLevel(perk: Perk): Short {
        val name = perk.type.typeName
        if (rootTag.contains(name, Tag.TAG_COMPOUND.toInt())) {
            return rootTag.getCompound(name).getShort("Level")
        }
        if (rootTag.contains(name, Tag.TAG_LIST.toInt())) {
            val list = rootTag.getList(name, Tag.TAG_COMPOUND.toInt())
            val entry = list.firstOrNull { (it as CompoundTag).getString("Name") == perk.name } as? CompoundTag
            return entry?.getShort("Level") ?: 0
        }
        return 0
    }

    fun getLevel(registry: RegistryObject<Perk>): Short = getLevel(registry.get())
    fun getLevel(item: PerkItem): Short = getLevel(item.perk)

    /**
     * Returns all active [PerkInstance]s for [type].
     *
     * @param type the perk category.
     * @return list of active perk instances.
     */
    fun getInstances(type: Perk.Type): List<PerkInstance> {
        val typeName = type.typeName
        val instances = mutableListOf<PerkInstance>()

        if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            val list = rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
            for (i in 0 until list.size) {
                val tag = list.getCompound(i)
                val perk = findPerkByName(tag.getString("Name")) ?: continue
                instances.add(PerkInstance(perk, tag.getShort("Level")))
            }
        } else {
            val tag = rootTag.getCompound(typeName)
            val perk = findPerkByName(tag.getString("Name")) ?: return instances
            instances.add(PerkInstance(perk, tag.getShort("Level")))
        }

        return instances
    }

    /**
     * Sets or updates [perk] at [level] and marks gun PMC as dirty.
     *
     * @param perk the perk to apply.
     * @param level the perk level.
     */
    fun set(perk: Perk, level: Short) {
        val list = getOrCreateList(perk.type)
        val existing = list.firstOrNull {
            (it as CompoundTag).getString("Name") == perk.name
        } as? CompoundTag

        if (existing != null) {
            existing.putShort("Level", level)
        } else {
            list.add(CompoundTag().apply {
                putString("Name", perk.name)
                putShort("Level", level)
            })
        }
        rootTag.put(perk.type.typeName, list)
        gun.nbtVersion.invalidateStructural()
    }

    fun set(instance: PerkInstance) = set(instance.perk, instance.level)

    /**
     * Removes [perk] from the weapon and invalidates structural version.
     *
     * @param perk the perk to remove.
     */
    fun remove(perk: Perk) {
        val typeName = perk.type.typeName
        if (!rootTag.contains(typeName, Tag.TAG_LIST.toInt())) return

        val list = rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
        val removed = list.removeIf { (it as CompoundTag).getString("Name") == perk.name }

        if (removed) {
            if (list.isEmpty()) rootTag.remove(typeName)
            gun.nbtVersion.invalidateStructural()
        }
    }

    /**
     * Removes all perks of [type].
     *
     * @param type perk category to remove.
     */
    fun removeAll(type: Perk.Type) {
        if (rootTag.contains(type.typeName)) {
            rootTag.remove(type.typeName)
            gun.nbtVersion.invalidateStructural()
        }
    }

    /**
    * Decrements an integer cooldown stored inside a perk's compound entry.
    * Removes the key when the value reaches zero.
    *
    * This method increments [NbtVersion.structural] on the owning [GunData]
    * because several perks (e.g. KillClip, OneTwoPunch) read their cooldown
    * counters inside [Perk.modifyProperty], meaning a cooldown change affects
    * computed gun properties.
    *
    * @param perk        the owning perk.
    * @param cooldownKey the NBT key of the cooldown counter.
    */
    fun reduceCooldown(perk: Perk, cooldownKey: String) {
        val list = rootTag.getList(perk.type.typeName, Tag.TAG_COMPOUND.toInt())
        val entry = list.firstOrNull {
            (it as CompoundTag).getString("Name") == perk.name
        } as? CompoundTag ?: return

        if (!entry.contains(cooldownKey)) return

        val next = entry.getInt(cooldownKey) - 1
        if (next <= 0) entry.remove(cooldownKey) else entry.putInt(cooldownKey, next)

        // Perk cooldowns affect modifyProperty() output — invalidate PMC.
        invalidateStructural()
    }

    /**
     * Sets (or creates) an integer value inside a perk's compound entry.
     *
     * When used for cooldown counters that are read by [Perk.modifyProperty],
     * this method ensures the structural version is incremented so that the
     * PMC is rebuilt with the updated predicate ({@code value > 0}).
     *
     * @param perk  the owning perk.
     * @param key   the NBT key to write.
     * @param value the integer value.
     */
    fun putStructuralInt(perk: Perk, key: String, value: Int) {
        val tag = getTag(perk) ?: return
        val old = if (tag.contains(key)) tag.getInt(key) else 0
        tag.putInt(key, value)
        
        // Invalidate ONLY on status transition (0 -> Active or Active -> 0)
        if ((old <= 0) != (value <= 0)) {
            gun.nbtVersion.invalidateStructural()
        }
    }

    fun getTag(perk: Perk): CompoundTag? =
        getOrCreateList(perk.type)
            .filterIsInstance<CompoundTag>()
            .firstOrNull { perk.name == it.getString("Name") }

    fun getTag(registry: RegistryObject<Perk>): CompoundTag? = getTag(registry.get())

    fun getOrCreateTag(perk: Perk): CompoundTag {
        val type = perk.type
        if (!rootTag.contains(type.typeName)) {
            rootTag.put(type.typeName, CompoundTag())
        }
        return rootTag.getCompound(type.typeName)
    }

    fun get(registry: RegistryObject<Perk>): Perk? = get(registry.get())
    fun get(perk: Perk): Perk? = get(perk.type)

    fun get(type: Perk.Type): Perk? {
        val typeName = type.typeName
        return if (rootTag.contains(typeName, Tag.TAG_LIST.toInt())) {
            val list = rootTag.getList(typeName, Tag.TAG_COMPOUND.toInt())
            if (list.isEmpty()) null
            else findPerkByName(list.getCompound(0).getString("Name"))
        } else {
            findPerkByName(rootTag.getCompound(typeName).getString("Name"))
        }
    }
}