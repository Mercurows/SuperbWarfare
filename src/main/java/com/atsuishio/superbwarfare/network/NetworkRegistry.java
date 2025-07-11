package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.Mod.PACKET_HANDLER;
import static com.atsuishio.superbwarfare.Mod.messageID;

public class NetworkRegistry {

    public static void register() {
        playToClient(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
        playToClient(ShakeClientMessage.class, ShakeClientMessage::encode, ShakeClientMessage::decode, ShakeClientMessage::handler);
        playToClient(ClientMotionSyncMessage.class, ClientMotionSyncMessage::encode, ClientMotionSyncMessage::decode, ClientMotionSyncMessage::handler);
        playToClient(ClientIndicatorMessage.class, ClientIndicatorMessage::encode, ClientIndicatorMessage::decode, ClientIndicatorMessage::handler);
        playToClient(PlayerGunKillMessage.class, PlayerGunKillMessage::encode, PlayerGunKillMessage::decode, PlayerGunKillMessage::handler);
        playToClient(GunsDataMessage.class, GunsDataMessage::encode, GunsDataMessage::decode, (message, ctx) -> GunsDataMessage.handler(message));
        playToClient(ContainerDataMessage.class, ContainerDataMessage::encode, ContainerDataMessage::decode, ContainerDataMessage::handler);
        playToClient(ShootClientMessage.class, ShootClientMessage::encode, ShootClientMessage::decode, (message, context) -> ShootClientMessage.handler(context));
        playToClient(DrawClientMessage.class, DrawClientMessage::encode, DrawClientMessage::decode, (msg, ctx) -> DrawClientMessage.handler(ctx));
        playToClient(ResetCameraTypeMessage.INSTANCE, ResetCameraTypeMessage::handler);
        playToClient(RadarMenuOpenMessage.class, RadarMenuOpenMessage::encode, RadarMenuOpenMessage::decode, RadarMenuOpenMessage::handler);
        playToClient(RadarMenuCloseMessage.INSTANCE, RadarMenuCloseMessage::handler);
        playToClient(ClientTacticalSprintSyncMessage.class, ClientTacticalSprintSyncMessage::encode, ClientTacticalSprintSyncMessage::decode, ClientTacticalSprintSyncMessage::handler);
        playToClient(VehiclesDataMessage.class, VehiclesDataMessage::encode, VehiclesDataMessage::decode, (msg, ctx) -> VehiclesDataMessage.handler(msg));

        playToServer(LaserShootMessage.class, LaserShootMessage::encode, LaserShootMessage::decode, LaserShootMessage::handler);
        playToServer(ShootMessage.class, ShootMessage::encode, ShootMessage::decode, ShootMessage::handler);
        playToServer(DoubleJumpMessage.INSTANCE, DoubleJumpMessage::handler);
        playToServer(VehicleMovementMessage.class, VehicleMovementMessage::encode, VehicleMovementMessage::decode, VehicleMovementMessage::handler);
        playToServer(MeleeAttackMessage.class, MeleeAttackMessage::encode, MeleeAttackMessage::decode, MeleeAttackMessage::handler);
        playToServer(LungeMineAttackMessage.class, LungeMineAttackMessage::encode, LungeMineAttackMessage::decode, LungeMineAttackMessage::handler);
        playToServer(VehicleFireMessage.class, VehicleFireMessage::encode, VehicleFireMessage::decode, VehicleFireMessage::handler);
        playToServer(AimVillagerMessage.class, AimVillagerMessage::encode, AimVillagerMessage::decode, AimVillagerMessage::handler);
        playToServer(RadarChangeModeMessage.class, RadarChangeModeMessage::encode, RadarChangeModeMessage::decode, RadarChangeModeMessage::handler);
        playToServer(RadarSetParametersMessage.class, RadarSetParametersMessage::encode, RadarSetParametersMessage::decode, RadarSetParametersMessage::handler);
        playToServer(RadarSetPosMessage.class, RadarSetPosMessage::encode, RadarSetPosMessage::decode, RadarSetPosMessage::handler);
        playToServer(RadarSetTargetMessage.class, RadarSetTargetMessage::encode, RadarSetTargetMessage::decode, RadarSetTargetMessage::handler);
        playToServer(GunReforgeMessage.INSTANCE, GunReforgeMessage::handler);
        playToServer(SetPerkLevelMessage.class, SetPerkLevelMessage::encode, SetPerkLevelMessage::decode, SetPerkLevelMessage::handler);
        playToServer(SwitchVehicleWeaponMessage.class, SwitchVehicleWeaponMessage::encode, SwitchVehicleWeaponMessage::decode, SwitchVehicleWeaponMessage::handler);
        playToServer(AdjustZoomFovMessage.class, AdjustZoomFovMessage::encode, AdjustZoomFovMessage::decode, AdjustZoomFovMessage::handler);
        playToServer(SwitchScopeMessage.class, SwitchScopeMessage::encode, SwitchScopeMessage::decode, SwitchScopeMessage::handler);
        playToServer(FireKeyMessage.class, FireKeyMessage::encode, FireKeyMessage::decode, FireKeyMessage::handler);
        playToServer(ReloadMessage.INSTANCE, ReloadMessage::handler);
        playToServer(FireModeMessage.INSTANCE, FireModeMessage::handler);
        playToServer(PlayerStopRidingMessage.INSTANCE, PlayerStopRidingMessage::handler);
        playToServer(ZoomMessage.class, ZoomMessage::encode, ZoomMessage::decode, ZoomMessage::handler);
        playToServer(DroneFireMessage.class, DroneFireMessage::encode, DroneFireMessage::decode, DroneFireMessage::handler);
        playToServer(SetFiringParametersMessage.INSTANCE, SetFiringParametersMessage::handler);
        playToServer(ArtilleryIndicatorFireMessage.INSTANCE, ArtilleryIndicatorFireMessage::handler);
        playToServer(SensitivityMessage.class, SensitivityMessage::encode, SensitivityMessage::decode, SensitivityMessage::handler);
        playToServer(EditMessage.class, EditMessage::encode, EditMessage::decode, EditMessage::handler);
        playToServer(InteractMessage.INSTANCE, InteractMessage::handler);
        playToServer(AdjustMortarAngleMessage.class, AdjustMortarAngleMessage::encode, AdjustMortarAngleMessage::decode, AdjustMortarAngleMessage::handler);
        playToServer(ChangeVehicleSeatMessage.class, ChangeVehicleSeatMessage::encode, ChangeVehicleSeatMessage::decode, ChangeVehicleSeatMessage::handler);
        playToServer(ShowChargingRangeMessage.class, ShowChargingRangeMessage::encode, ShowChargingRangeMessage::decode, ShowChargingRangeMessage::handler);
        playToServer(TacticalSprintMessage.class, TacticalSprintMessage::encode, TacticalSprintMessage::decode, TacticalSprintMessage::handler);
        playToServer(DogTagFinishEditMessage.class, DogTagFinishEditMessage::encode, DogTagFinishEditMessage::decode, DogTagFinishEditMessage::handler);
        playToServer(MouseMoveMessage.class, MouseMoveMessage::encode, MouseMoveMessage::decode, MouseMoveMessage::handler);
        playToServer(FiringParametersEditMessage.class, FiringParametersEditMessage::encode, FiringParametersEditMessage::decode, FiringParametersEditMessage::handler);

        var registerContainerEvent = new RegisterContainersEvent();
        FMLJavaModLoadingContext.get().getModEventBus().post(registerContainerEvent);
    }

    public static <T> void playToClient(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        messageID++;
    }

    /**
     * 注册无参数、向客户端发送的消息
     */
    @SuppressWarnings("unchecked")
    public static <T> void playToClient(T instance, Consumer<Supplier<NetworkEvent.Context>> messageConsumer) {
        var type = (Class<T>) instance.getClass();
        PACKET_HANDLER.registerMessage(messageID, type, (msg, buf) -> {
        }, (buf) -> instance, (msg, ctx) -> messageConsumer.accept(ctx), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        messageID++;
    }

    public static <T> void playToServer(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        messageID++;
    }

    /**
     * 注册无参数、向服务器发送的消息
     */
    @SuppressWarnings("unchecked")
    public static <T> void playToServer(T instance, Consumer<Supplier<NetworkEvent.Context>> messageConsumer) {
        var type = (Class<T>) instance.getClass();
        PACKET_HANDLER.registerMessage(messageID, type, (msg, buf) -> {
        }, (buf) -> instance, (msg, ctx) -> messageConsumer.accept(ctx), Optional.of(NetworkDirection.PLAY_TO_SERVER));
        messageID++;
    }
}
