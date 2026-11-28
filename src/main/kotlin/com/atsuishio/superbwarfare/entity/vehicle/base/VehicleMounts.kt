package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.ProjectileInfo
import com.atsuishio.superbwarfare.data.launchable.LaunchableEntityTool
import com.atsuishio.superbwarfare.data.launchable.ShootData
import com.atsuishio.superbwarfare.data.vehicle.VehicleMountDefinition
import com.atsuishio.superbwarfare.data.vehicle.VehicleMountDefinition.Action.Type
import com.atsuishio.superbwarfare.data.vehicle.VehicleMountSlot
import com.atsuishio.superbwarfare.entity.projectile.IBulletProperties
import com.atsuishio.superbwarfare.entity.vehicle.remote.RCOperatorBodyEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.DamageHandler
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries
import java.util.*
import kotlin.math.abs

open class VehicleMounts(protected val vehicle: VehicleEntity) {
    open class Installed(val definitionId: ResourceLocation, val stack: ItemStack)

    open class Active(
        val slot: VehicleMountSlot,
        val installed: Installed,
        val definition: VehicleMountDefinition,
    )

    private var nextUseTick = Long.MIN_VALUE
    private var releasing = false
    private val mountedGuns = mutableMapOf<String, VehicleMountedGun>()
    private var clientGunSnapshot: CompoundTag? = null
    private var clientGunSeat = -1
    private var clientGunData: GunData? = null

    open fun slots(): List<VehicleMountSlot> = vehicle.computed().mountSlots
        .filter { it.id.isNotBlank() && (it.capacity > 0 || it.builtInDefinition != null) }.distinctBy { it.id }

    open fun getInstalled(slotId: String): Installed? {
        val tag = vehicle.entityData.get(VehicleEntity.MOUNT_STATE).getCompound(slotId)
        val definitionId = ResourceLocation.tryParse(tag.getString("DefinitionId")) ?: return null
        // ItemStack.of keeps the nested item tag, so decode a copy of the synced snapshot.
        val stack = ItemStack.of(tag.getCompound("Stack").copy())
        return if (stack.isEmpty) null else Installed(definitionId, stack)
    }

    open fun getDefinition(id: ResourceLocation): VehicleMountDefinition? = CustomData.VEHICLE_MOUNTS[id.toString()]

    open fun occupiedSlotIds(): Set<String> = vehicle.entityData.get(VehicleEntity.MOUNT_STATE).allKeys.toSet()

    open fun setInstalled(slotId: String, installed: Installed?) {
        if (vehicle.level() !is ServerLevel) return
        mountedGuns.remove(slotId)?.stop()
        if (installed != null && getDefinition(installed.definitionId)?.action?.type == Type.GUN && GunItem.isHeldWeapon(installed.stack)) {
            mountedGuns[slotId] = VehicleMountedGun(installed)
        }
        syncInstalled(slotId, installed)
    }

    protected open fun syncInstalled(slotId: String, installed: Installed?) {
        val state = vehicle.entityData.get(VehicleEntity.MOUNT_STATE).copy()
        if (installed == null || installed.stack.isEmpty) {
            state.remove(slotId)
        } else {
            val tag = CompoundTag()
            tag.putString("DefinitionId", installed.definitionId.toString())
            tag.put("Stack", installed.stack.save(CompoundTag()))
            state.put(slotId, tag)
        }
        vehicle.entityData.set(VehicleEntity.MOUNT_STATE, state)
    }

    open fun worldPosition(slot: VehicleMountSlot, additionalOffset: Vec3 = Vec3.ZERO, partialTick: Float = 1f): Vec3 {
        val offset = slot.offset.add(additionalOffset)
        val point = transformPosition(vehicle.getTransformFromString(slot.transform, partialTick), offset.x, offset.y, offset.z)
        return Vec3(point.x, point.y, point.z)
    }

    open fun previewInstallation(slotId: String, source: ItemStack): Installed? {
        if (releasing || vehicle.isWreck || vehicle.health <= 0 || vehicle.isRemoved || source.isEmpty ||
            getInstalled(slotId) != null) return null
        val slot = slots().firstOrNull { it.id == slotId } ?: return null
        if (slot.builtInDefinition != null) return null
        for (definitionId in slot.definitions) {
            val definition = getDefinition(definitionId) ?: continue
            if (!definition.ingredient.matches(source)) continue
            val count = minOf(capacity(slot, definition, source), source.count)
            if (count <= 0) continue
            val stack = source.copyWithCount(count)
            if (definition.action.type != Type.GUN || supportsGun(stack)) return Installed(definitionId, stack)
        }
        return null
    }

    open fun capacity(slot: VehicleMountSlot, definition: VehicleMountDefinition?, stack: ItemStack): Int {
        val type = definition?.action?.type
        val actionLimit = if (type == Type.DETONATE || type == Type.GUN) 1 else 64
        return minOf(slot.capacity, definition?.capacity ?: 64, stack.maxStackSize, actionLimit)
    }

    open fun install(slotId: String, source: ItemStack, consumeItems: Boolean = true): Boolean {
        if (vehicle.level() !is ServerLevel) return false
        val installed = previewInstallation(slotId, source) ?: return false
        setInstalled(slotId, installed)
        if (consumeItems) source.shrink(installed.stack.count)
        return true
    }

    open fun uninstall(slotId: String): ItemStack {
        if (vehicle.level() !is ServerLevel || releasing || vehicle.isRemoved) return ItemStack.EMPTY
        val gun = mountedGuns.remove(slotId)?.also { it.stop() }
        val installed = gun?.installed ?: getInstalled(slotId) ?: return ItemStack.EMPTY
        setInstalled(slotId, null)
        return installed.stack
    }

    protected open fun activeMounts(): List<Active> = slots().mapNotNull { slot ->
        if (slot.builtInDefinition != null) return@mapNotNull null
        val installed = getInstalled(slot.id) ?: return@mapNotNull null
        val definition = getDefinition(installed.definitionId) ?: return@mapNotNull null
        if (installed.definitionId !in slot.definitions || !definition.ingredient.matches(installed.stack)) return@mapNotNull null
        Active(slot, installed, definition)
    }

    protected open fun builtInAction(slot: VehicleMountSlot): VehicleMountDefinition.Action? =
        slot.builtInDefinition?.let(::getDefinition)?.action?.takeIf { it.type == Type.DROP_ITEM }

    open fun canControl(seat: Int): Boolean = seat >= 0 && (
        activeMounts().any { it.slot.controlSeat == seat } ||
            vehicle.hasContainer() && slots().any { it.controlSeat == seat && builtInAction(it) != null }
        )

    protected open fun supportsGun(stack: ItemStack): Boolean {
        if (!GunItem.isHeldWeapon(stack)) return false
        val data = GunData.from(stack)
        return !data.meleeOnly() && !data.item.useSpecialFireProcedure(data)
    }

    open fun mountedGunData(player: Player): GunData? {
        if (vehicle.level().isClientSide) {
            if (player.vehicle !== vehicle || vehicle.isWreck || vehicle.health <= 0) return null
            val snapshot = vehicle.entityData.get(VehicleEntity.MOUNT_STATE)
            val seat = vehicle.getSeatIndex(player)
            if (snapshot !== clientGunSnapshot || seat != clientGunSeat) {
                clientGunSnapshot = snapshot
                clientGunSeat = seat
                clientGunData = gunMount(player)?.installed?.stack?.let(GunData::from)
            }
            return clientGunData
        }
        val mount = gunMount(player) ?: return null
        return gunRuntime(mount).data
    }

    @JvmOverloads
    open fun gunTrigger(player: Player, gunId: UUID, pressed: Boolean, renew: Boolean = false) {
        if (vehicle.level() !is ServerLevel) return
        val mount = gunMount(player) ?: return
        val gun = gunRuntime(mount)
        if (gun.data.uuid == gunId) gun.trigger(player, pressed, renew)
    }

    open fun reloadGun(player: Player): Boolean {
        if (vehicle.level() !is ServerLevel) return false
        val mount = gunMount(player) ?: return false
        val gun = gunRuntime(mount)
        gun.reload(player)
        syncInstalled(mount.slot.id, gun.installed)
        return true
    }

    open fun tickGuns() {
        if (vehicle.level() !is ServerLevel || vehicle.isWreck || vehicle.health <= 0 || vehicle.isRemoved ||
            vehicle.entityData.get(VehicleEntity.MOUNT_STATE).isEmpty) return
        val mounts = activeMounts().filter { it.definition.action.type == Type.GUN && GunItem.isHeldWeapon(it.installed.stack) }
        val activeIds = mounts.mapTo(hashSetOf()) { it.slot.id }
        for (id in mountedGuns.keys.filter { it !in activeIds }) {
            mountedGuns.remove(id)?.let {
                it.stop()
                syncInstalled(id, it.installed)
            }
        }
        for (mount in mounts) {
            val gun = gunRuntime(mount)
            val seat = mount.slot.controlSeat
            val operator = (vehicle.getNthEntity(seat) as? Player)?.takeUnless { vehicle.hasWeapon(seat) }
            gun.tick(vehicle, mount.slot, worldPosition(mount.slot, mount.definition.action.offset), operator)
            if (mountedGuns[mount.slot.id] === gun && !vehicle.isRemoved) syncInstalled(mount.slot.id, gun.installed)
        }
    }

    open fun stopGuns() {
        if (vehicle.level() !is ServerLevel) return
        for ((id, gun) in mountedGuns) {
            gun.stop()
            syncInstalled(id, gun.installed)
        }
    }

    open fun resetGuns() {
        mountedGuns.clear()
    }

    protected open fun gunRuntime(mount: Active): VehicleMountedGun =
        mountedGuns.getOrPut(mount.slot.id) { VehicleMountedGun(mount.installed) }

    protected open fun gunMount(player: Player): Active? {
        if (vehicle.isWreck || vehicle.health <= 0 || vehicle.isRemoved || player.vehicle !== vehicle || !player.isAlive || player.isSpectator) return null
        val seat = vehicle.getSeatIndex(player)
        if (seat < 0 || vehicle.hasWeapon(seat)) return null
        return activeMounts().firstOrNull { it.slot.controlSeat == seat && it.definition.action.type == Type.GUN && GunItem.isHeldWeapon(it.installed.stack) }
    }

    open fun use(player: Player, slotId: String? = null) {
        val level = vehicle.level() as? ServerLevel ?: return
        if (releasing || vehicle.isWreck || vehicle.health <= 0 || vehicle.isRemoved || !player.isAlive || player.isSpectator ||
            player.vehicle !== vehicle) return
        val seat = vehicle.getSeatIndex(player)
        if (seat < 0 || level.gameTime < nextUseTick) return
        val mount = activeMounts().firstOrNull { it.slot.controlSeat == seat && (slotId == null || it.slot.id == slotId) }
        if (mount == null) {
            if (!vehicle.hasContainer()) return
            for (slot in slots()) {
                if (slot.controlSeat != seat || slotId != null && slot.id != slotId) continue
                val action = builtInAction(slot) ?: continue
                nextUseTick = level.gameTime + 4
                dropCargo(slot, action)
                return
            }
            return
        }
        nextUseTick = level.gameTime + 4
        when (mount.definition.action.type) {
            Type.DETONATE -> detonate(mount, player)
            Type.DROP_ITEM -> dropCargo(mount.slot, mount.definition.action)
            Type.GUN -> Unit
            else -> releaseOne(mount, player)
        }
    }

    open fun dropCargo(slot: VehicleMountSlot, action: VehicleMountDefinition.Action): Boolean {
        val level = vehicle.level() as? ServerLevel ?: return false
        if (releasing) return false
        val point = worldPosition(slot, action.offset)
        val motion = vehicle.deltaMovement.scale(action.inheritMotion)
        var dropped = false
        releasing = true
        try {
            for (index in 0 until minOf(vehicle.getContainerSize(), vehicle.inventory.slots)) {
                val stack = vehicle.inventory.getStackInSlot(index)
                if (stack.isEmpty) continue
                val entity = ItemEntity(level, point.x, point.y, point.z, stack.copy())
                entity.setDefaultPickUpDelay()
                entity.deltaMovement = motion
                vehicle.inventory.setStackInSlot(index, ItemStack.EMPTY)
                var spawned = false
                try {
                    spawned = level.addFreshEntity(entity)
                    dropped = dropped || spawned
                } finally {
                    if (!spawned) {
                        if (vehicle.isRemoved) vehicle.spawnAtLocation(stack, 0.5f)
                        else vehicle.inventory.setStackInSlot(index, stack)
                    }
                }
            }
        } finally {
            releasing = false
        }
        return dropped
    }

    open fun consume(mount: Active) {
        val installed = mount.installed
        setInstalled(mount.slot.id, Installed(installed.definitionId, installed.stack.copyWithCount(installed.stack.count - 1)))
    }

    open fun releaseOne(mount: Active, operator: Entity?): Boolean {
        releasing = true
        var spawned = false
        try {
            consume(mount)
            spawned = release(mount, operator)
            return spawned
        } finally {
            if (!spawned) {
                if (vehicle.isRemoved) vehicle.spawnAtLocation(mount.installed.stack.copyWithCount(1), 0.2f)
                else setInstalled(mount.slot.id, mount.installed)
            }
            releasing = false
        }
    }

    open fun release(mount: Active, operator: Entity?): Boolean {
        val level = vehicle.level() as? ServerLevel ?: return false
        val action = mount.definition.action
        val point = worldPosition(mount.slot, action.offset)
        val type = action.entity?.let { ForgeRegistries.ENTITY_TYPES.getValue(it) } ?: return false
        val entity = type.create(level) ?: return false
        if (action.type == Type.LAUNCH_ENTITY && entity !is Projectile) return false

        if (action.entity != null) {
            val projectile = ProjectileInfo(action.data).apply { itemId = action.entity.toString() }
            val shootData = ShootData(operator?.uuid, (action.damage ?: 0f).toDouble(),
                (action.explosionDamage ?: 0f).toDouble(), (action.explosionRadius ?: 0f).toDouble(), 0.0)
            LaunchableEntityTool.getModifiedTag(projectile, shootData)?.let {
                val tag = entity.saveWithoutId(CompoundTag()).merge(it)
                entity.load(tag)
            }
        }
        if (entity is Projectile && operator != null) entity.owner = operator
        if (entity is IBulletProperties) {
            action.damage?.let(entity::setDamage)
            action.explosionDamage?.let(entity::setExplosionDamage)
            action.explosionRadius?.let(entity::setExplosionRadius)
        }
        entity.setPos(point)
        if (action.type == Type.LAUNCH_ENTITY && entity is Projectile) {
            val direction = vehicle.getVectorFromString(mount.slot.transform, 1f)
            entity.shoot(direction.x, direction.y, direction.z, action.speed.coerceIn(0.01f, 20f), 0f)
        } else {
            entity.deltaMovement = Vec3.ZERO
        }
        entity.deltaMovement = entity.deltaMovement.add(vehicle.deltaMovement.scale(action.inheritMotion))
        return level.addFreshEntity(entity)
    }

    open fun onImpact(origin: Vec3, motion: Vec3) {
        val level = vehicle.level() as? ServerLevel ?: return
        val operator = vehicle.firstPassenger as? Player ?: return
        if (releasing || vehicle.isWreck || vehicle.health <= 0 || vehicle.isRemoved || !operator.isAlive) return
        val mount = activeMounts().firstOrNull {
            val action = it.definition.action
            if (it.slot.controlSeat != vehicle.getSeatIndex(operator) || action.type != Type.DETONATE) return@firstOrNull false
            val impactSpeed = action.impactSpeed?.takeIf { speed -> speed > 0 } ?: return@firstOrNull false
            // 目标载具的 OBB 推挤会在本 tick 截断运动量，因此同时参考上一 tick 的速度
            motion.lengthSqr() >= impactSpeed * impactSpeed || vehicle.lastTickSpeed >= impactSpeed
        } ?: return
        val speed = mount.definition.action.impactSpeed ?: return
        val hit = findImpactTarget(origin, motion)
        if (hit != null || vehicle.horizontalCollision && motion.horizontalDistance() >= speed ||
            vehicle.verticalCollision && abs(motion.y) >= speed
        ) {
            detonate(mount, operator, hit)
        }
    }

    /**
     * 撞击命中目标：优先本 tick 的扫掠射线；射线漏判时（目标为 OBB 载具、被推挤截速等）
     * 退回按接触距离找最近目标，保证 kamikaze 直伤不会因为射线判定失败而丢失。
     */
    private fun findImpactTarget(origin: Vec3, motion: Vec3): Entity? {
        val level = vehicle.level() as? ServerLevel ?: return null
        val stop = vehicle.position()
        val travel = stop.subtract(origin)
        if (travel.lengthSqr() > 1.0E-4) {
            val start = origin.add(0.0, vehicle.bbHeight * 0.5, 0.0)
            val rayHit = ProjectileUtil.getEntityHitResult(
                level, vehicle, start, start.add(travel),
                vehicle.boundingBox.move(travel.reverse()).expandTowards(travel).inflate(0.6)
            ) { isValidImpactTarget(it) }?.entity
            if (rayHit != null) return rayHit
        }
        return nearbyImpactTarget(maxOf(0.4, motion.length() * 0.5 + 0.4), motion)
    }

    /** 接触范围内最近的可命中目标（排除自身乘客/替身/不可选中实体） */
    private fun nearbyImpactTarget(reach: Double, motion: Vec3? = null): Entity? {
        val level = vehicle.level() as? ServerLevel ?: return null
        val moveDir = motion?.takeIf { it.lengthSqr() > 1.0E-8 }?.normalize()
        val origin = vehicle.position()
        return level.getEntities(vehicle, vehicle.boundingBox.inflate(reach)) { isValidImpactTarget(it) }
            .filter { target ->
                if (moveDir == null) return@filter true
                val to = target.boundingBox.center.subtract(origin)
                to.lengthSqr() < 1.0E-8 || to.normalize().dot(moveDir) > -0.2
            }
            .minByOrNull { it.distanceToSqr(vehicle) }
    }

    private fun isValidImpactTarget(entity: Entity): Boolean =
        (entity is LivingEntity || entity is VehicleEntity) &&
            entity !is RCOperatorBodyEntity && entity.isPickable && !entity.isSpectator && entity.isAlive &&
            !vehicle.hasPassenger(entity) && !entity.isPassengerOfSameVehicle(vehicle)

    /** 最近一次伤害来源里的载具（例如撞击时把无人机碾碎的目标载具），用于殉爆直伤兜底 */
    private fun lastAttackerImpactTarget(): Entity? {
        val source = vehicle.lastDamageSource ?: return null
        return listOfNotNull(source.directEntity, source.entity)
            .firstOrNull { it is VehicleEntity && it !== vehicle && isValidImpactTarget(it) && it.distanceToSqr(vehicle) <= 64.0 }
    }

    open fun detonate(mount: Active, operator: Player, hit: Entity? = null) {
        val action = mount.definition.action
        val point = worldPosition(mount.slot, action.offset)
        consume(mount)
        // 必须在 destroy() 之前结算直伤：destroy() 会结束 RC 会话并把操控者传送回假人处，
        // 伤害来源的 attacker 位置一旦变成假人，目标的角度减伤就会永远按正面计算。
        applyDirectHit(action, hit, operator)
        vehicle.health = 0f
        vehicle.isWreck = true
        vehicle.destroy()
        explode(action, point, operator)
    }

    /** 结算挂载物的撞击直伤（伤害类型按挂载定义配置，缺省回退自定义爆炸伤害） */
    private fun applyDirectHit(action: VehicleMountDefinition.Action, hit: Entity?, operator: Entity?) {
        val damage = action.damage ?: 0f
        if (hit == null || damage <= 0f || hit.isRemoved) return
        val source = action.damageType?.let {
            ModDamageTypes.causeDamageType(vehicle.level().registryAccess(), it.toString(), vehicle, operator)
        } ?: ModDamageTypes.causeCustomExplosionDamage(vehicle.level().registryAccess(), vehicle, operator)
        DamageHandler.doDamage(hit, source, damage)
    }

    open fun beforeDestroy(): () -> Unit {
        if (vehicle.level() !is ServerLevel || vehicle.isRemoved) return {}
        if (releasing) {
            dropAll()
            return {}
        }
        val operator = vehicle.firstPassenger as? Player
        val attacker = vehicle.lastAttacker ?: operator
        val detonations = mutableListOf<Pair<VehicleMountDefinition.Action, Vec3>>()
        for (mount in activeMounts()) {
            val action = mount.definition.action
            if (!action.onDestroyed) continue
            if (action.type == Type.GUN) continue
            if (action.type == Type.DROP_ITEM) {
                dropCargo(mount.slot, action)
                continue
            }
            if (action.type == Type.DETONATE) {
                if (operator != null) {
                    val point = worldPosition(mount.slot, action.offset)
                    // 无人机被撞毁/碾碎时往往来不及走 onImpact，殉爆时也要把直伤结算给贴身目标。
                    // 这里在 beforeDestroy 内立即结算：destroy() 之后操控者会被传送回假人处，角度减伤会失真。
                    val hit = lastAttackerImpactTarget() ?: nearbyImpactTarget(maxOf(0.6, vehicle.deltaMovement.length() * 0.5 + 0.4), vehicle.deltaMovement)
                    applyDirectHit(action, hit, operator)
                    detonations += action to point
                    consume(mount)
                }
            } else {
                while (!vehicle.isRemoved) {
                    val installed = getInstalled(mount.slot.id) ?: break
                    if (!releaseOne(Active(mount.slot, installed, mount.definition), operator)) break
                }
            }
        }
        for (slot in slots()) {
            val action = builtInAction(slot) ?: continue
            if (action.onDestroyed) dropCargo(slot, action)
        }
        dropAll()
        return {
            detonations.forEach { (action, point) ->
                explode(action, point, attacker)
            }
        }
    }

    open fun explode(action: VehicleMountDefinition.Action, point: Vec3, attacker: Entity?) {
        val radius = action.explosionRadius ?: return
        if (radius <= 0f) return
        CustomExplosion.Builder(vehicle).position(point).attacker(attacker)
            .radius(radius).damage(action.explosionDamage ?: 0f).explode()
    }

    open fun dropAll() {
        if (vehicle.level() !is ServerLevel) return
        stopGuns()
        mountedGuns.clear()
        val state = vehicle.entityData.get(VehicleEntity.MOUNT_STATE)
        if (state.isEmpty) return
        val stacks = state.allKeys.mapNotNull { getInstalled(it)?.stack }
        vehicle.entityData.set(VehicleEntity.MOUNT_STATE, CompoundTag())
        stacks.forEach { vehicle.spawnAtLocation(it, 0.2f) }
    }
}
