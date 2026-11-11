package com.atsuishio.superbwarfare.entity.misc

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkHooks
import java.util.*

class HighEnergyExplosivesEntity(type: EntityType<out HighEnergyExplosivesEntity>, level: Level) : PrimedTnt(type, level) {
    var igniterUUID: UUID? = null

    override fun getOwner(): LivingEntity? =
        EntityFindUtil.findEntity(level(), igniterUUID?.toString()) as? LivingEntity

    override fun explode() {
        CustomExplosion.Builder(this)
            .attacker(owner)
            .damage(ExplosionConfig.HIGH_ENERGY_EXPLOSIVES_BLOCK_EXPLOSION_DAMAGE.get().toFloat())
            .radius(ExplosionConfig.HIGH_ENERGY_EXPLOSIVES_BLOCK_EXPLOSION_RADIUS.get().toFloat())
            .explode()
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        igniterUUID?.let { tag.putUUID("Igniter", it) }
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        igniterUUID = if (tag.hasUUID("Igniter")) tag.getUUID("Igniter") else null
    }

    override fun getAddEntityPacket(): Packet<ClientGamePacketListener> = NetworkHooks.getEntitySpawningPacket(this)
}
