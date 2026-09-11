package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.AttachmentZoom
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.BACKUP_AMMO_CACHE_TICKS
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.DATA_CACHE
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.DATA_VERSION
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.UUID_CACHE
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.from
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.get
import com.atsuishio.superbwarfare.data.gun.GunData.Companion.getDefault
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AMMO_CONSUMER
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AMMO_COST_PER_SHOOT
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AVAILABLE_FIRE_MODES
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.AVAILABLE_PERKS
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.BOLT_ACTION_TIME
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.DEFAULT_ZOOM
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MAGAZINE
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MAX_ZOOM
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MELEE_DAMAGE
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.MIN_ZOOM
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.PROJECTILE_AMOUNT
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SHOOT_POS
import com.atsuishio.superbwarfare.data.gun.GunProp.Companion.SHOOT_SHAKE
import com.atsuishio.superbwarfare.data.gun.subdata.*
import com.atsuishio.superbwarfare.data.gun.value.*
import com.atsuishio.superbwarfare.event.GunEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.EmptyGunItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage
import com.atsuishio.superbwarfare.perk.Perk
import com.atsuishio.superbwarfare.tools.InventoryTool
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
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
import java.util.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

/**
 * Selects the magazine-level value, falling back to the last configured value
 * when the list is shorter than the requested level.
 */
fun ObjectToList<Int>.atMagazineLevel(level: Int): Int {
    if (list.isEmpty()) return 0
    return list[level.coerceAtLeast(0).coerceAtMost(list.lastIndex)]
}

/**
 * Resolves the current magazine level from the installed attachment definition.
 * Level 0 is the no-attachment/original magazine state.
 */
fun GunData.magazineLevel(): Int {
    val id = attachment.id(AttachmentType.MAGAZINE) ?: return 0
    return AttachmentDefinition.from(id)?.level?.coerceAtLeast(0) ?: 0
}

/**
 * Checks whether the installed barrel attachment is configured as a silencer.
 */
fun GunData.isBarrelSilenced(): Boolean {
    val id = attachment.id(AttachmentType.BARREL) ?: return false
    return AttachmentDefinition.from(id)?.isSilenced == true
}

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
fun ItemStack.toGunData(): GunData? = if (isGunItem()) from(this) else null

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
    stack: ItemStack
) : DefaultDataSupplier<DefaultGunData> {

    /**
     * The target weapon item stack wrapped by this data object.
     *
     * Reassigned in place when this instance is adopted for a newer snapshot of the same gun (see
     * [rebind]); because a [GunData] is looked up by the gun's [uuid], code holding a reference to this
     * instance keeps working across vanilla's client-side [ItemStack] replacement.
     */
    @JvmField
    var stack: ItemStack

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

    /**
     * Optional [DefaultGunData] id override, stored in the gun tag.
     *
     * When non-empty, the baseline is resolved from [CustomData.GUN_DATA] by this id instead of the
     * item's registry id. Vehicle-mounted weapons all share the single `superbwarfare:vehicle_gun`
     * item, so they stamp `<vehicleId>.<weaponKey>` (composed by `VehicleData.weaponDefaultDataId`)
     * onto their stack and resolve their per-vehicle weapon baseline from it — no external supplier
     * injection needed.
     */
    @JvmField
    val defaultDataId: StringValue

    /**
     * Monotonic revision of the persisted gun state.
     *
     * [persist] advances it whenever the persisted content actually changes. Together with [uuid] it
     * lets [from] tell a *newer snapshot of the same gun* (vanilla replaced the client-side [ItemStack])
     * apart from an unrelated stack such as a creative-mode copy — a copy carries an equal revision and
     * must get its own instance. See [GunState.isNewerRevision] for the wraparound-safe comparison.
     */
    val revision: Long
        get() = state.revision

    /** Unique registry identifier string for the underlying item. */
    @JvmField
    val id: String

    /**
     * Set when something outside [GunState] changed data the computed properties depend on — the
     * sections that are still tag-backed (`Perks`, `Attachment`, ammo slots) invalidate through
     * [invalidateProperties].
     *
     * State-driven invalidation does not need this: [get] compares the [state] snapshot the cached
     * properties were derived from.
     */
    private var propertiesInvalidated: Boolean = false

    /** Whether anything ever asked for a write, so [persist] cannot take its "never touched" shortcut. */
    private var mutated: Boolean = false

    /**
     * Marks the cached computed properties stale.
     *
     * For writes that do not go through [update]: the still tag-backed sections (`Perks`, `Attachment`,
     * ammo slots) and callers that mutate loose keys by hand (see `AdjustZoomFovMessage`). Replaces the
     * old `nbtVersion.invalidateStructural()`.
     */
    fun invalidateProperties() {
        propertiesInvalidated = true
        mutated = true
    }

    /**
     * Kotlin-side snapshot of this gun's persisted state — the single source of truth for every scalar
     * field declared below, for both reads and writes.
     *
     * In 1.20 there are no immutable data components: the storage is the mutable [CompoundTag] on the
     * stack, and a write into that tag is immediately live *and* saved. [state] is therefore the
     * decoded mirror of [gunDataTag]: [update] applies a change to it and then writes it straight back
     * into the tag, and the lazily decoded value is dropped whenever the tag is re-read ([rebind]) so
     * the mirror can never be observed half-updated or out of sync.
     */
    private var decodedState: GunState? = null

    var state: GunState
        get() = decodedState ?: GunState.fromTag(gunDataTag).also { decodedState = it }
        private set(value) {
            decodedState = value
        }

    /**
     * Cached [DefaultGunData] baseline, resolved from [defaultDataId] or the gun item itself.
     *
     * [getDefault] is called once per property read during a PMC rebuild, so the resolution (an NBT
     * lookup plus a map lookup) is cached here. It is re-resolved when [defaultDataId] changes or when
     * the datapack data is reloaded ([DATA_VERSION]).
     */
    private var cachedDefaultData: DefaultGunData? = null

    /** [defaultDataId] value that produced [cachedDefaultData]. */
    private var cachedDefaultDataId: String? = null

    /** [DATA_VERSION] snapshot taken when [cachedDefaultData] was resolved. */
    private var cachedDefaultDataVersion: Int = -1

    /** Depth of nested [batch] scopes. Writes to the stack are deferred while this is positive. */
    private var batchDepth: Int = 0

    /** Whether a [batch] (or a deferred [save]) still owes the stack a write. */
    private var persistPending: Boolean = false

    /** Cached snapshot of the item stack used for equality checks. */
    var lastTimeStack: ItemStack? = null

    /** Cached result of the last [countBackupAmmo] inventory computation. */
    @JvmField
    var cachedBackupAmmo: Int = -1

    /** Game time (in ticks) when [cachedBackupAmmo] was last computed. */
    @JvmField
    var cachedBackupAmmoTick: Long = -BACKUP_AMMO_CACHE_TICKS

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

        invalidateProperties()
    }

    /** Returns the underlying [GunItem]. */
    fun item(): GunItem = item

    /** Returns the wrapped [ItemStack]. */
    fun stack(): ItemStack = stack

    /** Returns the root NBT [CompoundTag] — the live tag of the wrapped stack. */
    fun tag(): CompoundTag = tag

    /** Returns the gun data NBT [CompoundTag]. */
    fun data(): CompoundTag = gunDataTag

    /** Returns the perk NBT [CompoundTag]. */
    fun perk(): CompoundTag = perkTag

    /** Returns the attachment NBT [CompoundTag]. */
    fun attachment(): CompoundTag = attachmentTag

    /**
     * Stable identity of this gun, or `null` when the gun was never initialised.
     *
     * Unlike the [ItemStack] reference this stays the same across server syncs, item copies made by
     * vanilla and inventory resyncs, which is what [from] keys the [UUID_CACHE] on.
     */
    val uuid: UUID?
        get() = state.uuid

    /**
     * Returns the default un-modified [DefaultGunData] baseline for this weapon.
     *
     * Resolution order: [defaultDataId] (stamped on the stack, used by vehicle weapons) and then the
     * owning [GunItem]'s own baseline (normally the item's registry id).
     */
    override fun getDefault(): DefaultGunData {
        val defaultDataId = this.defaultDataId.get()

        val cached = cachedDefaultData
        if (cached != null && cachedDefaultDataId == defaultDataId && cachedDefaultDataVersion == DATA_VERSION) {
            return cached
        }

        val resolved = if (defaultDataId.isEmpty()) item.getDefaultData(this) else getDefault(defaultDataId)

        cachedDefaultData = resolved
        cachedDefaultDataId = defaultDataId
        cachedDefaultDataVersion = DATA_VERSION
        return resolved
    }

    /**
     * Sets temporary runtime modifications to default weapon data.
     *
     * @param modification function modifying default gun data.
     */
    fun setTempModifications(modification: Function<DefaultGunData, DefaultGunData>) {
        tempModifications = modification
        invalidateProperties()
    }

    /** Clears temporary runtime weapon modifications. */
    fun clearTempModifications() {
        tempModifications = null
        invalidateProperties()
    }

    private val jsonPropModifier = JsonPropertyModifier(GunProp.entries)
    private val attachmentJsonPropModifier = JsonPropertyModifier(GunProp.entries)
    private var tempModifications: Function<DefaultGunData, DefaultGunData>? = null
    private val pmcInstance: PMC<GunData, DefaultGunData> by lazy { PMC(this) }

    /** [GunState] snapshot the cached properties in [pmcInstance] were derived from. */
    private var pmcState: GunState? = null

    /** [DATA_VERSION] value the cached properties were derived from. */
    private var pmcDataVersion: Int = -1

    /**
     * Resolves a computed weapon property using lazy PMC caching.
     *
     * The computed values are cached together with the [GunState] snapshot they were derived from, so
     * the cache survives every change that cannot affect them:
     *
     *  * a state change that is not structural ([GunState.structurallyDiffersFrom]) keeps the values;
     *  * [NbtVersion.structural] is still the escape hatch for sections that are not modelled by
     *    [GunState] yet (`Perks`, `Attachment`, ...), which call [invalidateProperties];
     *  * [DATA_VERSION] covers datapack reloads, without having to recreate any instance.
     *
     * The fast path is two field reads and a reference compare.
     *
     * @param prop the target weapon property key.
     * @return calculated value for the given property.
     */
    @Suppress("unchecked_cast")
    fun <T> get(prop: GunProp<*, T>): T {
        val current = state
        val dataVersion = DATA_VERSION
        val cached = pmcState

        if (cached !== current || propertiesInvalidated || pmcDataVersion != dataVersion) {
            val rebuild = cached == null ||
                    propertiesInvalidated ||
                    pmcDataVersion != dataVersion ||
                    current.structurallyDiffersFrom(cached)

            if (rebuild) {
                rebuildProperties()
            }

            pmcState = current
            propertiesInvalidated = false
            pmcDataVersion = dataVersion
        }

        return pmcInstance[prop]
    }

    /** Runs the property modification pipeline into [pmcInstance]. */
    private fun rebuildProperties() {
        pmcInstance.reset()

        // 1. Property override tag
        jsonPropModifier.update(propertyOverrideString.get())
        jsonPropModifier.modifyProperty(pmcInstance)

        // 2. Gun item level modifiers
        item.modifyProperty(pmcInstance)

        // 3. Attachments
        attachmentJsonPropModifier.update(`object` = null)
        for (instance in attachment.installed()) {
            attachmentOption(instance.slot, instance.id)?.let { option ->
                attachmentJsonPropModifier.update(option.override)
                attachmentJsonPropModifier.modifyProperty(pmcInstance)
            }
            instance.definition.modifyProperty(pmcInstance)
        }

        // 4. FireMode modifiers
        selectedFireModeInfo(pmcInstance[AVAILABLE_FIRE_MODES]).modifyProperty(pmcInstance)

        // 5. AmmoConsumer modifiers
        selectedAmmoConsumer(pmcInstance[AMMO_CONSUMER]).modifyProperty(pmcInstance)

        // 6. Active Perks
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

        // 7. Global property bounds limit
        GunProp.modifyProperty(pmcInstance)
    }

    /**
     * Checks if the shooter has infinite backup ammunition available.
     *
     * @param shooter the entity attempting to fire or reload.
     * @return `true` if creative mode, infinite consumer, or creative ammo box is present.
     */
    fun hasInfiniteBackupAmmo(shooter: Entity?): Boolean {
        return shooter is Player && shooter.isCreative
                || shooter?.let { InfiniteAmmoCapability.get(it) }?.hasInfiniteAmmo ?: false
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
        if (scopeZoomDefinition() == null) return 1.25
        return get(MIN_ZOOM)
    }

    /**
     * Calculates maximum scope zoom ratio.
     *
     * @return maximum allowed zoom value.
     */
    fun maxZoom(): Double {
        if (scopeZoomDefinition() == null) return 114514.0
        return get(MAX_ZOOM)
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
     * Gets the configured zoom level to use while aiming and moving.
     *
     * @return the movement zoom level, or null when the installed scope does not enable it.
     */
    fun movingZoom(): Double? {
        val id = attachment.id(AttachmentType.SCOPE) ?: return null
        return AttachmentDefinition.from(id)?.scopeInfo?.movingZoom
    }

    private fun scopeZoomDefinition(): AttachmentZoom? {
        val id = attachment.id(AttachmentType.SCOPE) ?: return null
        val definition = AttachmentDefinition.from(id) ?: return null
        return definition.scopeZoom(attachment.scopeMode(AttachmentType.SCOPE))
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

        GunActionStepExecutor.triggerNoAmmo(this)
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
        this.reload.totalTicks.reset()
        this.reload.finishTimer.reset()
        this.reload.finishTotalTicks.reset()
        this.reload.prepareTimer.reset()
        this.reload.prepareLoadTimer.reset()
        this.reload.reloadStarter.finish()
        this.reload.singleReloadStarter.finish()
        this.bolt.actionTimer.reset()
        this.bolt.totalTicks.reset()
        this.bolt.needed.reset()
        this.charge.starter.finish()
        this.charge.timer.reset()

        invalidateProperties()
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

        invalidateProperties()
    }

    /** Starts manual bolt-action sequence. */
    fun startBolt() {
        this.bolt.start(get(BOLT_ACTION_TIME) + 1)

        invalidateProperties()
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
        if (hasInfiniteBackupAmmo(entity)) return Int.MAX_VALUE

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
        if (remaining <= 0 || hasInfiniteBackupAmmo(entity)) return

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

        invalidateProperties()
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

    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?, power: Double) {
        this.item.shoot(this, entity, spread, zoom, uuid, power)
    }

    /** Fires projectile targeting specific world position. */
    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?, targetPos: Vec3?) {
        this.item.shoot(this, entity, spread, zoom, uuid, targetPos)
    }

    fun shoot(entity: Entity, spread: Double, zoom: Boolean, uuid: UUID?, targetPos: Vec3?, power: Double) {
        this.item.shoot(this, entity, spread, zoom, uuid, targetPos, power)
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

    /** Gets attachments allowed on [slot] from the gun data definition. */
    fun availableAttachments(slot: AttachmentType): List<ResourceLocation> {
        return getDefault().availableAttachments[slot.attachmentName]
            .orEmpty()
            .mapNotNull { ResourceLocation.tryParse(it.value.id) }
    }

    /** Returns the weapon-level option for [id] installed in [slot], if declared. */
    fun attachmentOption(slot: AttachmentType, id: ResourceLocation): AttachmentOption? {
        val idString = id.toString()
        return getDefault().availableAttachments[slot.attachmentName]
            .orEmpty()
            .firstOrNull { it.value.id == idString }
            ?.value
    }

    /** Checks whether [id] can be installed in [slot] for this gun. */
    fun canInstall(slot: AttachmentType, id: ResourceLocation): Boolean {
        if (id !in availableAttachments(slot)) return false
        val definition = AttachmentDefinition.from(id) ?: return false
        return definition.slot == slot
    }

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

    /** Checks if the installed scope has data-driven zoom state. */
    fun hasAdjustableScopeZoom(): Boolean {
        return scopeZoomDefinition() != null
    }

    /** Checks if scope zoom adjustment is supported. */
    fun canAdjustZoom(): Boolean = item.canAdjustZoom(this) || hasAdjustableScopeZoom()

    /** Checks if scope switching is supported. */
    fun canSwitchScope(): Boolean {
        val attachmentSupportsSwitching = attachment.id(AttachmentType.SCOPE)
            ?.let { AttachmentDefinition.from(it)?.supportsScopeSwitching() }
            ?: false
        return item.canSwitchScope(this) || attachmentSupportsSwitching
    }

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
     * Applies [block] to [state] and writes the result back into the gun tag.
     *
     * This is the write path for everything the value wrappers used to mutate in place: the returned
     * state becomes the new snapshot, the change is classified as structural or state-only so the PMC
     * cache is invalidated only when needed, and the change lands in the stack's NBT immediately.
     * Callers therefore never have to remember a `save()`.
     *
     * Inside a [batch] only the revision bookkeeping is deferred to the end of that scope; the tag
     * itself is written at once, so a read of [state] or of the tag is always current.
     */
    fun update(block: (GunState) -> GunState): GunState {
        val previous = state
        val next = block(previous)
        if (next == previous) return previous

        applyState(previous, next)
        requestPersist()

        return next
    }

    /**
     * Applies [block] to [state] **without** touching the gun tag.
     *
     * For client-side predictions. The gun stack is server-authoritative, so such a write must stay in
     * memory: persisting it would fight the next server snapshot and, because it advances
     * [GunState.revision], would also stop this gun from adopting that snapshot. The next sync
     * overwrites the prediction through [rebind].
     */
    fun updateLocal(block: (GunState) -> GunState): GunState {
        val previous = state
        val next = block(previous)
        if (next == previous) return previous

        applyState(previous, next, writeTag = false)

        return next
    }

    /**
     * Installs [next] as the current snapshot.
     *
     * No invalidation is needed here: [get] compares the [state] snapshot its cached properties were
     * derived from, so a structural change is detected by that comparison, and a non-structural one
     * keeps the computed values.
     */
    private fun applyState(previous: GunState, next: GunState, writeTag: Boolean = true) {
        state = next
        mutated = true
        if (writeTag) next.writeInto(gunDataTag)


        // Side effects the old value wrappers performed through their `onSet` callbacks.
        if (next.ammo != previous.ammo || next.virtualAmmo != previous.virtualAmmo) {
            cachedBackupAmmo = -1
        }
        if (next.defaultDataId != previous.defaultDataId) {
            cachedDefaultData = null
            cachedDefaultDataId = null
        }
    }

    /**
     * Groups the [update] calls made by [block] into a single revision bump.
     *
     * A tick touches several timers, ammo and heat; each of those writes already reaches the gun tag
     * immediately, so what this scope saves is the per-write bookkeeping — most notably the [revision]
     * bump, which would otherwise advance dozens of times per tick and make every client resync look
     * like a brand-new gun. [state] and the tag are current at all times, even inside the scope.
     *
     * The deferred work is driven by this scope, not by the callers, and it also runs when [block]
     * throws. A batch must not span ticks — that is what keeps the pending window inside one
     * synchronous scope.
     */
    fun <T> batch(block: () -> T): T {
        batchDepth++
        try {
            return block()
        } finally {
            batchDepth--
            if (batchDepth == 0) flush()
        }
    }

    /** Performs the bookkeeping a [batch] deferred. No-op when nothing is pending. */
    fun flush() {
        if (!persistPending) return
        persistPending = false
        persist(compare = true)
    }

    /**
     * Outside a [batch] the bookkeeping runs at once, skipping the "did the tag change" comparison:
     * [update] only routes here after the state actually changed, and the serialized form is a pure
     * function of the state, so the tag is guaranteed to differ.
     */
    private fun requestPersist() {
        if (batchDepth > 0) {
            persistPending = true
        } else {
            persist(compare = false)
        }
    }

    /**
     * Flushes tag-backed changes ( `Perks`, `Attachment`, ammo slots, loose keys) and bumps [revision].
     *
     * Scalars no longer need this — they persist through [update]. Kept as a public entry point because
     * the existing call sites use it; calling it when nothing changed is a cheap no-op.
     */
    fun save() {
        if (batchDepth > 0) {
            persistPending = true
            return
        }
        persist(compare = true)
    }

    /**
     * Advances [GunState.revision] when the persisted content actually changed, which is what lets a
     * remote copy of this gun recognise this instance as its predecessor (see [rebind]).
     *
     * The state scalars themselves are already in the tag by the time this runs — they are written by
     * [applyState]; what happens here is the once-per-change revision bookkeeping.
     *
     * @param compare whether to compare the current tag against the last persisted snapshot first.
     *   Needed for tag-backed sections, where a mutation may or may not have changed anything; skipped
     *   on the [update] path, where a change is already known to have happened.
     */
    private fun persist(compare: Boolean) {
        // Fast-path: nothing was ever mutated on this instance, so the tag cannot be out of date.
        if (!mutated) return

        // Make this instance reachable by its own identity, so that a remote snapshot of the same gun
        // can adopt it (see Companion.from).
        state.uuid?.let { UUID_CACHE.put(it, this) }

        // Mirror the immutable state into the sub-compound that is still the serialized view. In 1.20 the
        // tag is mutable and lives inside the stack, so writing here *is* the persistence — nothing has
        // to be written back to the stack afterwards, unlike 1.21's DataComponents.
        state.writeInto(gunDataTag)

//        val keysToRemove = mutableListOf<String>()
//        for (key in perkTag.allKeys) {
//            val compoundTag = perkTag.get(key) as? CompoundTag
//            if (compoundTag?.isEmpty ?: false) {
//                keysToRemove.add(key)
//            }
//        }
//        keysToRemove.forEach { key -> perkTag.remove(key) }
//
//        val cleanedTag = tag.copy()
//
//        if (perkTag.isEmpty) {
//            cleanedTag.remove(KEY_PERKS)
//        }
//
//        if (attachmentTag.isEmpty) {
//            cleanedTag.remove(KEY_ATTACHMENTS)
//        }
//
//        if (gunDataTag.isEmpty) {
//            cleanedTag.remove(KEY_GUN_DATA)
//        }
//
//        if (tag.isEmpty) {
//            stack.tag = null
//            return
//        }

//        if (compare) {
//            if (lastTimeStack?.tag == cleanedTag) return
//
//            // Content changed: remember what the persistence is about to look like, so an unchanged
//            // state never bumps the revision on a later call.
//            lastTimeStack = stack.copy()
//        }

        // Content changed: advance the revision, mirrored into the state and the tag. Done after the
        // comparison above so an unchanged state never bumps it.
        state = state.copy(revision = state.revision + 1)
        gunDataTag.putLong(KEY_REVISION, state.revision)

//        lastTimeStack?.getOrCreateTag()?.getCompound(KEY_GUN_DATA)?.putLong(KEY_REVISION, state.revision)
    }

    /**
     * Re-binds this instance to [newStack], a newer snapshot of the same logical gun.
     *
     * The persisted tag is re-read into the *same* [CompoundTag] instances ([tag], [gunDataTag],
     * [perkTag], [attachmentTag]) so every value wrapper and subdata handler stays valid, [state] is
     * re-decoded from it, and the structural version is invalidated because the persisted content did
     * change. Preserving the instance itself is the point: it keeps client-side holders (renderers,
     * animation state, tooltips) and [uuid]-keyed lookups working instead of being invalidated on every
     * [ItemStack] resync.
     *
     * Must run on the game thread: it mutates an instance other code may already be using. Its only
     * caller is the [DATA_CACHE] loader, which is reachable from main-thread paths (client
     * render/handlers, server gameplay). Codec `decode` implementations that build a [GunData] run on the
     * netty thread instead, so they must never trigger adoption for a gun that already has a live
     * instance — vehicle-gun stacks therefore stay UUID-less (VehicleGunItem never writes one).
     */
    private fun rebind(newStack: ItemStack) {
        val incoming = newStack.getOrCreateTag()

        this.stack = newStack

        reloadTagFrom(incoming)
        state = GunState.fromTag(gunDataTag)

        // The remote snapshot supersedes anything a batch was still holding back.
        persistPending = false

        // Bookkeeping that depends on the previous tag / stack contents.
        this.lastTimeStack = null
        this.cachedBackupAmmo = -1
        cachedDefaultData = null
        cachedDefaultDataId = null

        invalidateProperties()
    }

    /**
     * Folds [incoming] into the existing tag instances, preserving their identity.
     *
     * Mutating the existing compounds (instead of replacing them) is what keeps every [IntValue] /
     * [DoubleValue] / subdata handler in this [GunData] pointing at live data.
     */
    private fun reloadTagFrom(incoming: CompoundTag) {
        val incomingGunData = incoming.getCompound(KEY_GUN_DATA)
        val incomingPerks = incoming.getCompound(KEY_PERKS)
        val incomingAttachments = incoming.getCompound(KEY_ATTACHMENTS)

        clearTag(tag)
        clearTag(gunDataTag)
        clearTag(perkTag)
        clearTag(attachmentTag)

        gunDataTag.merge(incomingGunData)
        perkTag.merge(incomingPerks)
        attachmentTag.merge(incomingAttachments)

        tag.put(KEY_GUN_DATA, gunDataTag)
        tag.put(KEY_PERKS, perkTag)
        tag.put(KEY_ATTACHMENTS, attachmentTag)

        // Remaining root entries (other mods' custom data, ScopeAlt, CustomRPM, ...).
        for (key in incoming.allKeys) {
            if (key == KEY_GUN_DATA || key == KEY_PERKS || key == KEY_ATTACHMENTS) continue
            incoming.get(key)?.let { tag.put(key, it) }
        }
    }

    /** Removes every entry of [compound], keeping the tag instance itself alive. */
    private fun clearTag(compound: CompoundTag) {
        for (key in compound.allKeys.toList()) {
            compound.remove(key)
        }
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
        // The copy re-decodes the state from the stack, so deferred writes have to land first.
        flush()
        return GunData(this.stack.copy())
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
        val realGunItem = stack.item as? GunItem
        val useEmptyGunData = realGunItem == null || stack.isEmpty
        val gunItem = if (useEmptyGunData) ModItems.EMPTY_GUN.get() as GunItem else realGunItem
        this.item = gunItem
        this.stack = stack
        this.id = if (useEmptyGunData) EmptyGunItem.EMPTY_GUN_ID else getRegistryId(stack.item)

        this.tag = if (useEmptyGunData) CompoundTag() else stack.getOrCreateTag()

        gunDataTag = getOrPut(KEY_GUN_DATA)
        perkTag = getOrPut(KEY_PERKS)
        attachmentTag = getOrPut(KEY_ATTACHMENTS)

        // The immutable state is the source of truth for every scalar below; the sub-compound above is
        // kept in sync as its serialized mirror, so sub-data handlers and loose keys still work.
        state = GunState.fromTag(gunDataTag)

        // Structural properties -> invalidate PMC pipeline on change
        propertyOverrideString = StateStringValue(
            this, { it.override }, { s, v -> s.copy(override = v) }
        )
        defaultDataId = StateStringValue(
            this, { it.defaultDataId }, { s, v -> s.copy(defaultDataId = v) }
        )
        selectedAmmoType = StateIntValue(
            this, { it.selectedAmmoType }, { s, v -> s.copy(selectedAmmoType = v) }
        )
        selectedFireMode = StateDefaultedIntValue(
            this, { it.selectedFireMode }, { s, v -> s.copy(selectedFireMode = v) }
        )
        level = StateIntValue(
            this, { it.level }, { s, v -> s.copy(level = v) }
        )

        // Subdata handlers
        reload = Reload(this)
        charge = Charge(this)
        bolt = Bolt(this)
        attachment = Attachment(this)
        perk = Perks(this)

        // Ephemeral state properties -> no structural invalidation (ammo/virtualAmmo excepted: a perk
        // reads ammo while computing properties, see GunState.structurallyDiffersFrom)
        fireIndex = StateIntValue(this, { it.fireIndex }, { s, v -> s.copy(fireIndex = v) })
        ammo = StateIntValue(this, { it.ammo }, { s, v -> s.copy(ammo = v) })
        virtualAmmo = StateIntValue(this, { it.virtualAmmo }, { s, v -> s.copy(virtualAmmo = v) })
        backupAmmoCount = StateIntValue(
            this, { it.backupAmmoCount }, { s, v -> s.copy(backupAmmoCount = v) }
        )
        ammoSlot = AmmoSlot(gunDataTag)
        burstAmount = StateIntValue(this, { it.burstAmount }, { s, v -> s.copy(burstAmount = v) })
        exp = StateDoubleValue(this, { it.exp }, { s, v -> s.copy(exp = v) })

        isEmpty = StateBooleanValue(this, { it.isEmpty }, { s, v -> s.copy(isEmpty = v) })
        closeHammer = StateBooleanValue(this, { it.closeHammer }, { s, v -> s.copy(closeHammer = v) })
        closeStrike = StateBooleanValue(this, { it.closeStrike }, { s, v -> s.copy(closeStrike = v) })
        stopped = StateBooleanValue(this, { it.stopped }, { s, v -> s.copy(stopped = v) })
        forceStop = StateBooleanValue(this, { it.forceStop }, { s, v -> s.copy(forceStop = v) })
        loadIndex = StateIntValue(this, { it.loadIndex }, { s, v -> s.copy(loadIndex = v) })
        holdOpen = StateBooleanValue(this, { it.holdOpen }, { s, v -> s.copy(holdOpen = v) })
        hideBulletChain = StateBooleanValue(
            this, { it.hideBulletChain }, { s, v -> s.copy(hideBulletChain = v) }
        )
        sensitivity = StateIntValue(this, { it.sensitivity }, { s, v -> s.copy(sensitivity = v) })
        heat = StateDoubleValue(this, { it.heat }, { s, v -> s.copy(heat = v) })
        shootAnimationTimer = StateIntValue(
            this, { it.shootAnimationTimer }, { s, v -> s.copy(shootAnimationTimer = v) }
        )
        shootTimer = StateIntValue(this, { it.shootTimer }, { s, v -> s.copy(shootTimer = v) })
        overHeat = StateBooleanValue(this, { it.overHeat }, { s, v -> s.copy(overHeat = v) })
        zooming = StateBooleanValue(this, { it.zooming }, { s, v -> s.copy(zooming = v) })
        weaponPitch = StateDoubleValue(this, { it.weaponPitch }, { s, v -> s.copy(weaponPitch = v) })
        weaponYaw = StateDoubleValue(this, { it.weaponYaw }, { s, v -> s.copy(weaponYaw = v) })

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
        const val BACKUP_AMMO_CACHE_TICKS: Long = 10L

        /** Root gun tag key inside the item stack's NBT. */
        const val KEY_GUN_DATA = GunState.KEY_GUN_DATA

        /** Perk tag key inside the item stack's NBT. */
        private const val KEY_PERKS = "Perks"

        /** Attachment tag key inside the item stack's NBT. */
        private const val KEY_ATTACHMENTS = "Attachments"

        /** [GunState.defaultDataId] key inside the gun tag. */
        const val KEY_DEFAULT_DATA = GunState.KEY_DEFAULT_DATA

        /** Identity key inside the gun tag, written by `GunItem.init`. */
        const val KEY_UUID = GunState.KEY_UUID

        /** [GunState.revision] key inside the gun tag. */
        const val KEY_REVISION = GunState.KEY_REVISION

        /**
         * Datapack data version, bumped whenever [CustomData.GUN_DATA] / [CustomData.VEHICLE_DATA]
         * are (re)loaded. Instances compare it against their own snapshot to re-resolve their cached
         * [DefaultGunData] baseline after a `/reload`.
         */
        @JvmField
        var DATA_VERSION: Int = 0

        /**
         * Cached array of all [Perk.Type] entries.
         *
         * Avoids repeated [Array] allocation from [Enum.entries.toTypedArray] inside
         * hot paths such as [get] and [GunEventHandler.tickPerk].
         */
        @JvmField
        val PERK_TYPES: Array<Perk.Type> = Perk.Type.entries.toTypedArray()

        /**
         * Identity cache resolving a [GunData] per live [ItemStack].
         *
         * Uses *soft* values on purpose: a weak value could be collected while its stack is still alive,
         * and the next lookup would then build a second [GunData] for the same stack. Both instances
         * would keep their own [state] snapshot and write the whole tag on every change, so they would
         * overwrite each other's fields — which shows up as gun state that stops updating. Soft values
         * keep the instance for as long as the JVM is not actually short on memory, and a collection
         * stays harmless because the stack is the source of truth.
         */
        @JvmField
        val DATA_CACHE: LoadingCache<ItemStack, GunData> = CacheBuilder.newBuilder()
            .weakKeys()
            .softValues()
            .build(object : CacheLoader<ItemStack, GunData>() {
                override fun load(stack: ItemStack): GunData {
                    // Vanilla replaces the client-side ItemStack on every sync, so an identity-keyed
                    // lookup misses there. Fall back to the gun's stable identity.
                    val gunTag = readGunTag(stack)
                    val uuid = readUuid(gunTag)

                    if (uuid != null) {
                        val existing = UUID_CACHE.getIfPresent(uuid)
                        val incomingRevision = gunTag?.getLong(KEY_REVISION) ?: 0L

                        if (existing != null) {
                            if (existing.stack === stack) {
                                // Same stack instance: reuse it rather than building a duplicate.
                                return existing
                            }
                            // Adopt only a newer snapshot of the same gun (wraparound-safe comparison).
                            // An equal or older revision means an unrelated stack — a creative-mode copy
                            // carries the same revision and must get its own instance, or the two would
                            // fight over one GunData (and over which stack writes go to).
                            if (GunState.isNewerRevision(incomingRevision, existing.revision)) {
                                existing.rebind(stack)
                                return existing
                            }
                        }
                    }

                    val created = GunData(stack)
                    if (uuid != null) UUID_CACHE.put(uuid, created)

                    return created
                }
            })

        /**
         * Adoption registry: gun [uuid] -> the live [GunData] instance for that logical gun.
         *
         * Strong values on purpose — the instance has to survive between a server sync and the next
         * lookup for adoption (and for the "same stack instance" reuse above) to happen at all.
         *
         * No access expiry: the identity-cache fast path never touches this cache, so an expiry would
         * silently drop the entry for a gun that is being used normally, and the next identity-cache miss
         * would then build a second instance for the same stack. Size-bounded instead.
         */
        @JvmField
        val UUID_CACHE: Cache<UUID, GunData> = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .build()

        /** Reads the gun sub-tag of [stack] without constructing a [GunData]. */
        private fun readGunTag(stack: ItemStack): CompoundTag? =
            stack.getOrCreateTag().getCompound(KEY_GUN_DATA)

        /** Reads the gun identity out of [gunTag], or `null` when the gun was never initialised. */
        private fun readUuid(gunTag: CompoundTag?): UUID? =
            if (gunTag != null && gunTag.hasUUID(KEY_UUID)) gunTag.getUUID(KEY_UUID) else null

        /** Creates a new [GunData] instance from an item definition. */
        fun create(item: Item): GunData {
            return from(ItemStack(item))
        }

        /** Retrieves cached or new [GunData] for an [ItemStack]. */
        @JvmStatic
        fun from(stack: ItemStack): GunData {
            return DATA_CACHE.getUnchecked(stack)
        }

        /**
         * Stamps a [defaultDataId] onto [stack] *before* any [GunData] is created for it.
         *
         * Vehicle-mounted weapons all share the single `superbwarfare:vehicle_gun` item id, so they
         * cannot resolve their baseline from the item alone; this writes the per-vehicle weapon id
         * (`<vehicleId>.<weaponKey>`, composed by `VehicleData.weaponDefaultDataId`) into the gun tag
         * so the resulting [GunData] resolves it from its own stack. No-op when already set.
         */
        @JvmStatic
        fun setDefaultDataId(stack: ItemStack, defaultDataId: String) {
            if (defaultDataId.isEmpty()) return

            val tag = stack.getOrCreateTag()
            val gunDataTag = if (tag.contains(KEY_GUN_DATA, Tag.TAG_COMPOUND.toInt())) {
                tag.getCompound(KEY_GUN_DATA)
            } else {
                CompoundTag().also { tag.put(KEY_GUN_DATA, it) }
            }

            if (gunDataTag.getString(KEY_DEFAULT_DATA) == defaultDataId) return

            gunDataTag.putString(KEY_DEFAULT_DATA, defaultDataId)
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
            val isDefault = !CustomData.GUN_DATA.containsKey(id)
            val data = CustomData.GUN_DATA.getOrElseGet(id) { DefaultGunData() }
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
