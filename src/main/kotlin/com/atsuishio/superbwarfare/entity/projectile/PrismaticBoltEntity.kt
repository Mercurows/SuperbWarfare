package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

open class PrismaticBoltEntity : Entity {
    var randomAngle: Float = (((Math.random() * 2) - 1) * 45).toFloat()
    var tickO: Int = 0
    var tick: Int = 0

    constructor(type: EntityType<out PrismaticBoltEntity>, world: Level) : super(type, world)

    constructor(level: Level) : super(ModEntities.PRISMATIC_BOLT.get(), level)

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
    }

    override fun tick() {
        tickO = tick
        super.tick()
        tick++
        if (this.tick > 4) {
            this.discard()
        }
    }

    fun getLerpTick(tickDelta: Float): Float {
        return Mth.lerp(tickDelta, tickO.toFloat(), tick.toFloat())
    }

    override fun move(pType: MoverType, pPos: Vec3) {
    }
}
