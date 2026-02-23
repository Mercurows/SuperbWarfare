package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.SerializedUUID
import com.atsuishio.superbwarfare.network.SerializedVec3
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.DamageHandler
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.ParticleTool
import kotlinx.serialization.Serializable
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity

@Serializable
data class LungeMineAttackMessage(
    val type: Int,
    val uuid: SerializedUUID,
    val pos: SerializedVec3,
) : ServerPacketPayload() {

    override fun PayloadContext.handler() {
        val player = sender()
        val stack = player.mainHandItem

        if (stack.`is`(ModItems.LUNGE_MINE.get())) {
            if (type == 0) {
                if (!player.isCreative) {
                    stack.shrink(1)
                }
                val lookingEntity = EntityFindUtil.findEntity(player.level(), uuid.toString())
                if (lookingEntity != null) {
                    DamageHandler.doDamage(
                        lookingEntity,
                        ModDamageTypes.causeLungeMineDamage(player.level().registryAccess(), player, player),
                        (if (lookingEntity is VehicleEntity) 600 else 150).toFloat()
                    )
                    causeLungeMineExplode(player, lookingEntity)
                }
            } else if (type == 1) {
                if (!player.isCreative) {
                    stack.shrink(1)
                }

                CustomExplosion.Builder(player)
                    .damage(60f)
                    .radius(4f)
                    .damageMultiplier(1.25f)
                    .withParticleType(ParticleTool.ParticleType.MEDIUM)
                    .position(pos)
                    .explode()
            }
            player.swing(InteractionHand.MAIN_HAND)
        }
    }

    fun causeLungeMineExplode(attacker: Entity, target: Entity) {
        CustomExplosion.Builder(target)
            .damage(60f)
            .radius(4f)
            .attacker(attacker)
            .damageMultiplier(1.25f)
            .withParticleType(ParticleTool.ParticleType.MEDIUM)
            .explode()
    }
}
