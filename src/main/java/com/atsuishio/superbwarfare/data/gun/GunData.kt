package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.data.JsonPropertyModifier
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AMMO_CONSUMER
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AMMO_COST_PER_SHOOT
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AVAILABLE_FIRE_MODES
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AVAILABLE_PERKS
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.BOLT_ACTION_TIME
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.DEFAULT_ZOOM
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MAGAZINE
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MELEE_DAMAGE
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.PROJECTILE_AMOUNT
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SHOOT_POS
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SHOOT_SHAKE
import com.atsuishio.superbwarfare.data.gun.subdata.*
import com.atsuishio.superbwarfare.data.gun.value.*
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.atsuishio.superbwarfare.tools.sameWith
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.IEnergyStorage
import net.minecraftforge.items.IItemHandler
import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

/**
 * Extension function checking whether an [ItemStack] represents a valid gun item.
 *
 * @return `true` if the item is an instance of [GunItem].
 */
fun ItemStack.isGunItem(): Boolean = this.item is GunItem

/**
 * Converts an [ItemStack] to a [GunData] wrapper if applicable.
 *
 * @return [GunData] instance, or `null` if stack is not a gun.
 */
fun ItemStack.toGunData(): GunData? = if (isGunItem()) GunData.from(this) else null

/**
 * Core runtime data container and Property Modifier Calculator (PMC) wrapper for firearm items.
 *
 * Manages NBT tags, computed properties, fire modes, ammo consumers, perks, attachments,
 * and state timers. Optimized via [NbtVersion] to distinguish structural NBT changes
 * (requiring PMC re-calculation) from ephemeral runtime state modifications.
 *
 * @author superbwarfare contributors
 * @since 0.8.9.1
 */
class GunData private constructor(
    stack: ItemStack, initialDefaultDataSupplier: (() -> DefaultGunData)? = null
) : DefaultDataSupplier<DefaultGunData> {

    /** The target weapon item stack wrapped by this data object. */
    @JvmField
    val stack: ItemStack

    /** The underlying [GunItem] definition for this weapon. */
    @JvmField
    val item: GunItem

    /** The root NBT tag compound attached to the item stack. */
    @JvmField
    val tag: CompoundTag

    /** The primary NBT compound containing gun data properties. */
    @JvmField
    val gunDataTag: CompoundTag

    /** The NBT compound containing active perk configurations. */
    @JvmField
    val perkTag: CompoundTag

    /** The NBT compound containing attachment slot configurations. */
    @JvmField
    val attachmentTag: CompoundTag

    /** JSON override string for dynamically replacing property values. */
    @JvmField
    val propertyOverrideString: StringValue

    /** Unique registry identifier string for the underlying item. */
    @JvmField
    val id: String

    /** Tracks structural and state NBT mutations for O(1) PMC invalidation. */
    @JvmField
    val nbtVersion: NbtVersion = NbtVersion()

    /**
     * Supplier for the default (unmodified) gun property set.
     *
     * Marked as [internal] to allow [VehicleEntity] to update the baseline supplier
     * without reconstructing the entire [GunData] instance.
     */
    @JvmField
    internal var defaultDataSupplier: () -> DefaultGunData

    /** Cached snapshot of the item stack used for equality checks. */
    var lastTimeStack: ItemStack? = null

    /** Cached result of the last [countBackupAmmo] inventory computation. */
    @JvmField
    var cachedBackupAmmo: Int = -1

    /** Game time (in ticks) when [cachedBackupAmmo] was last computed. */
    @JvmField
    var cachedBackupAmmoTick: Long = -BACKUP_AMMO_CACHE_TICKS

    /** Combined NBT version snapshot taken at construction time to track mutations O(1). */
    private val initialCombinedVersion: Int = nbtVersion.structural + nbtVersion.state

    /**
     * Gets or creates a child [CompoundTag] with the given [name] inside [tag].
     *
     * @param name the child tag key name.
     * @return existing or newly created [CompoundTag].
     */
    private fun getOrPut(name: String): CompoundTag {
        if (!this.tag.contains(name)) {
            this.tag.put(name, CompoundTag())
        }
        return this.tag.getCompound(name)
    }

    /**
     * Checks if the gun has been properly initialized.
     *
     * @return `true` if initialization has occurred.
     */
    fun initialized(): Boolean {
        return item.isInitialized(this)
    }

    /**
     * Executes initial setup logic for this weapon item.
     */
    fun initialize() {
        item.init(this)
    }

    /** Returns the underlying [GunItem]. */
    fun item(): GunItem = item

    /** Returns the wrapped [ItemStack]. */
    fun stack(): ItemStack = stack

    /** Returns the root NBT [CompoundTag]. */
    fun tag(): CompoundTag = tag

    /** Returns the gun data NBT [CompoundTag]. */
    fun data(): CompoundTag = gunDataTag

    /** Returns the perk NBT [CompoundTag]. */
    fun perk(): CompoundTag = perkTag

    /** Returns the attachment NBT [CompoundTag]. */
    fun attachment(): CompoundTag = attachmentTag

    /** Returns default un-modified [DefaultGunData] baseline for this weapon. */
    override fun getDefault(): DefaultGunData = this.defaultDataSupplier()

    /**
     * Updates the default data supplier and invalidates the structural version counter.
     *
     * This forces a PMC rebuild on the next [get] access with the updated defaults,
     * while preserving the existing [GunData] instance, stack, and [NbtVersion] state.
     *
     * @param supplier new function supplying updated [DefaultGunData].
     */
    fun updateDefaultDataSupplier(supplier: () -> DefaultGunData) {
        defaultDataSupplier = supplier
        nbtVersion.invalidateStructural()
    }

    /**
     * Sets temporary runtime modifications to default weapon data.
     *
     * @param modification function modifying default gun data.
     */
    fun setTempModifications(modification: Function<DefaultGunData, DefaultGunData>) {
        tempModifications = modification
        nbtVersion.invalidateStructural()
    }

    /** Clears temporary runtime weapon modifications. */
    fun clearTempModifications() {
        tempModifications = null
        nbtVersion.invalidateStructural()
    }

    private val jsonPropModifier = JsonPropertyModifier(GunProp.entries)
    private var cache: DefaultGunData? = null
    private var tempModifications: Function<DefaultGunData, DefaultGunData>? = null

    /**
     * Computes modified property values into a standalone [DefaultGunData] snapshot.
     *
     * @param useCache whether to re-use cached result if available.
     * @return computed gun data properties.
     * @deprecated Use [get] with [GunProp] keys instead for optimized property resolution.
     */
    @JvmOverloads
    @Deprecated("Use get() instead")
    @ApiStatus.ScheduledForRemoval
    fun compute(useCache: Boolean = true): DefaultGunData {
        if (cache != null && useCache) return cache!!

        var rawData = getDefault().copy()
//
//        // property override tag
//        jsonPropModifier.update(propertyOverrideString.get())
//        rawData = jsonPropModifier.computeProperties(this, rawData)
//
//        // gun modifiers
//        rawData = item.computeProperties(this, rawData)
//
//        // FireMode
//        rawData = selectedFireModeInfo(rawData.availableFireModes()).computeProperties(this, rawData)
//
//        // AmmoConsumer
//        rawData = selectedAmmoConsumer(rawData.getProcessedAmmoConsumers()).computeProperties(this, rawData)
//
//        // perk
//        for (type in PERK_TYPES) {
//            val instance = perk.get(type) ?: continue
//
//            rawData = instance.computeProperties(this, rawData)
//        }
//
//        // Temporary property modifications
//        if (tempModifications != null) {
//            rawData = tempModifications!!.apply(rawData)
//        }
//
//        rawData.limit()
//        if (useCache) {
//            cache = rawData
//        }

        return rawData
    }

    private val pmcInstance: PMC<GunData, DefaultGunData> by lazy { PMC(this) }
    private var cachedStructuralVersion: Int = -1

    /**
     * Resolves a computed weapon property using lazy PMC caching.
     *
     * Utilizes [NbtVersion.structural] to bypass redundant property calculation
     * when weapon structure (attachments, perks, fire mode, overrides) has not changed.
     *
     * @param prop the target weapon property key.
     * @return calculated value for the given property.
     */
    @Suppress("unchecked_cast")
    fun <T> get(prop: GunProp<*, T>): T {        
        // Fast path: structural version matches cached version -> return cached value
        if (cachedStructuralVersion == nbtVersion.structural) {
            return pmcInstance[prop]
        }

        // Structural version mismatch: rebuild property modification pipeline
        pmcInstance.reset()

        // 1. Property override tag
        jsonPropModifier.update(propertyOverrideString.get())
        jsonPropModifier.modifyProperty(pmcInstance)

        // 2. Gun item level modifiers
        item.modifyProperty(pmcInstance)

        // 3. FireMode modifiers
        selectedFireModeInfo(pmcInstance[AVAILABLE_FIRE_MODES]).modifyProperty(pmcInstance)

        // 4. AmmoConsumer modifiers
        selectedAmmoConsumer(pmcInstance[AMMO_CONSUMER]).modifyProperty(pmcInstance)

        // 5. Active Perks
        for (type in PERK_TYPES) {
            val list = perk.getInstances(type)
            for (instance in list) {
                instance.perk.modifyProperty(pmcInstance)
            }
        }

        // TODO Temporary property modifications
//        if (tempModifications != null) {
//            rawData = tempModifications!!.apply(rawData)
//        }

        // 6. Global property bounds limit
        GunProp.modifyProperty(pmcInstance)

        cachedStructuralVersion = nbtVersion.structural
        return pmcInstance[prop]
    }

    /**
     * Checks if the shooter has infinite backup ammunition available.
     *
     * @param shooter the entity attempting to fire or reload.
     * @return `true` if creative mode, infinite consumer, or creative ammo box is present.
     */
    fun hasInfiniteBackupAmmo(shooter: Entity?): Boolean {
        return shooter is Player && shooter.isCreative
                || selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.INFINITE
                || meleeOnly()
                || InventoryTool.hasCreativeAmmoBox(shooter)
    }

    /**
     * Determines whether the weapon directly consumes ammo from the inventory without reloading.
     *
     * @return `true` if magazine capacity is zero or less.
     */
    fun useBackpackAmmo(): Boolean {
        return get(MAGAZINE) <= 0
    }

    /**
     * Calculates minimum scope zoom ratio.
     *
     * @return minimum allowed zoom value.
     */
    fun minZoom(): Double {
        val scopeType = this.attachment.get(AttachmentType.SCOPE)
        return if (scopeType == 3) max(getDefault().minZoom, 1.25) else 1.25
    }

    /**
     * Calculates maximum scope zoom ratio.
     *
     * @return maximum allowed zoom value.
     */
    fun maxZoom(): Double {
        val scopeType = this.attachment.get(AttachmentType.SCOPE)
        return if (scopeType == 3) getDefault().maxZoom else 114514.0
    }

    /**
     * Gets current clamped zoom ratio for camera rendering.
     *
     * @return clamped zoom magnification level.
     */
    fun zoom(): Double {
        if (minZoom() >= maxZoom()) return get(DEFAULT_ZOOM)
        return Mth.clamp(get(DEFAULT_ZOOM), minZoom(), maxZoom())
    }

    /**
     * Retrieves currently selected ammo consumer definition.
     *
     * @param consumers list of available consumers, defaults to weapon's computed consumers.
     * @return active [AmmoConsumer] instance.
     */
    @JvmOverloads
    fun selectedAmmoConsumer(consumers: List<AmmoConsumer>? = get(AMMO_CONSUMER)): AmmoConsumer {
        if (consumers.isNullOrEmpty()) {
            return AmmoConsumer.INVALID
        }
        return consumers[this.selectedAmmoType.get().coerceIn(consumers.indices)]
    }

    /**
     * Switches weapon's active ammo consumer type and handles inventory unloading.
     *
     * @param index index of the target ammo consumer in the available list.
     * @param ammoSupplier entity supplying ammo for inventory operations.
     */
    fun changeAmmoConsumer(index: Int, ammoSupplier: Entity?) {
        val consumers = get(AMMO_CONSUMER)
        val targetIndex = index.coerceIn(consumers.indices)
        if (targetIndex == selectedAmmoType.get()) return

        if (!(ammoSupplier is Player && ammoSupplier.isCreative)) {
            val currentConsumer = selectedAmmoConsumer()
            val targetConsumer = consumers[selectedAmmoType.get()]

            val currentSlot = currentConsumer.ammoSlot
            val targetSlot = targetConsumer.ammoSlot

            if (currentSlot == targetSlot && ammoSupplier != null && targetConsumer.shouldUnload) {
                this.withdrawAmmo(ammoSupplier)
            } else {
                val ammo = this.ammo.get()
                val virtualAmmo = this.virtualAmmo.get()
                this.ammoSlot.set(currentSlot, ammo, virtualAmmo)

                this.ammo.set(this.ammoSlot.getAmmo(targetSlot))
                this.virtualAmmo.set(this.ammoSlot.getVirtualAmmo(targetSlot))
                this.ammoSlot.reset(targetSlot)
            }
        }

        this.selectedAmmoType.set(targetIndex)

        if (ammoSupplier is Player && ammoSupplier.isCreative) {
            this.ammo.set(get(MAGAZINE))
        }

        this.item.whenNoAmmo(this)
        this.closeHammer.set(false)
        this.fireIndex.reset()

        resetStatus()

        // Ammo type changed — old backupAmmoCount belongs to previous consumer type.
        // selectedAmmoType is already updated above, so countBackupAmmo uses NEW consumer.
        this.cachedBackupAmmo = -1
        this.backupAmmoCount.set(countBackupAmmo(ammoSupplier))
    }

    /**
     * Resets transient runtime weapon states including reload, charge, and bolt timers.
     */
    fun resetStatus() {
        this.reload.stage.reset()
        this.reload.setState(ReloadState.NOT_RELOADING)
        this.reload.iterativeLoadTimer.reset()
        this.reload.reloadTimer.reset()
        this.reload.finishTimer.reset()
        this.reload.prepareTimer.reset()
        this.reload.prepareLoadTimer.reset()
        this.reload.reloadStarter.finish()
        this.reload.singleReloadStarter.finish()
        this.bolt.actionTimer.reset()
        this.bolt.needed.reset()
        this.charge.starter.finish()
        this.charge.timer.reset()
    }

    /**
     * Retrieves information about the currently selected fire mode.
     *
     * @param fireModes list of available fire modes, defaults to weapon's computed modes.
     * @return active [FireModeInfo].
     */
    @JvmOverloads
    fun selectedFireModeInfo(fireModes: List<FireModeInfo>? = get(AVAILABLE_FIRE_MODES)): FireModeInfo {
        if (fireModes.isNullOrEmpty()) {
            return FireModeInfo()
        }
        return fireModes[this.selectedFireMode.get().coerceIn(fireModes.indices)]
    }

    // Fire process start

    /*
     * Fire Process Sequence Description:
     * 1. Call shouldStartReloading and shouldStartBolt to verify whether reloading or bolt action should start.
     * If so, call startReload or startBolt.
     * 2. Call canShoot(@Nullable Entity shooter) to check if shooting conditions are met, then invoke shoot.
     * 3. Call tick(@Nullable Entity shooter) to execute weapon tick routines (reload timers, heat, bolt, etc.).
     *
     * Optional Steps:
     * 1. Use GunData.virtualAmmo.set to specify virtual ammo count.
     * 2. Pass an Entity with IItemHandler capability to provide extra ammo.
     */

    /**
     * Checks if weapon should initiate reload sequence.
     *
     * @param entity the entity holding the weapon.
     * @return `true` if weapon is empty and backup ammo is available.
     */
    fun shouldStartReloading(entity: Entity?): Boolean {
        return !reloading() && !useBackpackAmmo() && !hasEnoughAmmoToShoot(entity) && hasBackupAmmo(entity)
    }

    /**
     * Checks if bolt action process should start.
     *
     * @return `true` if bolt timer is zero and bolt is flagged as needed.
     */
    fun shouldStartBolt(): Boolean {
        return this.bolt.actionTimer.get() == 0 && this.bolt.needed.get()
    }

    /** Starts reload sequence in next tick update. */
    fun startReload() {
        this.reload.reloadStarter.markStart()
    }

    /** Starts manual bolt-action sequence. */
    fun startBolt() {
        this.bolt.actionTimer.set(get(BOLT_ACTION_TIME) + 1)
    }

    /**
     * Checks if backup ammo is available (excluding loaded magazine ammo).
     *
     * @param entity the ammo source entity.
     * @return `true` if backup ammo count > 0.
     */
    fun hasBackupAmmo(entity: Entity?): Boolean {
        return countBackupAmmo(entity) > 0
    }

    /**
     * Calculates total backup ammo quantity available from an entity source.
     * Caches result for [BACKUP_AMMO_CACHE_TICKS] ticks to avoid iterating inventory slots every tick.
     *
     * @param entity the ammo supplier entity; may be null.
     * @return available backup ammo count.
     */
    fun countBackupAmmo(entity: Entity?): Int {
        if (entity == null) return virtualAmmo.get()
        if (entity is Player && entity.isCreative) return Int.MAX_VALUE
        if (InventoryTool.hasCreativeAmmoBox(entity)) return Int.MAX_VALUE

        val currentTick = entity.level().gameTime
        if (cachedBackupAmmo >= 0 && (currentTick - cachedBackupAmmoTick) < BACKUP_AMMO_CACHE_TICKS) {
            return cachedBackupAmmo
        }

        val computed = Math.toIntExact(
            min(
                countBackupAmmoItem(entity).toLong() * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(),
                Int.MAX_VALUE.toLong()
            )
        )
        cachedBackupAmmo = computed
        cachedBackupAmmoTick = currentTick
        return computed
    }

    /**
     * Calculates total backup ammo quantity available from an item handler.
     *
     * @param handler the item handler container; may be null.
     * @return available backup ammo count.
     */
    fun countBackupAmmo(handler: IItemHandler?): Int {
        if (handler == null) return virtualAmmo.get()
        if (InventoryTool.hasCreativeAmmoBox(handler)) return Int.MAX_VALUE

        return Math.toIntExact(
            min(
                countBackupAmmoItem(handler).toLong() * this.selectedAmmoConsumer().loadAmount + this.virtualAmmo.get(),
                Int.MAX_VALUE.toLong()
            )
        )
    }

    /** Counts raw backup ammo item stacks for entity source. */
    fun countBackupAmmoItem(entity: Entity?): Int {
        return this.selectedAmmoConsumer().count(this, entity)
    }

    /** Counts raw backup ammo item stacks for item handler source. */
    fun countBackupAmmoItem(handler: IItemHandler?): Int {
        return this.selectedAmmoConsumer().count(this, handler)
    }

    /**
     * Consumes backup ammunition without reducing loaded magazine rounds.
     *
     * @param entity ammo source entity.
     * @param count required ammo count.
     */
    fun consumeBackupAmmo(entity: Entity?, count: Int) {
        var remaining = count
        if (remaining <= 0 || entity is Player && entity.isCreative || InventoryTool.hasCreativeAmmoBox(entity)) return

        if (virtualAmmo.get() > 0) {
            val consumed = min(virtualAmmo.get(), remaining)
            virtualAmmo.add(-consumed)
            remaining -= consumed
            save()
        }
        if (remaining <= 0 || entity == null) return

        val consumer = this.selectedAmmoConsumer()
        val loadAmount = consumer.loadAmount
        if (remaining % loadAmount != 0) {
            val required = (remaining / loadAmount) + 1
            val consumed = consumer.consume(this, entity, required)
            remaining -= consumed * loadAmount

            if (remaining <= 0) {
                this.virtualAmmo.add(-remaining)
            }
        } else {
            consumer.consume(this, entity, remaining / loadAmount)
        }

        // Event-driven: instant display update and cache invalidation.
        // backupAmmoCount is synced to client via GUN_DATA_MAP entityData each tick.
        val display = backupAmmoCount.get()
        if (display in 1 until Int.MAX_VALUE) {
            backupAmmoCount.set(max(0, display - count))
        }
        // Force countBackupAmmo() to rescan on next call — prevents stale reload logic.
        cachedBackupAmmo = -1
    }

    /**
     * Consumes backup ammunition from item handler without reducing loaded magazine rounds.
     *
     * @param handler ammo container item handler.
     * @param count required ammo count.
     */
    fun consumeBackupAmmo(handler: IItemHandler?, count: Int) {
        var remaining = count
        if (remaining <= 0 || InventoryTool.hasCreativeAmmoBox(handler)) return

        if (virtualAmmo.get() > 0) {
            val consumed = min(virtualAmmo.get(), remaining)
            virtualAmmo.add(-consumed)
            remaining -= consumed
            save()
        }
        if (remaining <= 0 || handler == null) return

        val consumer = selectedAmmoConsumer()
        val loadAmount = consumer.loadAmount

        if (remaining % loadAmount != 0) {
            val required = (remaining / loadAmount) + 1
            val consumed = consumer.consume(this, handler, required)
            remaining -= consumed * loadAmount

            if (remaining <= 0) {
                this.virtualAmmo.add(-remaining)
            }
        } else {
            consumer.consume(this, handler, remaining / loadAmount)
        }

        // ----- Event-driven: instant HUD update on ammo consumption -----
        val display = backupAmmoCount.get()
        if (display in 1 until Int.MAX_VALUE) {
            backupAmmoCount.set(max(0, display - count))
        }
        cachedBackupAmmo = -1
    }

    /**
     * Calculates remaining shots possible before requiring a reload.
     *
     * @param entity the shooter entity.
     * @return total shot count.
     */
    fun currentAvailableShots(entity: Entity?): Int {
        val ammoCost = get(AMMO_COST_PER_SHOOT)
        if (ammoCost <= 0) return Int.MAX_VALUE

        return currentAvailableAmmo(entity) / ammoCost
    }

    /**
     * Gets ammo count currently available inside gun magazine or inventory.
     *
     * @param entity shooter entity.
     * @return current available ammo quantity.
     */
    fun currentAvailableAmmo(entity: Entity?): Int {
        return if (useBackpackAmmo()) countBackupAmmo(entity) else this.ammo.get()
    }

    /**
     * Checks whether weapon has sufficient magazine/inventory ammo to execute one shot.
     *
     * @param entity shooter entity.
     * @return `true` if available ammo >= cost per shot.
     */
    fun hasEnoughAmmoToShoot(entity: Entity?): Boolean {
        return get(AMMO_COST_PER_SHOOT) <= currentAvailableAmmo(entity)
    }

    /**
     * Refills magazine upon completion of reload sequence.
     *
     * @param entity shooter entity.
     * @param extraOne whether to add +1 round in chamber for open-bolt/chambered designs.
     */
    @JvmOverloads
    fun reloadAmmo(entity: Entity?, extraOne: Boolean = false) {
        if (useBackpackAmmo()) return

        val mag = get(MAGAZINE)
        val ammo = this.ammo.get()
        val ammoNeeded = mag - ammo + (if (extraOne) 1 else 0)

        // Empty reload bolt-action weapon should cancel bolt-needed flag after reloading
        if (ammo == 0 && get(BOLT_ACTION_TIME) > 0) {
            bolt.needed.set(false)
        }

        val available = countBackupAmmo(entity)
        val ammoToAdd = min(ammoNeeded, available)

        consumeBackupAmmo(entity, ammoToAdd)
        this.ammo.set(ammo + ammoToAdd)

        reload.setState(ReloadState.NOT_RELOADING)
        this.fireIndex.reset()
    }

    /**
     * Verifies if weapon can shoot under current state.
     *
     * @param shooter entity firing weapon.
     * @return `true` if weapon can fire.
     */
    fun canShoot(shooter: Entity?): Boolean {
        return item.canShoot(this, shooter)
    }

    /** Fires projectile without entity shooter context. */
    fun shoot(level: ServerLevel, shootPosition: Vec3, shootDirection: Vec3, spread: Double, zoom: Boolean) {
        this.item.shoot(level, shootPosition, shootDirection, this, spread, zoom, null)
    }

    /** Fires projectile with entity shooter context. */
    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?) {
        this.item.shoot(this, entity, spread, zoom, uuid)
    }

    /** Fires projectile targeting specific world position. */
    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?, targetPos: Vec3?) {
        this.item.shoot(this, entity, spread, zoom, uuid, targetPos)
    }

    /** Fires projectile using encapsulated parameter structure. */
    fun shoot(parameters: ShootParameters) {
        this.item.shoot(parameters)
    }

    /**
     * Updates weapon state and timers during tick.
     *
     * Automatically invoked via [GunItem.inventoryTick] when in player inventory.
     *
     * @param shooter entity holding weapon.
     * @param inMainHand whether weapon is currently held in main hand.
     */
    fun tick(shooter: Entity?, inMainHand: Boolean) {
        GunEventHandler.gunTick(shooter, this, inMainHand)
    }

    // Fire process end

    /**
     * Withdraws loaded rounds back to entity inventory during reload or attachment modification.
     *
     * @param ammoSupplier target entity receiving withdrawn ammo.
     */
    fun withdrawAmmo(ammoSupplier: Entity) {
        val itemAmount = withdrawAmmoCount()

        this.virtualAmmo.reset()
        this.ammo.reset()

        selectedAmmoConsumer().withdraw(ammoSupplier, itemAmount)
    }

    /** Calculates item count returned upon ammo withdrawal. */
    fun withdrawAmmoCount(): Int {
        return (this.virtualAmmo.get() + this.ammo.get()) / selectedAmmoConsumer().loadAmount
    }

    /**
     * Withdraws loaded rounds back to item handler container during reload or attachment modification.
     *
     * @param handler target container handler.
     */
    fun withdrawAmmo(handler: IItemHandler) {
        val itemAmount = withdrawAmmoCount()

        this.virtualAmmo.reset()
        this.ammo.reset()

        // Discards remainder when withdrawing to item handler
        selectedAmmoConsumer().withdraw(handler, itemAmount)
    }

    /** Gets list of available perks applicable to weapon. */
    fun availablePerks(): List<Perk> = get(AVAILABLE_PERKS)

    /** Checks if specific perk can be applied. */
    fun canApplyPerk(perk: Perk): Boolean = availablePerks().contains(perk)

    /** Raw damage reduction property structure. */
    val rawDamageReduce: DamageReduce
        get() = getDefault().damageReduce

    /** Modified damage reduction rate. */
    val damageReduceRate: Double
        get() {
            for (type in PERK_TYPES) {
                return this.perk.getInstances(type)
                    .minOfOrNull { it.perk.getModifiedDamageReduceRate(this.rawDamageReduce) } ?: continue
            }
            return this.rawDamageReduce.rate
        }

    /** Modified damage reduction minimum distance. */
    val damageReduceMinDistance: Double
        get() {
            for (type in PERK_TYPES) {
                return this.perk.getInstances(type)
                    .minOfOrNull { it.perk.getModifiedDamageReduceMinDistance(this.rawDamageReduce) } ?: continue
            }
            return this.rawDamageReduce.minDistance
        }

    /** Checks if weapon is configured strictly for melee attacks. */
    fun meleeOnly(): Boolean {
        return get(PROJECTILE_AMOUNT) <= 0 && get(MELEE_DAMAGE) > 0
    }

    /** Checks if weapon is a shotgun (projectile count > 1). */
    val isShotgun: Boolean
        get() = get(PROJECTILE_AMOUNT) > 1

    /** Returns current barrel fire offset position. */
    fun firePosition(): Vec3 {
        val list = get(SHOOT_POS).positions
        val size = list.size
        if (size == 0) {
            return Vec3.ZERO
        }

        return if (get(SHOOT_POS).boundUpWithAmmoAmount) {
            list.getOrNull(Mth.clamp(this.ammo.get() - 1, 0, size)) ?: Vec3.ZERO
        } else {
            list.getOrNull(this.fireIndex.get() % size) ?: Vec3.ZERO
        }
    }

    /** Returns current HUD aiming position override or fire position. */
    fun firePositionForHud(): Vec3 {
        return get(SHOOT_POS).shootPositionForHud ?: firePosition()
    }

    /** Returns fire direction vector definition. */
    fun fireDirection(): StringOrVec3 {
        val list = get(SHOOT_POS).directions
        val size = list.size
        if (size == 0) {
            return StringOrVec3("Default")
        }

        return list.getOrNull(this.fireIndex.get() % size) ?: StringOrVec3("Default")
    }

    /** Returns HUD fire direction vector override. */
    fun fireDirectionForHud(): StringOrVec3? {
        return get(SHOOT_POS).shootDirectionForHud
    }

    /** Returns energy capability provider for energy-based weapons. */
    fun getEnergyProvider(ammoSupplier: Entity?): LazyOptional<IEnergyStorage> {
        return this.item.getEnergyProvider(this, ammoSupplier)
    }

    /** Triggers camera shake packet to surrounding players upon firing. */
    fun shakePlayers(source: Entity?) {
        if (source == null) return

        val shootShake = get(SHOOT_SHAKE) ?: return

        ShakeClientMessage.sendToNearbyPlayers(source, shootShake.x, shootShake.y, shootShake.z)
    }

    // Persistent properties start

    @JvmField
    val selectedAmmoType: IntValue

    @JvmField
    val selectedFireMode: IntValue

    @JvmField
    val level: IntValue

    @JvmField
    val ammo: IntValue

    @JvmField
    val virtualAmmo: IntValue

    // Backup ammo count override
    @JvmField
    val backupAmmoCount: IntValue

    @JvmField
    val ammoSlot: AmmoSlot

    @JvmField
    val burstAmount: IntValue

    @JvmField
    val fireIndex: IntValue

    @JvmField
    val exp: DoubleValue

    // Max: 100
    @JvmField
    val heat: DoubleValue

    @JvmField
    val shootAnimationTimer: IntValue

    @JvmField
    val shootTimer: IntValue

    @JvmField
    val overHeat: BooleanValue

    /** Checks if scope zoom adjustment is supported. */
    fun canAdjustZoom(): Boolean = item.canAdjustZoom(this)

    /** Checks if scope switching is supported. */
    fun canSwitchScope(): Boolean = item.canSwitchScope(this)

    @JvmField
    val reload: Reload

    /** Checks if weapon is currently reloading. */
    fun reloading(): Boolean = reload.state() != ReloadState.NOT_RELOADING

    @JvmField
    val charge: Charge

    /** Checks if energy charging is active. */
    fun charging(): Boolean = charge.time() > 0

    @JvmField
    val isEmpty: BooleanValue

    @JvmField
    val closeHammer: BooleanValue

    @JvmField
    val closeStrike: BooleanValue

    @JvmField
    val stopped: BooleanValue

    @JvmField
    val forceStop: BooleanValue

    @JvmField
    val loadIndex: IntValue

    @JvmField
    val holdOpen: BooleanValue

    @JvmField
    val hideBulletChain: BooleanValue

    @JvmField
    val sensitivity: IntValue

    @JvmField
    val zooming: BooleanValue

    // Other child subdata properties

    @JvmField
    val bolt: Bolt

    @JvmField
    val attachment: Attachment

    @JvmField
    val perk: Perks

    @JvmField
    val weaponPitch: DoubleValue

    @JvmField
    val weaponYaw: DoubleValue

    /**
     * Persists pending NBT changes back to the underlying [ItemStack] tag.
     */
    fun save() {
        // Fast-path: If neither structural nor state versions changed, tag is unmodified
        val currentCombined = nbtVersion.structural + nbtVersion.state
        if (currentCombined == initialCombinedVersion) return

        // TODO Implement proper empty tag removal
//        var keysToRemove = new ArrayList<String>();
//        for (var key : perkTag.getAllKeys()) {
//            if (perkTag.get(key) instanceof CompoundTag compoundTag && compoundTag.isEmpty()) {
//                keysToRemove.add(key);
//            }
//        }
//        keysToRemove.forEach(perkTag::remove);
//
//        if (perkTag.isEmpty()) {
//            stack.removeTagKey("Perks");
//        }
//
//        if (attachmentTag.isEmpty()) {
//            stack.removeTagKey("Attachments");
//        }
//
//        if (gunDataTag.isEmpty()) {
//            stack.removeTagKey("GunData");
//        }
    }

    /**
     * Checks equality between two [GunData] instances based on item stack reference identity.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is GunData) return false
        return other.stack === this.stack
    }

    /** Creates duplicate copy of this [GunData]. */
    fun copy(): GunData {
        return GunData(this.stack.copy(), this.defaultDataSupplier)
    }

    // TODO Deprecated: temporary adaptation for Touhou Little Maid mod
    @Deprecated("use selectedFireModeInfo() instead", ReplaceWith("selectedFireModeInfo()"))
    @Suppress("unused")
    @JvmField
    val fireMode: StringEnumValue<FireMode> = object : StringEnumValue<FireMode>(
        CompoundTag(),
        "DeprecatedFireMode",
        FireMode.SEMI,
        { _ -> FireMode.SEMI }) {

        override fun get(): FireMode {
            return this@GunData.selectedFireModeInfo().mode ?: FireMode.SEMI
        }
    }

    init {
        require(stack.item is GunItem) { "stack is not GunItem!" }

        val gunItem = stack.item as GunItem
        this.item = gunItem
        this.stack = stack
        this.id = getRegistryId(stack.item)

        this.defaultDataSupplier = initialDefaultDataSupplier ?: { gunItem.getDefaultData(this) }

        this.tag = stack.getOrCreateTag()

        gunDataTag = getOrPut("GunData")
        perkTag = getOrPut("Perks")
        attachmentTag = getOrPut("Attachments")

        // Structural properties -> invalidate PMC pipeline on change
        propertyOverrideString = StringValue(this.gunDataTag, "Override", onSet = nbtVersion::invalidateStructural)
        selectedAmmoType = IntValue(gunDataTag, "SelectedAmmoType", onSet = nbtVersion::invalidateStructural)
        selectedFireMode = IntValue(gunDataTag, "SelectedFireMode", 0, onSet = nbtVersion::invalidateStructural)
        level = IntValue(gunDataTag, "Level", onSet = nbtVersion::invalidateStructural)

        // Subdata handlers
        reload = Reload(this)
        charge = Charge(this)
        bolt = Bolt(this)
        attachment = Attachment(this)
        perk = Perks(this)

        // Ephemeral state properties -> no structural invalidation
        fireIndex = IntValue(gunDataTag, "FireIndex", 0)
        ammo = IntValue(gunDataTag, "Ammo", onSet = { cachedBackupAmmo = -1 })
        virtualAmmo = IntValue(gunDataTag, "VirtualAmmo", onSet = { cachedBackupAmmo = -1 })
        backupAmmoCount = IntValue(gunDataTag, "BackupAmmoCount")
        ammoSlot = AmmoSlot(gunDataTag)
        burstAmount = IntValue(gunDataTag, "BurstAmount")
        exp = DoubleValue(gunDataTag, "Exp")

        isEmpty = BooleanValue(gunDataTag, "IsEmpty")
        closeHammer = BooleanValue(gunDataTag, "CloseHammer")
        closeStrike = BooleanValue(gunDataTag, "CloseStrike")
        stopped = BooleanValue(gunDataTag, "Stopped")
        forceStop = BooleanValue(gunDataTag, "ForceStop")
        loadIndex = IntValue(gunDataTag, "LoadIndex")
        holdOpen = BooleanValue(gunDataTag, "HoldOpen")
        hideBulletChain = BooleanValue(gunDataTag, "HideBulletChain")
        sensitivity = IntValue(gunDataTag, "Sensitivity")
        heat = DoubleValue(gunDataTag, "Heat")
        shootAnimationTimer = IntValue(gunDataTag, "ShootAnimationTimer")
        shootTimer = IntValue(gunDataTag, "ShootTimer")
        overHeat = BooleanValue(gunDataTag, "OverHeat")
        zooming = BooleanValue(gunDataTag, "Zooming")
        weaponPitch = DoubleValue(gunDataTag, "weaponPitch")
        weaponYaw = DoubleValue(gunDataTag, "weaponYaw")

        val defaultFireMode = get(GunProp.DEFAULT_FIRE_MODE)

        val fireModes = get(AVAILABLE_FIRE_MODES)
        for (i in fireModes.indices) {
            if (fireModes[i].name == defaultFireMode) {
                selectedFireMode.defaultValue = i
                break
            }
        }
    }

    companion object {
        /** Tick interval between backup ammo inventory re-computations. */
        const val BACKUP_AMMO_CACHE_TICKS: Long = 4L

        /**
         * Cached array of all [Perk.Type] entries.
         *
         * Avoids repeated [Array] allocation from [Enum.entries.toTypedArray] inside
         * hot paths such as [get] and [GunEventHandler.tickPerk].
         */
        @JvmField
        val PERK_TYPES: Array<Perk.Type> = Perk.Type.entries.toTypedArray()

        /** Weak LoadingCache for resolving GunData instances from ItemStack references. */
        @JvmField
        val DATA_CACHE: LoadingCache<ItemStack, GunData> = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(object : CacheLoader<ItemStack, GunData>() {
                override fun load(stack: ItemStack): GunData {
                    return GunData(stack)
                }
            })

        /** Creates a new [GunData] instance from an item definition. */
        fun create(item: Item): GunData {
            return from(ItemStack(item))
        }

        /** Retrieves cached or new [GunData] for an [ItemStack]. */
        @JvmStatic
        @JvmOverloads
        fun from(stack: ItemStack, defaultDataSupplier: (() -> DefaultGunData)? = null): GunData {
            if (defaultDataSupplier != null) {
                return GunData(stack, defaultDataSupplier)
            }
            return DATA_CACHE.getUnchecked(stack)
        }

        /** Resolves computed property for given item stack directly. */
        @JvmOverloads
        @JvmStatic
        fun <T> get(stack: ItemStack, prop: GunProp<*, T>, useCache: Boolean = true): T {
            return from(stack).get(prop)
        }

        /** Retrieves default un-modified properties by item registry identifier. */
        @JvmStatic
        fun getDefault(id: String): DefaultGunData {
            val isDefault = !com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.containsKey(id)
            val data = com.atsuishio.superbwarfare.data.CustomData.GUN_DATA.getOrElseGet(id) { DefaultGunData() }
            data.isDefaultData = isDefault
            return data
        }

        /** Retrieves default un-modified properties for item stack. */
        fun getDefault(stack: ItemStack): DefaultGunData {
            return getDefault(stack.item)
        }

        /** Retrieves default un-modified properties for item definition. */
        fun getDefault(item: Item): DefaultGunData {
            return getDefault(getRegistryId(item))
        }

        /** Extracts formatted registry ID from item. */
        fun getRegistryId(item: Item): String {
            var id = item.descriptionId
            id = id.substring(id.indexOf(".") + 1).replace('.', ':')
            return id
        }

        @JvmStatic
        @Suppress("unused")
        @Deprecated("use get() instead", level = DeprecationLevel.ERROR)
        @ApiStatus.ScheduledForRemoval
        fun compute(stack: ItemStack): DefaultGunData {
            error("use get() instead!")
        }

        /** Priority mapping helper for perk execution order. */
        fun getPerkPriority(s: String): Int {
            if (s.isEmpty()) return 2

            return when (s[0]) {
                '@' -> 0
                '!' -> 2
                else -> 1
            }
        }
    }

    override fun hashCode(): Int = stack.hashCode()
}