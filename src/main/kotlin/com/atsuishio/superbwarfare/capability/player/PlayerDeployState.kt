package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.capability.sync.CapabilitySync
import com.atsuishio.superbwarfare.capability.sync.SyncTarget
import com.atsuishio.superbwarfare.capability.sync.SyncedCapability
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability
import net.minecraftforge.common.util.INBTSerializable
import java.util.function.Consumer

/**
 * 重武器架设状态：架在三脚架上的位置，以及被架的是哪把枪（按枪自己的 uuid 认）。
 */
@AutoRegisterCapability
class PlayerDeployState : INBTSerializable<CompoundTag>, SyncedCapability {

    @JvmField
    var tripodPos: BlockPos? = null

    @JvmField
    var gunUuid: String? = null

    override val syncTarget: SyncTarget
        get() = SyncTarget.TRACKING

    override fun writeSync(encoder: ByteBufEncoder, full: Boolean) {
        val pos = tripodPos

        encoder.encodeBoolean(pos != null)
        if (pos != null) {
            encoder.encodeLong(pos.asLong())
        }
        encoder.encodeString(gunUuid ?: "")
    }

    override fun readSync(decoder: ByteBufDecoder, full: Boolean) {
        tripodPos = if (decoder.decodeBoolean()) BlockPos.of(decoder.decodeLong()) else null
        gunUuid = decoder.decodeString().takeIf { it.isNotEmpty() }
    }

    // 不写 NBT、不进 clonePlayer：下线/死亡即解除
    override fun serializeNBT(): CompoundTag = CompoundTag()

    override fun deserializeNBT(nbt: CompoundTag) {}

    companion object {
        var ID: ResourceLocation = loc("player_deploy_state")

        @JvmStatic
        fun getOrDefault(entity: Entity): PlayerDeployState {
            return entity.getCapability(ModCapabilities.PLAYER_DEPLOY_STATE).orElse(PlayerDeployState())
        }

        @JvmStatic
        fun modify(entity: Entity, consumer: Consumer<PlayerDeployState>) {
            if (entity.level().isClientSide) return

            val cap = entity.getCapability(ModCapabilities.PLAYER_DEPLOY_STATE).orElse(PlayerDeployState())
            val oldPos = cap.tripodPos
            val oldUuid = cap.gunUuid

            consumer.accept(cap)

            if (cap.tripodPos == oldPos && cap.gunUuid == oldUuid) return

            CapabilitySync.markDirty(entity, ID)
        }
    }
}
