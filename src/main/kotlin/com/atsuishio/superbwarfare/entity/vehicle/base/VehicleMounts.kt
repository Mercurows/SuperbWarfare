package com.atsuishio.superbwarfare.entity.vehicle.base

import com.atsuishio.superbwarfare.data.CustomData
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

    open fun slots(): List<VehicleMountSlot> = vehicle.computed().mountSlots
        .filter { it.id.isNotBlank() && it.capacity > 0 }.distinctBy { it.id }

    open fun getInstalled(slotId: String): Installed? {
        val tag = vehicle.entityData.get(VehicleEntity.MOUNT_STATE).getCompound(slotId)
        val definitionId = ResourceLocation.tryParse(tag.getString("DefinitionId")) ?: return null
        val stack = ItemStack.of(tag.getCompound("Stack"))
        return if (stack.isEmpty) null else Installed(definitionId, stack)
    }

    open fun getDefinition(id: ResourceLocation): VehicleMountDefinition? = CustomData.VEHICLE_MOUNTS[id.toString()]

    open fun occupiedSlotIds(): Set<String> = vehicle.entityData.get(VehicleEntity.MOUNT_STATE).allKeys.toSet()

    open fun setInstalled(slotId: String, installed: Installed?) {
        if (vehicle.level() !is ServerLevel) return
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
        for (definitionId in slot.definitions) {
            val definition = getDefinition(definitionId) ?: continue
            if (!definition.ingredient.matches(source)) continue
            val count = minOf(capacity(slot, definition, source), source.count)
            if (count > 0) return Installed(definitionId, source.copyWithCount(count))
        }
        return null
    }

    open fun capacity(slot: VehicleMountSlot, definition: VehicleMountDefinition?, stack: ItemStack): Int {
        val actionLimit = if (definition?.action?.type == Type.DETONATE) 1 else 64
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
        val installed = getInstalled(slotId) ?: return ItemStack.EMPTY
        setInstalled(slotId, null)
        return installed.stack
    }

    protected open fun activeMounts(): List<Active> = slots().mapNotNull { slot ->
        val installed = getInstalled(slot.id) ?: return@mapNotNull null
        val definition = getDefinition(installed.definitionId) ?: return@mapNotNull null
        if (installed.definitionId !in slot.definitions || !definition.ingredient.matches(installed.stack)) return@mapNotNull null
        Active(slot, installed, definition)
    }

    open fun canControl(seat: Int): Boolean = seat >= 0 && activeMounts().any { it.slot.controlSeat == seat }

    open fun use(player: Player, slotId: String? = null) {
        val level = vehicle.level() as? ServerLevel ?: return
        if (releasing || vehicle.isWreck || vehicle.health <= 0 || vehicle.isRemoved || !player.isAlive || player.isSpectator ||
            player.vehicle !== vehicle) return
        val seat = vehicle.getSeatIndex(player)
        if (seat < 0 || level.gameTime < nextUseTick) return
        val mount = activeMounts().firstOrNull { it.slot.controlSeat == seat && (slotId == null || it.slot.id == slotId) } ?: return
        nextUseTick = level.gameTime + 4
        if (mount.definition.action.type == Type.DETONATE) {
            detonate(mount, player)
        } else {
            releaseOne(mount, player)
        }
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
        val entity = if (action.type == Type.DROP_ITEM) {
            ItemEntity(level, point.x, point.y, point.z, mount.installed.stack.copyWithCount(1)).also { it.setDefaultPickUpDelay() }
        } else {
            val type = action.entity?.let { ForgeRegistries.ENTITY_TYPES.getValue(it) } ?: return false
            type.create(level) ?: return false
        }
        if (action.type == Type.LAUNCH_ENTITY && entity !is Projectile) return false

        if (action.entity != null && action.type != Type.DROP_ITEM) {
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
            it.slot.controlSeat == vehicle.getSeatIndex(operator) && action.type == Type.DETONATE &&
                action.impactSpeed?.let { speed -> speed > 0 && motion.lengthSqr() >= speed * speed } == true
        } ?: return
        val speed = mount.definition.action.impactSpeed ?: return
        val stop = vehicle.position()
        val hit = if (origin.distanceToSqr(stop) > 1e-6) {
            val start = origin.add(0.0, vehicle.bbHeight * 0.5, 0.0)
            val travel = stop.subtract(origin)
            ProjectileUtil.getEntityHitResult(level, vehicle, start, start.add(travel),
                vehicle.boundingBox.move(travel.reverse()).expandTowards(travel).inflate(0.3)) {
                (it is LivingEntity || it is VehicleEntity) && it !is RCOperatorBodyEntity && it.isPickable &&
                    !it.isSpectator && !vehicle.hasPassenger(it) && !it.isPassengerOfSameVehicle(vehicle)
            }?.entity
        } else null
        if (hit != null || vehicle.horizontalCollision && motion.horizontalDistance() >= speed ||
            vehicle.verticalCollision && abs(motion.y) >= speed) detonate(mount, operator, hit)
    }

    open fun detonate(mount: Active, operator: Player, hit: Entity? = null) {
        val action = mount.definition.action
        val point = worldPosition(mount.slot, action.offset)
        consume(mount)
        vehicle.health = 0f
        vehicle.isWreck = true
        vehicle.destroy()
        val damage = action.damage ?: 0f
        if (hit != null && damage > 0) {
            DamageHandler.doDamage(hit, ModDamageTypes.causeCustomExplosionDamage(vehicle.level().registryAccess(), vehicle, operator), damage)
        }
        explode(action, point, operator)
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
            if (action.type == Type.DETONATE) {
                if (operator != null) {
                    detonations += action to worldPosition(mount.slot, action.offset)
                    consume(mount)
                }
            } else {
                while (!vehicle.isRemoved) {
                    val installed = getInstalled(mount.slot.id) ?: break
                    if (!releaseOne(Active(mount.slot, installed, mount.definition), operator)) break
                }
            }
        }
        dropAll()
        return { detonations.forEach { (action, point) -> explode(action, point, attacker) } }
    }

    open fun explode(action: VehicleMountDefinition.Action, point: Vec3, attacker: Entity?) {
        val radius = action.explosionRadius ?: return
        if (radius <= 0f) return
        CustomExplosion.Builder(vehicle).position(point).attacker(attacker)
            .radius(radius).damage(action.explosionDamage ?: 0f).explode()
    }

    open fun dropAll() {
        if (vehicle.level() !is ServerLevel) return
        val state = vehicle.entityData.get(VehicleEntity.MOUNT_STATE)
        if (state.isEmpty) return
        val stacks = state.allKeys.mapNotNull { getInstalled(it)?.stack }
        vehicle.entityData.set(VehicleEntity.MOUNT_STATE, CompoundTag())
        stacks.forEach { vehicle.spawnAtLocation(it, 0.2f) }
    }
}
