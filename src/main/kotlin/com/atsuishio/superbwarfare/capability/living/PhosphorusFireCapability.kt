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
data class PhosphorusFireCapability(
    @SerialName("SbwPhosphorusFire")
    var isOnFire: Boolean = false,
) : INBTSerializable<CompoundTag>, SyncedCapability {

    override fun serializeNBT(): CompoundTag =
        encodeToCompoundTag(serializer<PhosphorusFireCapability>(), this)

    override fun deserializeNBT(nbt: CompoundTag) {
        isOnFire = decodeFromCompoundTag(serializer<PhosphorusFireCapability>(), nbt).isOnFire
    }

    // 只有一个字段，增量与全量没有区别
    override fun writeSync(encoder: ByteBufEncoder, full: Boolean) {
        encoder.encodeBoolean(isOnFire)
    }

    override fun readSync(decoder: ByteBufDecoder, full: Boolean) {
        isOnFire = decoder.decodeBoolean()
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
            // 由 CapabilitySync 自动决定何时、向谁同步（客户端调用时无副作用）
            CapabilitySync.markDirty(entity, ID)
        }
    }
}
