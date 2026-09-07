package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.serialization.decodeFromCompoundTag
import com.atsuishio.superbwarfare.serialization.encodeToCompoundTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable

@AutoRegisterCapability
@Serializable
data class PhosphorusFireCapability(
    @SerialName("SbwPhosphorusFire")
    var isOnFire: Boolean = false,
) : INBTSerializable<CompoundTag> {

    override fun serializeNBT(): CompoundTag =
        encodeToCompoundTag(serializer<PhosphorusFireCapability>(), this)

    override fun deserializeNBT(nbt: CompoundTag) {
        isOnFire = decodeFromCompoundTag(serializer<PhosphorusFireCapability>(), nbt).isOnFire
    }

    companion object {
        val ID: ResourceLocation = loc("phosphorus_fire_capability")

        @JvmStatic
        fun get(entity: Entity): PhosphorusFireCapability {
            return entity.getCapability(ModCapabilities.PHOSPHORUS_FIRE_CAPABILITY)
                .orElseGet { PhosphorusFireCapability() }
        }

        @JvmStatic
        fun set(entity: Entity, value: Boolean) {
            get(entity).isOnFire = value
        }
    }
}
