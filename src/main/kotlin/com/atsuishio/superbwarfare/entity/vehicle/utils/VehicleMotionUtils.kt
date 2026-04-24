package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo
import com.atsuishio.superbwarfare.entity.living.TargetEntity
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.entity.vehicle.TurretWreckEntity
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.lerpAngle
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.transformPosition
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.tools.OBB
import com.atsuishio.superbwarfare.tools.SpritePixelHelper
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.forceHurt
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.entity.vehicle.Minecart
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.entity.EntityTypeTest
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Math
import org.joml.Matrix4d
import org.joml.Vector3d


/**
 * 处理载具运动相关方法的工具类
 */
object VehicleMotionUtils {

    /**
     * 防止载具堆叠
     *
     * @param vehicle 载具
     */
    fun preventStacking(vehicle: VehicleEntity) {
        val entities = vehicle.level().getEntities(
            EntityTypeTest.forClass(VehicleEntity::class.java),
            vehicle.boundingBox.inflate(6.0)
        ) { entity: VehicleEntity ->
            entity !== vehicle && !vehicle.getPassengers().contains(entity) && entity.vehicle == null
        }

        for (entity in entities) {
            if (entity.boundingBox.intersects(vehicle.boundingBox)) {
                val toVec = vehicle.position()
                    .add(Vec3(1.0, 1.0, 1.0).scale((vehicle.getRandom().nextFloat() * 0.01f + 1f).toDouble()))
                    .vectorTo(entity.position())
                val velAdd = toVec.normalize().scale(
                    Math.max(
                        (vehicle.bbWidth + 2) - vehicle.position().distanceTo(entity.position()),
                        0.0
                    ) * 0.1
                )
                val entitySize = (entity.bbWidth * entity.bbHeight).toDouble()
                val thisSize = (vehicle.bbWidth * vehicle.bbHeight).toDouble()
                val f = Math.min(entitySize / thisSize, 2.0)
                val f1 = Math.min(thisSize / entitySize, 2.0)

                vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z)
                entity.push(f1 * velAdd.x, f1 * velAdd.y, f1 * velAdd.z)
            }
        }
    }

    /**
     * 支撑自身范围内的实体
     *
     * @param vehicle 载具
     */
    fun supportEntities(vehicle: VehicleEntity) {
        if (vehicle.isRemoved) return
        if (vehicle.enableAABB() || vehicle is Type63Entity) {
            return
        }

        val frontBox = calculateCombinedAABBOptimized(vehicle).inflate(1.0)
        val entities = vehicle.level().getEntities(
            EntityTypeTest.forClass(Entity::class.java), frontBox
        ) { entity -> entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity!!.vehicle == null }
            .stream().filter { entity ->
                if (entity!!.isAlive && vehicle.isInObb(entity, vehicle.deltaMovement)) {
                    val type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
                    return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5) || (entity is LivingEntity && !(entity is Player && entity.isSpectator))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get()
                        .contains(type.toString())
                }
                false
            }
            .toList()

        entities.forEach { e ->
            if (e is Player && vehicle.level().isClientSide) {
                vehicle.support(e)
            } else if (!vehicle.level().isClientSide) {
                vehicle.support(e)
            }
        }
    }

    /**
     * 支撑某一个实体
     *
     * @param vehicle 载具
     * @author YWZJ Ranpoes
     */
    fun support(vehicle: VehicleEntity, entity: Entity) {
        if (entity is DroneEntity) return

        if (vehicle.enableAABB()) return
        if (entity.noPhysics || vehicle.noPhysics) {
            return
        }

        if (entity is TurretWreckEntity) {
            entity.supportByVehicle = true
        }

        val feetPos = entity.position().subtract(Vec3(0.0, 0.1, 0.0))
        val lowPos = feetPos.add(0.0, (entity.eyeHeight / 4).toDouble(), 0.0)
        val midPos = feetPos.add(0.0, (entity.eyeHeight / 2).toDouble(), 0.0)
        val eyePos = feetPos.add(0.0, entity.eyeHeight.toDouble(), 0.0)

        for (obb in vehicle.getOBBs()) {
            if (obb.contains(feetPos)) {
                if (!entity.noPhysics && !vehicle.noPhysics) {
                    val gravity = Math.max(entity.deltaMovement.y, 0.0)
                    if (gravity == 0.0) {
                        entity.setOnGround(true)
                    }
                    val depth = obb.getEmbeddingDepth(feetPos)
                    entity.deltaMovement =
                        vehicle.deltaMovement.add(0.0, if (gravity + depth <= 0.4f) 0.0 else depth * 1.1, 0.0)
                    entity.fallDistance = 0f

                    continue
                }
            }

            if (obb.contains(midPos) || obb.contains(lowPos) || obb.contains(eyePos)) {
                entity.isSprinting = false
                var dx = entity.x - obb.center.x
                var dz = entity.z - obb.center.z
                var dMax = Mth.absMax(dx, dz)
                if (dMax >= 0.01) {
                    dMax = Math.sqrt(dMax)
                    dx /= dMax
                    dz /= dMax
                    var d = 1 / dMax
                    if (d > 1) {
                        d = 1.0
                    }
                    dx *= d
                    dz *= d
                    dx *= 0.12
                    dz *= 0.12
                    if (entity.isPushable) {
                        entity.push(dx, 0.0, dz)
                    }
                    continue
                }
            }

            val aabb = entity.boundingBox
            if (OBB.isColliding(obb, aabb)) {
                val face = obb.getEmbeddingFace(midPos)
                val axes = obb.getAxes()
                val support: Vector3d = axes[Math.abs(face) - 1]!!
                if (face < 0) {
                    support.negate()
                }

                if (entity is Player && entity.onGround() && entity.isCrouching && entity.level() is ServerLevel) {
                    // 推车
                    vehicle.setDeltaMovement(
                        vehicle.deltaMovement.add(OBB.vector3dToVec3(support).normalize().multiply(-1.0, 0.0, -1.0))
                            .normalize()
                            .scale(entity.deltaMovement.length())
                    )
                }

                entity.isSprinting = false
                if (entity.isPushable) {
                    var force = entity.deltaMovement.horizontalDistance() * 2
                    if (vehicle.deltaMovement.length() > 0.01 && Math.abs(face) != 2) {
                        force = 0.2
                    }
                    var vec = OBB.vector3dToVec3(support).scale(force)
                    vec = Vec3(vec.x, 0.0, vec.z)
                    entity.setPos(entity.position().add(vec))
                    vehicle.hasImpulse = true
                }
            }
        }
    }

    /**
     * 撞击实体并造成伤害
     *
     * @param vehicle 载具
     */
    fun crushEntities(vehicle: VehicleEntity) {
        if (!vehicle.canCrushEntities()) return
        if (vehicle.isRemoved) return

        val vec3 = vehicle.deltaMovement

        val entities: MutableList<Entity>?
        if (!vehicle.enableAABB()) {
            val frontBox = calculateCombinedAABBOptimized(vehicle)
            entities = vehicle.level().getEntities(
                EntityTypeTest.forClass(Entity::class.java), frontBox
            ) { entity -> entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity!!.vehicle == null }
                .stream().filter { entity ->
                    if (entity.isAlive && vehicle.isInObb(entity, vec3)) {
                        val type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
                        return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5) || (entity is LivingEntity && !(entity is Player && entity.isSpectator))) || VehicleConfig.COLLISION_ENTITY_WHITELIST.get()
                            .contains(type.toString())
                    }
                    false
                }
                .toList()
        } else {
            val frontBox = vehicle.boundingBox.move(vec3)
            entities = vehicle.level().getEntities(
                EntityTypeTest.forClass(Entity::class.java), frontBox
            ) { entity -> entity !== vehicle && entity !== vehicle.getFirstPassenger() && entity!!.vehicle == null }
                .stream().filter { entity ->
                    if (entity.isAlive) {
                        val type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
                        return@filter (entity is VehicleEntity || entity is Boat || entity is Minecart || (entity is TurretWreckEntity && entity.tickCount > 5)
                                || (entity is LivingEntity && !(entity is Player && entity.isSpectator)))
                                || VehicleConfig.COLLISION_ENTITY_WHITELIST.get().contains(type.toString())
                    }
                    false
                }
                .toList()
        }

        // TODO 继续优化这个逆天碰撞
        for (entity in entities) {
            val entitySize = entity.boundingBox.size
            val thisSize = vehicle.boundingBox.size
            val f: Double
            val f1: Double

            val v0 = vec3.subtract(entity.deltaMovement)
            if (v0.angleTo(vehicle.position().vectorTo(entity.position())) > 90) return

            if (vehicle.deltaMovement.lengthSqr() < 0.09) return

            // TODO 给非载具实体也设置质量
            if (entity is LivingEntity && entity.hasEffect(ModMobEffects.STRIKE_PROTECTION)) {
                continue
            }

            if (entity is VehicleEntity) {
                f = Mth.clamp((entity.mass / vehicle.mass).toDouble(), 0.25, 4.0)
                f1 = Mth.clamp((vehicle.mass / vehicle.mass).toDouble(), 0.25, 4.0)
            } else {
                f = Mth.clamp(entitySize / thisSize, 0.25, 4.0)
                f1 = Mth.clamp(thisSize / entitySize, 0.25, 4.0)
            }

            val length = v0.length().toFloat()
            var velAdd = v0.normalize().scale(0.8 * length)

            if (length <= 0.3) {
                continue
            }

            vehicle.level().playSound(null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.soundSource, 1f, 1f)

            if (entity is LivingEntity) {
                entity.forceHurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        vehicle,
                        if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                    ),
                    (f1 * 80 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )
            } else {
                entity.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        vehicle,
                        if (vehicle.getFirstPassenger() == null) vehicle else vehicle.getFirstPassenger()
                    ), (f1 * 60 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )
            }

            if (entity !is TargetEntity) {
                vehicle.pushNew(-0.3f * f * velAdd.x, -0.3f * f * velAdd.y, -0.3f * f * velAdd.z)
            }

            if (entity is VehicleEntity) {
                vehicle.hurt(
                    ModDamageTypes.causeVehicleStrikeDamage(
                        vehicle.level().registryAccess(),
                        entity,
                        if (entity.getFirstPassenger() == null) entity else entity.getFirstPassenger()
                    ), (f * 40 * (Mth.abs(length) - 0.3) * (Mth.abs(length) - 0.3)).toFloat()
                )

                if (!vehicle.enableAABB()) {
                    if (vehicle.isInObb(entity, Vec3.ZERO)) {
                        var thisPos = vehicle.position()
                        var otherPos = entity.position()

                        for (obb in vehicle.getOBBs()) {
                            if (!entity.enableAABB()) {
                                val obbList2 = entity.getOBBs()
                                for (obb2 in obbList2) {
                                    if (OBB.isColliding(obb, obb2)) {
                                        thisPos = OBB.vector3dToVec3(obb.center)
                                        otherPos = OBB.vector3dToVec3(obb2.center)
                                    }
                                }
                            } else {
                                if (OBB.isColliding(obb, entity.boundingBox)) {
                                    thisPos = OBB.vector3dToVec3(obb.center)
                                }
                            }
                        }

                        val toVec = thisPos.add(
                            Vec3(1.0, 1.0, 1.0).scale(
                                (vehicle.getRandom().nextFloat() * 0.01f + 1f).toDouble()
                            )
                        ).vectorTo(otherPos)
                        velAdd = toVec.normalize().scale(Math.max(thisPos.distanceTo(otherPos), 0.0) * 0.01)
                        vehicle.pushNew(-f * velAdd.x, -f * velAdd.y, -f * velAdd.z)
                    }
                }

                val vec31 = vehicle.deltaMovement.normalize().scale(velAdd.length())
                entity.pushNew(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            } else {
                val vec31 = vehicle.deltaMovement.normalize().scale(velAdd.length())
                entity.push(f1 * vec31.x, f1 * vec31.y, f1 * vec31.z)
            }
        }
    }

    // TODO 实现正确的AABB包围箱
    fun calculateCombinedAABBOptimized(vehicle: VehicleEntity): AABB {
        if (vehicle.enableAABB()) {
            return vehicle.boundingBox
        }

        val obbList = vehicle.getOBBs()

        val min = Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
        val max = Vector3d(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)

        for (obb in obbList) {
            val vertices = obb.getVertices()

            for (vertex in vertices) {
                min.x = Math.min(min.x, vertex.x)
                min.y = Math.min(min.y, vertex.y)
                min.z = Math.min(min.z, vertex.z)

                max.x = Math.max(max.x, vertex.x)
                max.y = Math.max(max.y, vertex.y)
                max.z = Math.max(max.z, vertex.z)
            }
        }

        return AABB(OBB.vector3dToVec3(min), OBB.vector3dToVec3(max))
    }

    /**
     * 根据条件来碰撞方块
     *
     * @param vehicle 载具
     */
    fun collideBlocks(vehicle: VehicleEntity) {
        if (!VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get()
            && !VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get()
            && !VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get()
            && !VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get()
        ) return

        val collisionLevel = vehicle.computed().collisionLevel
        val limits = collisionLevel.powerLimits

        val power = vehicle.power
        val motion = vehicle.deltaMovement.horizontalDistance()

        val flags = booleanArrayOf(
            VehicleConfig.COLLISION_DESTROY_SOFT_BLOCKS.get() && collisionLevel.level >= 1,
            VehicleConfig.COLLISION_DESTROY_NORMAL_BLOCKS.get() && collisionLevel.level >= 2,
            VehicleConfig.COLLISION_DESTROY_HARD_BLOCKS.get() && collisionLevel.level >= 3,
            VehicleConfig.COLLISION_DESTROY_BLOCKS_BEASTLY.get() && collisionLevel.level >= 4
        )

        var i = 0
        while (i < flags.size && i < limits.size) {
            val limit = limits[i]
            flags[i] =
                flags[i] and if (limit.equals) power >= limit.power || motion >= limit.motion else power > limit.power || motion > limit.motion
            i++
        }

        if (!vehicle.enableAABB()) {
            val aabb = calculateCombinedAABBOptimized(vehicle)
            BlockPos.betweenClosedStream(aabb).forEach { pos ->
                val state = vehicle.level().getBlockState(pos)
                if (vehicle.isInObb(pos, vehicle.deltaMovement)) {
                    if ((flags[0] && state.`is`(ModTags.Blocks.SOFT_COLLISION)) ||
                        (flags[1] && state.`is`(ModTags.Blocks.NORMAL_COLLISION)) ||
                        (flags[2] && state.`is`(ModTags.Blocks.HARD_COLLISION)) ||
                        (flags[3] && (state.block.defaultDestroyTime() > 0 || state.block
                            .defaultDestroyTime() <= 4))
                    ) {
                        vehicle.level().destroyBlock(pos, true)
                    }
                }
            }
        }

        val aabb = vehicle.boundingBox.inflate(0.25, 0.0, 0.25).move(vehicle.deltaMovement).move(0.0, 0.5, 0.0)
        BlockPos.betweenClosedStream(aabb).forEach { pos ->
            val state = vehicle.level().getBlockState(pos)
            if ((flags[0] && state.`is`(ModTags.Blocks.SOFT_COLLISION)) ||
                (flags[1] && state.`is`(ModTags.Blocks.NORMAL_COLLISION)) ||
                (flags[2] && state.`is`(ModTags.Blocks.HARD_COLLISION)) ||
                (flags[3] && (state.block.defaultDestroyTime() > 0 || state.block
                    .defaultDestroyTime() <= 4))
            ) {
                vehicle.level().destroyBlock(pos, true)
            }
        }
    }

    /**
     * 载具在龙牙上行驶时，减速
     *
     * @param vehicle 载具
     */
    fun handleVehicleMoveOnDragonTeeth(vehicle: VehicleEntity) {
        val aabb = vehicle.boundingBox
        val aabb1 = AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ)
        val pos = vehicle.level().findSupportingBlock(vehicle, aabb1).orElse(null) ?: return

        val state = vehicle.level().getBlockState(pos)
        if (state.`is`(ModBlocks.DRAGON_TEETH.get())) {
            vehicle.power *= 0.8f
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(-0.1, 0.0, -0.1))
        }
    }

    fun bounceHorizontal(vehicle: VehicleEntity, direction: Direction) {
        when (direction.axis) {
            Direction.Axis.X -> vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.8, 0.99, 0.99))
            Direction.Axis.Z -> vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.99, 0.99, 0.8))
            else -> {}
        }
    }

    fun bounceVertical(vehicle: VehicleEntity, direction: Direction) {
        if (!vehicle.level().isClientSide) {
            vehicle.level().playSound(null, vehicle, ModSounds.VEHICLE_STRIKE.get(), vehicle.soundSource, 1f, 1f)
        }
        vehicle.collisionCoolDown = 4
        vehicle.crash = true
        if (direction.axis === Direction.Axis.Y) {
            vehicle.setDeltaMovement(vehicle.deltaMovement.multiply(0.9, -0.8, 0.9))
        }
    }

    fun terrainCompact(vehicle: VehicleEntity, positions: MutableList<Vec3>) {
        if (vehicle.onGround()) {
            val transform = vehicle.getWheelsTransform(1f)
            for (vec3 in positions) {
                val vector4d = transformPosition(transform, vec3.x, vec3.y - 0.02, vec3.z)
                val p = Vec3(vector4d.x, vector4d.y, vector4d.z)
                val level = vehicle.level()
                val res = level.clip(
                    ClipContext(
                        p, p.add(0.0, -128.0, 0.0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle
                    )
                )

                val heightY: Double

                var blockPos = BlockPos.containing(p)
                val blockPosUp = BlockPos.containing(p.add(0.0, 1.0, 0.0))
                if (level.getBlockState(blockPosUp).canOcclude()) {
                    blockPos = blockPosUp
                }
                val state = level.getBlockState(blockPos)
                val shape = state.getCollisionShape(level, blockPos)

                if (vehicle.level().isClientSide && vehicle.deltaMovement.horizontalDistanceSqr() > 0.01) {
                    if (state.`is`(BlockTags.SAND) || state.`is`(BlockTags.SNOW)) {
                        val model = Minecraft.getInstance().modelManager.blockModelShaper.getBlockModel(state)
                        val sprite = model.particleIcon
                        val color = SpritePixelHelper.getRandomPixelRGB(sprite, 0)
                        val speed = Math.min(vehicle.deltaMovement.length(), 0.5).toFloat()

                        val particleOption = CustomCloudOption(
                            color,
                            70,
                            1f + 7f * speed + Math.random().toFloat() * 2,
                            Math.random().toFloat() * -0.12f,
                            cooldown = false,
                            light = false
                        )
                        vehicle.addRandomParticle(
                            particleOption,
                            p.add(0.0, 0.2, 0.0).subtract(vehicle.deltaMovement.scale(1.5)),
                            speed,
                            vehicle.level(),
                            1,
                            vehicle.deltaMovement.scale(60.0)
                        )
                    } else {
                        val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
                        vehicle.addRandomParticle(particleData, p.add(0.0, 0.1, 0.0), 0.2f, vehicle.level(), 0f, 1)

                        if (vehicle.engineInfo is EngineInfo.Track && vehicle.drift() && vehicle.deltaMovement.horizontalDistanceSqr() > 0.0004 && state.`is`(
                                BlockTags.MINEABLE_WITH_PICKAXE
                            )
                        ) {
                            vehicle.addRandomParticle(
                                ModParticleTypes.FIRE_STAR.get(),
                                p.add(0.0, 0.1, 0.0),
                                0.25f,
                                vehicle.level(),
                                0.08f,
                                1
                            )
                        }
                    }
                }

                heightY = if (!shape.isEmpty) {
                    p.y - (shape.max(Direction.Axis.Y) + blockPos.y)
                } else if (res.type == HitResult.Type.BLOCK && level.noCollision(AABB(p, p))) {
                    Mth.clamp(p.y - res.location.y, 0.0, 20.0)
                } else {
                    0.0
                }

                updateTerrainCompact(vehicle, p, heightY)
            }
        } else if (vehicle.isInFluidType) {
            vehicle.xRot *= 0.9f
            vehicle.setZRot(vehicle.roll * 0.9f)
        }
    }

    fun updateTerrainCompact(entity: VehicleEntity, landingTarget: Vec3, heightY: Double) {
        var currentPos = entity.position()
        val aabb = entity.boundingBox
        val aabb1 = AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ)
        val optional = entity.level().findSupportingBlock(entity, aabb1)
        if (optional.isPresent) {
            currentPos = currentPos.add(currentPos.vectorTo(optional.get().center).scale(0.6))
        }
        val horizontalOffset = Vec3(
            landingTarget.x - currentPos.x,
            0.0,
            landingTarget.z - currentPos.z
        )

        val horizontalDistance = horizontalOffset.length()
        val horizontalDirection = if (horizontalDistance > 0) horizontalOffset.normalize() else Vec3.ZERO


        val tiltSmoothingFactor = 0.01f

        val targetTilt =
            Math.min(heightY * 9 * entity.data().compute().terrainCompatRotateRate * horizontalDistance, 45.0).toFloat()

        val yawRad = Math.toRadians(-entity.yRot)
        val localDirection = Vec3(
            horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
            0.0,
            horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        )

        val targetXRot = (-localDirection.z * targetTilt).toFloat()
        val targetZRot = (localDirection.x * targetTilt).toFloat()

        entity.xRot = lerpAngle(entity.xRot, -targetXRot, tiltSmoothingFactor)
        entity.setZRot(lerpAngle(entity.roll, -targetZRot, tiltSmoothingFactor))
    }

    fun getWheelsTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transform = Matrix4d()
        transform.translate(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x).toFloat().toDouble(),
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y).toFloat().toDouble(),
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z).toFloat().toDouble()
        )
        transform.rotate(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)))
        return transform
    }
}
