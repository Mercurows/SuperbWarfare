package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.network.message.send.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkRegistry {

    private static void register() {
        NetworkRegistryKt.register();

        playToServer(ShootMessage.TYPE, ShootMessage.STREAM_CODEC, ShootMessage::handler);
        playToServer(VehicleMovementMessage.TYPE, VehicleMovementMessage.STREAM_CODEC, VehicleMovementMessage::handler);
        playToServer(VehicleFireMessage.TYPE, VehicleFireMessage.STREAM_CODEC, VehicleFireMessage::handler);
        playToServer(RadarChangeModeMessage.TYPE, RadarChangeModeMessage.STREAM_CODEC, RadarChangeModeMessage::handler);
        playToServer(RadarSetParametersMessage.TYPE, RadarSetParametersMessage.STREAM_CODEC, RadarSetParametersMessage::handler);
        playToServer(RadarSetPosMessage.TYPE, RadarSetPosMessage.STREAM_CODEC, RadarSetPosMessage::handler);
        playToServer(RadarSetTargetMessage.TYPE, RadarSetTargetMessage.STREAM_CODEC, RadarSetTargetMessage::handler);
        playToServer(SetPerkLevelMessage.TYPE, SetPerkLevelMessage.STREAM_CODEC, SetPerkLevelMessage::handler);
        playToServer(SwitchVehicleWeaponMessage.TYPE, SwitchVehicleWeaponMessage.STREAM_CODEC, SwitchVehicleWeaponMessage::handler);
        playToServer(SwitchScopeMessage.TYPE, SwitchScopeMessage.STREAM_CODEC, SwitchScopeMessage::handler);
        playToServer(ReloadMessage.TYPE, ReloadMessage.STREAM_CODEC, (message2, context2) -> ReloadMessage.handler(context2));
        playToServer(PlayerStopRidingMessage.TYPE, PlayerStopRidingMessage.STREAM_CODEC, PlayerStopRidingMessage::handler);
        playToServer(ZoomMessage.TYPE, ZoomMessage.STREAM_CODEC, ZoomMessage::handler);
        playToServer(SetFiringParametersMessage.TYPE, SetFiringParametersMessage.STREAM_CODEC, (message, context) -> SetFiringParametersMessage.handler(context));
        playToServer(SensitivityMessage.TYPE, SensitivityMessage.STREAM_CODEC, SensitivityMessage::handler);
        playToServer(ShowChargingRangeMessage.TYPE, ShowChargingRangeMessage.STREAM_CODEC, ShowChargingRangeMessage::handler);
        playToServer(UnloadMessage.TYPE, UnloadMessage.STREAM_CODEC, (msg, ctx) -> UnloadMessage.handler(ctx));
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
