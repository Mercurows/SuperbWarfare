package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.forceHurt
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
                    val damage = ExplosionConfig.LUNGE_MINE_ATTACK_DAMAGE.get().toFloat()
                    lookingEntity.forceHurt(
                        ModDamageTypes.causeLungeMineDamage(player.level().registryAccess(), player, player),
                        if (lookingEntity is VehicleEntity) damage else damage / 4f
                    )
                    causeLungeMineExplode(player, lookingEntity)
                }
            } else if (type == 1) {
                if (!player.isCreative) {
                    stack.shrink(1)
                }

                CustomExplosion.Builder(player)
                    .damage(ExplosionConfig.LUNGE_MINE_EXPLOSION_DAMAGE.get().toFloat())
                    .radius(ExplosionConfig.LUNGE_MINE_EXPLOSION_RADIUS.get().toFloat())
                    .position(pos)
                    .explode()
            }
            player.swing(InteractionHand.MAIN_HAND)
        }
    }

    fun causeLungeMineExplode(attacker: Entity, target: Entity) {
        CustomExplosion.Builder(target)
            .damage(ExplosionConfig.LUNGE_MINE_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.LUNGE_MINE_EXPLOSION_RADIUS.get().toFloat())
            .attacker(attacker)
            .explode()
    }
}
