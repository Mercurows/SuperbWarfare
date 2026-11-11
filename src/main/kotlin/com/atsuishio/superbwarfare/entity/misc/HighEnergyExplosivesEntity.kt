package com.atsuishio.superbwarfare.entity.misc

import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.tools.CustomExplosion
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.level.Level
import java.util.*

class HighEnergyExplosivesEntity(type: EntityType<out HighEnergyExplosivesEntity>, level: Level) : PrimedTnt(type, level) {
    var igniterUUID: UUID? = null

    init {
        // PrimedTnt synchronizes and saves its rendered block state in 1.21.1.
        blockState = ModBlocks.HIGH_ENERGY_EXPLOSIVES_BLOCK.get().defaultBlockState()
    }

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
}
