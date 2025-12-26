package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {

    private static void register() {
        playToClient(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, (msg, ctx) -> PlayerVariablesSyncMessage.handler(msg));
        playToClient(ShakeClientMessage.TYPE, ShakeClientMessage.STREAM_CODEC, ShakeClientMessage::handler);
        playToClient(ClientMotionSyncMessage.TYPE, ClientMotionSyncMessage.STREAM_CODEC, ClientMotionSyncMessage::handler);
        playToClient(ClientIndicatorMessage.TYPE, ClientIndicatorMessage.STREAM_CODEC, ClientIndicatorMessage::handler);
        playToClient(LivingGunKillMessage.TYPE, LivingGunKillMessage.STREAM_CODEC, LivingGunKillMessage::handler);
        playToClient(GunsDataMessage.TYPE, GunsDataMessage.STREAM_CODEC, GunsDataMessage::handler);
        playToClient(ContainerDataMessage.TYPE, ContainerDataMessage.STREAM_CODEC, ContainerDataMessage::handler);
        playToClient(ShootClientMessage.TYPE, ShootClientMessage.STREAM_CODEC, ShootClientMessage::handler);
        playToClient(DrawClientMessage.TYPE, DrawClientMessage.STREAM_CODEC, (msg, ctx) -> DrawClientMessage.handler());
        playToClient(ResetCameraTypeMessage.TYPE, ResetCameraTypeMessage.STREAM_CODEC, (message2, context2) -> ResetCameraTypeMessage.handler());
        playToClient(RadarMenuOpenMessage.TYPE, RadarMenuOpenMessage.STREAM_CODEC, RadarMenuOpenMessage::handler);
        playToClient(RadarMenuCloseMessage.TYPE, RadarMenuCloseMessage.STREAM_CODEC, (message1, context1) -> RadarMenuCloseMessage.handler());
        playToClient(ClientTacticalSprintSyncMessage.TYPE, ClientTacticalSprintSyncMessage.STREAM_CODEC, (msg, ctx) -> ClientTacticalSprintSyncMessage.handler(msg));
        playToClient(VehiclesDataMessage.TYPE, VehiclesDataMessage.STREAM_CODEC, (msg, ctx) -> VehiclesDataMessage.handler(msg));
        playToClient(ClientSetMotionMessage.TYPE, ClientSetMotionMessage.STREAM_CODEC, (msg, ctx) -> ClientSetMotionMessage.handler(msg));
        playToClient(FinishAssemblingVehicleMessage.TYPE, FinishAssemblingVehicleMessage.STREAM_CODEC, (message3, context3) -> FinishAssemblingVehicleMessage.handler(message3));
        playToClient(TDMSyncMessage.TYPE, TDMSyncMessage.STREAM_CODEC, (message3, context3) -> TDMSyncMessage.handler(message3));
        playToClient(SoundClientMessage.TYPE, SoundClientMessage.STREAM_CODEC, (message3, context3) -> SoundClientMessage.handler(message3));
        playToClient(ClientPhosphorusFireMessage.TYPE, ClientPhosphorusFireMessage.STREAM_CODEC, (message3, context3) -> ClientPhosphorusFireMessage.handler(message3));

        playToServer(LaserShootMessage.TYPE, LaserShootMessage.STREAM_CODEC, LaserShootMessage::handler);
        playToServer(ShootMessage.TYPE, ShootMessage.STREAM_CODEC, ShootMessage::handler);
        playToServer(DoubleJumpMessage.TYPE, DoubleJumpMessage.STREAM_CODEC, (message, context) -> DoubleJumpMessage.handler(context));
        playToServer(ParachuteMessage.TYPE, ParachuteMessage.STREAM_CODEC, (msg, ctx) -> ParachuteMessage.handler(ctx));
        playToServer(VehicleMovementMessage.TYPE, VehicleMovementMessage.STREAM_CODEC, VehicleMovementMessage::handler);
        playToServer(MeleeAttackMessage.TYPE, MeleeAttackMessage.STREAM_CODEC, MeleeAttackMessage::handler);
        playToServer(LungeMineAttackMessage.TYPE, LungeMineAttackMessage.STREAM_CODEC, LungeMineAttackMessage::handler);
        playToServer(VehicleFireMessage.TYPE, VehicleFireMessage.STREAM_CODEC, VehicleFireMessage::handler);
        playToServer(AimVillagerMessage.TYPE, AimVillagerMessage.STREAM_CODEC, AimVillagerMessage::handler);
        playToServer(RadarChangeModeMessage.TYPE, RadarChangeModeMessage.STREAM_CODEC, RadarChangeModeMessage::handler);
        playToServer(RadarSetParametersMessage.TYPE, RadarSetParametersMessage.STREAM_CODEC, RadarSetParametersMessage::handler);
        playToServer(RadarSetPosMessage.TYPE, RadarSetPosMessage.STREAM_CODEC, RadarSetPosMessage::handler);
        playToServer(RadarSetTargetMessage.TYPE, RadarSetTargetMessage.STREAM_CODEC, RadarSetTargetMessage::handler);
        playToServer(GunReforgeMessage.TYPE, GunReforgeMessage.STREAM_CODEC, (message1, context1) -> GunReforgeMessage.handler(context1));
        playToServer(SetPerkLevelMessage.TYPE, SetPerkLevelMessage.STREAM_CODEC, SetPerkLevelMessage::handler);
        playToServer(SwitchVehicleWeaponMessage.TYPE, SwitchVehicleWeaponMessage.STREAM_CODEC, SwitchVehicleWeaponMessage::handler);
        playToServer(AdjustZoomFovMessage.TYPE, AdjustZoomFovMessage.STREAM_CODEC, AdjustZoomFovMessage::handler);
        playToServer(SwitchScopeMessage.TYPE, SwitchScopeMessage.STREAM_CODEC, SwitchScopeMessage::handler);
        playToServer(FireKeyMessage.TYPE, FireKeyMessage.STREAM_CODEC, FireKeyMessage::handler);
        playToServer(ReloadMessage.TYPE, ReloadMessage.STREAM_CODEC, (message2, context2) -> ReloadMessage.handler(context2));
        playToServer(FireModeMessage.TYPE, FireModeMessage.STREAM_CODEC, FireModeMessage::handler);
        playToServer(PlayerStopRidingMessage.TYPE, PlayerStopRidingMessage.STREAM_CODEC, PlayerStopRidingMessage::handler);
        playToServer(ZoomMessage.TYPE, ZoomMessage.STREAM_CODEC, ZoomMessage::handler);
        playToServer(DroneFireMessage.TYPE, DroneFireMessage.STREAM_CODEC, DroneFireMessage::handler);
        playToServer(SetFiringParametersMessage.TYPE, SetFiringParametersMessage.STREAM_CODEC, (message, context) -> SetFiringParametersMessage.handler(context));
        playToServer(ArtilleryIndicatorFireMessage.TYPE, ArtilleryIndicatorFireMessage.STREAM_CODEC, (message, context) -> ArtilleryIndicatorFireMessage.handler(context));
        playToServer(SensitivityMessage.TYPE, SensitivityMessage.STREAM_CODEC, SensitivityMessage::handler);
        playToServer(EditMessage.TYPE, EditMessage.STREAM_CODEC, EditMessage::handler);
        playToServer(InteractMessage.TYPE, InteractMessage.STREAM_CODEC, (interactMessage, context) -> InteractMessage.handler(context));
        playToServer(AdjustMortarAngleMessage.TYPE, AdjustMortarAngleMessage.STREAM_CODEC, AdjustMortarAngleMessage::handler);
        playToServer(ChangeVehicleSeatMessage.TYPE, ChangeVehicleSeatMessage.STREAM_CODEC, ChangeVehicleSeatMessage::handler);
        playToServer(ShowChargingRangeMessage.TYPE, ShowChargingRangeMessage.STREAM_CODEC, ShowChargingRangeMessage::handler);
        playToServer(TacticalSprintMessage.TYPE, TacticalSprintMessage.STREAM_CODEC, TacticalSprintMessage::handler);
        playToServer(DogTagFinishEditMessage.TYPE, DogTagFinishEditMessage.STREAM_CODEC, DogTagFinishEditMessage::handler);
        playToServer(MouseMoveMessage.TYPE, MouseMoveMessage.STREAM_CODEC, MouseMoveMessage::handler);
        playToServer(FiringParametersEditMessage.TYPE, FiringParametersEditMessage.STREAM_CODEC, FiringParametersEditMessage::handler);
        playToServer(UnloadMessage.TYPE, UnloadMessage.STREAM_CODEC, (msg, ctx) -> UnloadMessage.handler(ctx));
        playToServer(AssembleVehicleMessage.TYPE, AssembleVehicleMessage.STREAM_CODEC, AssembleVehicleMessage::handler);
        playToServer(SeekingWeaponWarningMessage.TYPE, SeekingWeaponWarningMessage.STREAM_CODEC, SeekingWeaponWarningMessage::handler);
        playToServer(WeaponZoomingMessage.TYPE, WeaponZoomingMessage.STREAM_CODEC, WeaponZoomingMessage::handler);
    }

    private static PayloadRegistrar registrar;

    public static void register(final RegisterPayloadHandlersEvent event) {
        registrar = event.registrar("1");
        register();
    }

    public static <T extends CustomPacketPayload> void playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
        registrar.playToClient(type, reader, handler);
    }

    public static <T extends CustomPacketPayload> void playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
        registrar.playToServer(type, reader, handler);
    }
}
