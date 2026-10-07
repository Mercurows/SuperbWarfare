package com.atsuishio.superbwarfare.item.gun.special

import com.atsuishio.superbwarfare.client.PoseTool
import com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer
import com.atsuishio.superbwarfare.client.renderer.gun.RepairToolRenderer
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.world.phys.EntityResult
import net.minecraft.client.model.HumanoidModel
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@RegistryName("repair_tool")
class RepairToolItem : GeoGunItemV2(Properties()) {

    /**
     * 修理工具是唯一需要特殊渲染的 V2 枪：枪口那簇火焰要逐帧切换，见 [RepairToolRenderer]。
     */
    @OnlyIn(Dist.CLIENT)
    override fun createRenderer(): GeoGunRenderer = RepairToolRenderer()

    /**
     * 端工具的手势和别的枪（[PoseTool.pose]）不一样，所以不能走 V2 的默认实现。
     */
    @OnlyIn(Dist.CLIENT)
    override fun armPose(
        entityLiving: LivingEntity,
        hand: InteractionHand,
        itemStack: ItemStack
    ): HumanoidModel.ArmPose {
        return if (!itemStack.isEmpty && entityLiving.usedItemHand == hand) ModEnumExtensions.Client.repairToolPose
        else HumanoidModel.ArmPose.EMPTY
    }

    override fun onRayHitBlock(
        shooter: Entity?,
        level: ServerLevel,
        target: Entity?,
        data: GunData,
        shootDirection: Vec3?,
        result: BlockHitResult,
        pos: Vec3
    ) {
        super.onRayHitBlock(shooter, level, target, data, shootDirection, result, pos)
        val state = level.getBlockState(result.blockPos)
        summonRayHitParticle(level, state, pos, sparkDirection(shootDirection))
    }

    override fun getRayHitBlockSound(data: GunData): SoundEvent = ModSounds.REPAIRING.get()

    override fun getRayHitEntitySound(data: GunData): SoundEvent = ModSounds.REPAIRING.get()

    override fun onRayHitEntity(
        shooter: Entity?,
        level: ServerLevel,
        data: GunData,
        result: EntityResult,
        shootPosition: Vec3?,
        shootDirection: Vec3?
    ) {
        val target = result.entity
        val pos = result.hitVec
        val dir = sparkDirection(shootDirection)
        // 父类签名里 shooter 是可空的；为空时这里是"没人拿着工具"，按没下蹲算，不去 NPE
        val shiftDown = shooter?.isShiftKeyDown == true

        level.playSound(
            null,
            pos.x,
            pos.y,
            pos.z,
            getRayHitEntitySound(data),
            SoundSource.PLAYERS,
            0.7f,
            ((2 * Math.random() - 1) * 0.05f + 1.0f).toFloat()
        )

        // 修理实体（多重含义）
        if (target is VehicleEntity) {
            val lastDriver = EntityFindUtil.findEntity(level, target.lastDriverUUID)
            if ((lastDriver != null && !SeekTool.IN_SAME_TEAM.test(
                    shooter,
                    lastDriver
                ) && lastDriver.team != null) || shiftDown
            ) {
                target.hurt(ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter), 0.5f)
                if (shooter is ServerPlayer) {
                    shooter.level().playSound(
                        null,
                        shooter.blockPosition(),
                        ModSounds.INDICATION.get(),
                        SoundSource.VOICE,
                        0.1f,
                        1f
                    )
                    shooter.sendPacket(ClientIndicatorMessage(0, 5))
                }
            } else if (!target.isWreck) {
                target.heal(0.5f + 0.0025f * target.getMaxHealth())
            } else {
                target.hurt(
                    ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter),
                    0.5f + 0.0025f * target.getMaxHealth()
                )
            }

            summonRayHitParticle(level, null, pos, dir)
        } else if (target is LivingEntity) {
            if (target.type.`is`(ModTags.EntityTypes.CAN_REPAIR) && !shiftDown) {
                target.heal(0.5f + 0.0025f * target.getMaxHealth())
            } else {
                val iCustomKnockback = ICustomKnockback.getInstance(target)
                iCustomKnockback.`superbWarfare$setKnockbackStrength`(0.0)

                val damage = data.get(GunProp.DAMAGE).toFloat()
                DamageHandler.doDamage(
                    target,
                    ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter),
                    damage
                )
                target.invulnerableTime = 0

                iCustomKnockback.`superbWarfare$resetKnockbackStrength`()

                if (shooter is ServerPlayer) {
                    shooter.level().playSound(
                        null,
                        shooter.blockPosition(),
                        ModSounds.INDICATION.get(),
                        SoundSource.VOICE,
                        0.1f,
                        1f
                    )
                    shooter.sendPacket(ClientIndicatorMessage(0, 5))
                }
            }
            summonRayHitParticle(level, null, pos, dir)
        } else {
            val damage = data.get(GunProp.DAMAGE).toFloat()
            DamageHandler.doDamage(
                target,
                ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter),
                damage
            )
            target.invulnerableTime = 0

            if (shooter is ServerPlayer) {
                shooter.level().playSound(
                    null,
                    shooter.blockPosition(),
                    ModSounds.INDICATION.get(),
                    SoundSource.VOICE,
                    0.1f,
                    1f
                )
                shooter.sendPacket(ClientIndicatorMessage(0, 5))
            }

            summonRayHitParticle(level, null, pos, dir)
        }
    }

    /**
     * 打击点的火星/烟。`state` 不为空说明打在方块上，额外撒一层方块碎屑。
     */
    fun summonRayHitParticle(level: ServerLevel, state: BlockState?, pos: Vec3, dir: Vec3) {
        if (state != null) {
            val particleData = BlockParticleOption(ParticleTypes.BLOCK, state)
            for (i in 0 until 1) {
                val vec3 = randomVec(dir, 40.0)
                ParticleTool.sendParticle(
                    level,
                    particleData,
                    pos.x + 0.05 * i * dir.x,
                    pos.y + 0.05 * i * dir.y,
                    pos.z + 0.05 * i * dir.z,
                    0,
                    vec3.x,
                    vec3.y,
                    vec3.z,
                    10.0,
                    true
                )
            }
        }

        for (i in 0 until 3) {
            val vec3 = randomVec(dir, 20.0)
            ParticleTool.sendParticle(
                level,
                ParticleTypes.SMOKE,
                pos.x,
                pos.y,
                pos.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                0.05,
                true
            )
        }
        for (i in 0 until 2) {
            val vec3 = randomVec(dir, 80.0)
            ParticleTool.sendParticle(
                level,
                ModParticleTypes.FIRE_STAR.get(),
                pos.x,
                pos.y,
                pos.z,
                0,
                vec3.x,
                vec3.y,
                vec3.z,
                0.2 + 0.1 * Math.random(),
                true
            )
        }
    }

    /**
     * 火星的飞溅方向 = 射线方向取反。
     *
     * 父类签名里 `shootDirection` 是可空的（`GunItem.onRayHitEntity` 一脉相承），
     * 真为空时退化成零向量 —— [Vec3.normalize] 对零向量返回零向量，粒子就原地散开，不会抛异常。
     */
    private fun sparkDirection(shootDirection: Vec3?): Vec3 =
        (shootDirection ?: Vec3.ZERO).scale(-1.0).normalize()
}
