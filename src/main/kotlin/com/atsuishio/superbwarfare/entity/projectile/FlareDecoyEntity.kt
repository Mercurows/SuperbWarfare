package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.particle.CustomFlareOption
import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.network.NetworkHooks

open class FlareDecoyEntity : Entity {
    constructor(type: EntityType<out FlareDecoyEntity>, world: Level) : super(type, world)

    constructor(level: Level) : super(ModEntities.FLARE_DECOY.get(), level)

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> {
        return NetworkHooks.getEntitySpawningPacket(this)
    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun defineSynchedData() {
    }

    override fun tick() {
        super.tick()
        this.deltaMovement = this.deltaMovement.add(0.0, -0.02, 0.0)
        this.move(MoverType.SELF, this.deltaMovement)

        if (level().isClientSide()) {
            val l = deltaMovement.length()
            var i = 0.0
            while (i < l) {
                val startPos = Vec3(xo, yo + bbHeight / 2, zo)
                val pos = startPos.add(deltaMovement.normalize().scale(-i))
                val random = 2 * (this.random.nextFloat() - 0.5f)

                level().addParticle(
                    CustomFlareOption(
                        1f,
                        0.9f,
                        0.8f,
                        200,
                        0.95f,
                        (10 + 8 * random).toInt(),
                        0.03f,
                        size = 0.25f
                    ), pos.x + random * 0.2, pos.y + random * 0.2, pos.z + random * 0.2, 0.0, 0.0, 0.0
                )

                level().addParticle(
                    CustomFlareOption(
                        1f,
                        1f,
                        1f,
                        10,
                        0.5f,
                        (10 + 8 * random).toInt(),
                        0.2f,
                        size = 0.25f
                    ), pos.x + random * 0.2, pos.y + random * 0.2, pos.z + random * 0.2, 0.0, 0.0, 0.0
                )

                i += 2
            }
        }
        if (this.tickCount > 200 || this.isInWater || this.onGround()) {
            this.discard()
        }
    }

    fun decoyShoot(entity: Entity, shootVec: Vec3, pVelocity: Float, pInaccuracy: Float) {
        val vec3 = shootVec.normalize().add(
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble()),
            this.random.triangle(0.0, 0.0172275 * pInaccuracy.toDouble())
        ).scale(pVelocity.toDouble())
        this.deltaMovement = entity.deltaMovement.scale(0.75).add(vec3)
        val d0 = vec3.horizontalDistance()
        this.yRot = (Mth.atan2(vec3.x, vec3.z) * 57.2957763671875).toFloat()
        this.xRot = (Mth.atan2(vec3.y, d0) * 57.2957763671875).toFloat()
        this.yRotO = this.yRot
        this.xRotO = this.xRot
    }
}
