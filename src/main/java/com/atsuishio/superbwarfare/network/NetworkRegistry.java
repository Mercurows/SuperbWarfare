package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, (msg, ctx) -> PlayerVariablesSyncMessage.handler(msg));
        registrar.playToClient(ShakeClientMessage.TYPE, ShakeClientMessage.STREAM_CODEC, ShakeClientMessage::handler);
        registrar.playToClient(ClientMotionSyncMessage.TYPE, ClientMotionSyncMessage.STREAM_CODEC, ClientMotionSyncMessage::handler);
        registrar.playToClient(ClientIndicatorMessage.TYPE, ClientIndicatorMessage.STREAM_CODEC, ClientIndicatorMessage::handler);
        registrar.playToClient(PlayerGunKillMessage.TYPE, PlayerGunKillMessage.STREAM_CODEC, PlayerGunKillMessage::handler);
        registrar.playToClient(GunsDataMessage.TYPE, GunsDataMessage.STREAM_CODEC, GunsDataMessage::handler);
        registrar.playToClient(ContainerDataMessage.TYPE, ContainerDataMessage.STREAM_CODEC, ContainerDataMessage::handler);
        registrar.playToClient(ShootClientMessage.TYPE, ShootClientMessage.STREAM_CODEC, ShootClientMessage::handler);
        registrar.playToClient(DrawClientMessage.TYPE, DrawClientMessage.STREAM_CODEC, (msg, ctx) -> DrawClientMessage.handler());
        registrar.playToClient(ResetCameraTypeMessage.TYPE, ResetCameraTypeMessage.STREAM_CODEC, ResetCameraTypeMessage::handler);
        registrar.playToClient(RadarMenuOpenMessage.TYPE, RadarMenuOpenMessage.STREAM_CODEC, RadarMenuOpenMessage::handler);
        registrar.playToClient(RadarMenuCloseMessage.TYPE, RadarMenuCloseMessage.STREAM_CODEC, RadarMenuCloseMessage::handler);
        registrar.playToClient(SimulationDistanceMessage.TYPE, SimulationDistanceMessage.STREAM_CODEC, SimulationDistanceMessage::handler);
        registrar.playToClient(ClientTacticalSprintSyncMessage.TYPE, ClientTacticalSprintSyncMessage.STREAM_CODEC, (msg, ctx) -> ClientTacticalSprintSyncMessage.handler(msg));
        registrar.playToClient(DogTagEditorMessage.TYPE, DogTagEditorMessage.STREAM_CODEC, (msg, ctx) -> DogTagEditorMessage.handler(msg));
        registrar.playToClient(VehiclesDataMessage.TYPE, VehiclesDataMessage.STREAM_CODEC, (msg, ctx) -> VehiclesDataMessage.handler(msg));

        registrar.playToServer(LaserShootMessage.TYPE, LaserShootMessage.STREAM_CODEC, LaserShootMessage::handler);
        registrar.playToServer(ShootMessage.TYPE, ShootMessage.STREAM_CODEC, ShootMessage::handler);
        registrar.playToServer(DoubleJumpMessage.TYPE, DoubleJumpMessage.STREAM_CODEC, (message, context) -> DoubleJumpMessage.handler(context));
        registrar.playToServer(VehicleMovementMessage.TYPE, VehicleMovementMessage.STREAM_CODEC, VehicleMovementMessage::handler);
        registrar.playToServer(MeleeAttackMessage.TYPE, MeleeAttackMessage.STREAM_CODEC, MeleeAttackMessage::handler);
        registrar.playToServer(LungeMineAttackMessage.TYPE, LungeMineAttackMessage.STREAM_CODEC, LungeMineAttackMessage::handler);
        registrar.playToServer(VehicleFireMessage.TYPE, VehicleFireMessage.STREAM_CODEC, VehicleFireMessage::handler);
        registrar.playToServer(AimVillagerMessage.TYPE, AimVillagerMessage.STREAM_CODEC, AimVillagerMessage::handler);
        registrar.playToServer(RadarChangeModeMessage.TYPE, RadarChangeModeMessage.STREAM_CODEC, RadarChangeModeMessage::handler);
        registrar.playToServer(RadarSetParametersMessage.TYPE, RadarSetParametersMessage.STREAM_CODEC, RadarSetParametersMessage::handler);
        registrar.playToServer(RadarSetPosMessage.TYPE, RadarSetPosMessage.STREAM_CODEC, RadarSetPosMessage::handler);
        registrar.playToServer(RadarSetTargetMessage.TYPE, RadarSetTargetMessage.STREAM_CODEC, RadarSetTargetMessage::handler);
        registrar.playToServer(GunReforgeMessage.TYPE, GunReforgeMessage.STREAM_CODEC, GunReforgeMessage::handler);
        registrar.playToServer(SetPerkLevelMessage.TYPE, SetPerkLevelMessage.STREAM_CODEC, SetPerkLevelMessage::handler);
        registrar.playToServer(SwitchVehicleWeaponMessage.TYPE, SwitchVehicleWeaponMessage.STREAM_CODEC, SwitchVehicleWeaponMessage::handler);
        registrar.playToServer(AdjustZoomFovMessage.TYPE, AdjustZoomFovMessage.STREAM_CODEC, AdjustZoomFovMessage::handler);
        registrar.playToServer(SwitchScopeMessage.TYPE, SwitchScopeMessage.STREAM_CODEC, SwitchScopeMessage::handler);
        registrar.playToServer(FireKeyMessage.TYPE, FireKeyMessage.STREAM_CODEC, FireKeyMessage::handler);
        registrar.playToServer(ReloadMessage.TYPE, ReloadMessage.STREAM_CODEC, ReloadMessage::handler);
        registrar.playToServer(FireModeMessage.TYPE, FireModeMessage.STREAM_CODEC, FireModeMessage::handler);
        registrar.playToServer(PlayerStopRidingMessage.TYPE, PlayerStopRidingMessage.STREAM_CODEC, PlayerStopRidingMessage::handler);
        registrar.playToServer(ZoomMessage.TYPE, ZoomMessage.STREAM_CODEC, ZoomMessage::handler);
        registrar.playToServer(DroneFireMessage.TYPE, DroneFireMessage.STREAM_CODEC, DroneFireMessage::handler);
        registrar.playToServer(SetFiringParametersMessage.TYPE, SetFiringParametersMessage.STREAM_CODEC, SetFiringParametersMessage::handler);
        registrar.playToServer(SensitivityMessage.TYPE, SensitivityMessage.STREAM_CODEC, SensitivityMessage::handler);
        registrar.playToServer(EditMessage.TYPE, EditMessage.STREAM_CODEC, EditMessage::handler);
        registrar.playToServer(InteractMessage.TYPE, InteractMessage.STREAM_CODEC, InteractMessage::handler);
        registrar.playToServer(AdjustMortarAngleMessage.TYPE, AdjustMortarAngleMessage.STREAM_CODEC, AdjustMortarAngleMessage::handler);
        registrar.playToServer(ChangeVehicleSeatMessage.TYPE, ChangeVehicleSeatMessage.STREAM_CODEC, ChangeVehicleSeatMessage::handler);
        registrar.playToServer(ShowChargingRangeMessage.TYPE, ShowChargingRangeMessage.STREAM_CODEC, ShowChargingRangeMessage::handler);
        registrar.playToServer(TacticalSprintMessage.TYPE, TacticalSprintMessage.STREAM_CODEC, TacticalSprintMessage::handler);
        registrar.playToServer(DogTagFinishEditMessage.TYPE, DogTagFinishEditMessage.STREAM_CODEC, DogTagFinishEditMessage::handler);
        registrar.playToServer(MouseMoveMessage.TYPE, MouseMoveMessage.STREAM_CODEC, MouseMoveMessage::handler);
    }
}
