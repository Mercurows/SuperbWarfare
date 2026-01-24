package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.network.message.receive.*
import com.atsuishio.superbwarfare.network.message.send.*
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import java.util.function.BiConsumer
import java.util.function.Function

private inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

private inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

private inline fun <reified T : PacketPayload> playTo(
    reg: (BiConsumer<T, FriendlyByteBuf>, Function<FriendlyByteBuf, T>, BiConsumer<T, PayloadContext>) -> Unit
) {
    val instance = T::class.objectInstance
    if (instance != null) {
        reg({ _, _ -> }, { instance }, { msg, context -> msg.handleInternal(msg, context) })
    } else {
        reg(
            { value, buf -> encodeTo(buf, value) },
            { buf -> decodeFrom(buf) },
            { msg, context -> msg.handleInternal(msg, context) }
        )
    }
}

private inline fun <reified T : ServerPacketPayload> playToServer() {
    playTo<T> { enc, dec, handler ->
        NetworkRegistry.playToServer(T::class.java, enc, dec, handler)
    }
}

private inline fun <reified T : ClientPacketPayload> playToClient() {
    playTo<T> { enc, dec, handler ->
        NetworkRegistry.playToClient(T::class.java, enc, dec, handler)
    }
}

fun initializeNetwork() {
    registerPayloads()
}

private fun registerPayloads() {
    playToClient<ClientIndicatorMessage>()
    playToClient<ClientSetMotionMessage>()
    playToClient<DataSyncMessage>()
    playToClient<ClientMotionSyncMessage>()
    playToClient<ClientPhosphorusFireMessage>()
    playToClient<ContainerDataMessage>()
    playToClient<DrawClientMessage>()
    playToClient<FinishAssemblingVehicleMessage>()
    playToClient<LivingGunKillMessage>()
    playToClient<PlayerVariablesSyncMessage>()
    playToClient<RadarMenuCloseMessage>()
    playToClient<RadarMenuOpenMessage>()
    playToClient<ResetCameraTypeMessage>()
    playToClient<ShakeClientMessage>()
    playToClient<ShootClientMessage>()
    playToClient<SoundClientMessage>()
    playToClient<TDMSyncMessage>()

    playToServer<AdjustMortarAngleMessage>()
    playToServer<AdjustZoomFovMessage>()
    playToServer<AimVillagerMessage>()
    playToServer<AssembleVehicleMessage>()
    playToServer<ChangeVehicleSeatMessage>()
    playToServer<ActiveThermalImagingMessage>()
    playToServer<ArtilleryIndicatorFireMessage>()
    playToServer<DogTagFinishEditMessage>()
    playToServer<DoubleJumpMessage>()
    playToServer<DroneFireMessage>()
    playToServer<EditMessage>()
    playToServer<FireKeyMessage>()
    playToServer<FireModeMessage>()
    playToServer<FiringParametersEditMessage>()
    playToServer<GunReforgeMessage>()
    playToServer<InteractMessage>()
    playToServer<LaserShootMessage>()
    playToServer<LungeMineAttackMessage>()
    playToServer<MeleeAttackMessage>()
    playToServer<MouseMoveMessage>()
    playToServer<ParachuteMessage>()
    playToServer<PlayerStopRidingMessage>()
    playToServer<RadarChangeModeMessage>()
    playToServer<RadarSetPosMessage>()
    playToServer<RadarSetTargetMessage>()
    playToServer<ReloadMessage>()
    playToServer<SeekingWeaponWarningMessage>()
    playToServer<SensitivityMessage>()
    playToServer<SetFiringParametersMessage>()
    playToServer<SetPerkLevelMessage>()
    playToServer<ShootMessage>()
    playToServer<ShowChargingRangeMessage>()
    playToServer<SwitchScopeMessage>()
    playToServer<SwitchVehicleWeaponMessage>()
    playToServer<UnloadMessage>()
    playToServer<VehicleFireMessage>()
    playToServer<VehicleMovementMessage>()
    playToServer<WeaponZoomingMessage>()
    playToServer<ZoomMessage>()
}