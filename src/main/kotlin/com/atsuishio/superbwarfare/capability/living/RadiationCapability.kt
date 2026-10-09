package com.atsuishio.superbwarfare.capability.living

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.capability.sync.CapabilitySync
import com.atsuishio.superbwarfare.capability.sync.SyncedCapability
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
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
data class RadiationCapability(
    @SerialName("SbwRadiationDosage")
    var dosage: Float = 0f,
) : INBTSerializable<CompoundTag>, SyncedCapability {

    override fun serializeNBT(): CompoundTag =
        encodeToCompoundTag(serializer<RadiationCapability>(), this)

    override fun deserializeNBT(nbt: CompoundTag) {
        dosage = decodeFromCompoundTag(serializer<RadiationCapability>(), nbt).dosage
    }

    override fun writeSync(encoder: ByteBufEncoder, full: Boolean) {
        encoder.encodeFloat(dosage)
    }

    override fun readSync(decoder: ByteBufDecoder, full: Boolean) {
        dosage = decoder.decodeFloat()
    }

    companion object {
        val ID: ResourceLocation = loc("radiation_capability")

        const val MAX_DOSAGE = 20000f

        @JvmStatic
        fun get(entity: Entity): RadiationCapability {
            return entity.getCapability(ModCapabilities.RADIATION_CAPABILITY)
                .orElseGet { RadiationCapability() }
        }

        @JvmStatic
        fun getOrNull(entity: Entity): RadiationCapability? {
            return entity.getCapability(ModCapabilities.RADIATION_CAPABILITY).resolve().orElse(null)
        }

        @JvmStatic
        fun getDosage(entity: Entity): Float {
            return get(entity).dosage
        }

        @JvmStatic
        fun setDosage(entity: Entity, value: Float): Float {
            val capability = get(entity)
            capability.dosage = value.coerceIn(0f, MAX_DOSAGE)
            CapabilitySync.markDirty(entity, ID)
            return capability.dosage
        }

        @JvmStatic
        fun addDosage(entity: Entity, amount: Float): Float {
            return setDosage(entity, getDosage(entity) + amount)
        }
    }
}
