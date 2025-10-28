package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.network.message.receive.*;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkRegistry {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = net.minecraftforge.network.NetworkRegistry.newSimpleChannel(new ResourceLocation(Mod.MODID, Mod.MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static int messageID = 0;

    public static void register() {
        playToClient(PlayerVariablesSyncMessage.class, PlayerVariablesSyncMessage::buffer, PlayerVariablesSyncMessage::new, PlayerVariablesSyncMessage::handler);
        playToClient(ShakeClientMessage.class, ShakeClientMessage::encode, ShakeClientMessage::decode, ShakeClientMessage::handler);
        playToClient(ClientMotionSyncMessage.class, ClientMotionSyncMessage::encode, ClientMotionSyncMessage::decode, ClientMotionSyncMessage::handler);
        playToClient(ClientIndicatorMessage.class, ClientIndicatorMessage::encode, ClientIndicatorMessage::decode, ClientIndicatorMessage::handler);
        playToClient(LivingGunKillMessage.class, LivingGunKillMessage::encode, LivingGunKillMessage::decode, LivingGunKillMessage::handler);
        playToClient(GunsDataMessage.class, GunsDataMessage::encode, GunsDataMessage::decode, (message, ctx) -> GunsDataMessage.handler(message));
        playToClient(ContainerDataMessage.class, ContainerDataMessage::encode, ContainerDataMessage::decode, ContainerDataMessage::handler);
        playToClient(ShootClientMessage.class, ShootClientMessage::encode, ShootClientMessage::decode, (message, context) -> ShootClientMessage.handler(context));
        playToClient(DrawClientMessage.class, DrawClientMessage::encode, DrawClientMessage::decode, (msg, ctx) -> DrawClientMessage.handler(ctx));
        playToClient(ResetCameraTypeMessage.INSTANCE, ResetCameraTypeMessage::handler);
        playToClient(RadarMenuOpenMessage.class, RadarMenuOpenMessage::encode, RadarMenuOpenMessage::decode, RadarMenuOpenMessage::handler);
        playToClient(RadarMenuCloseMessage.INSTANCE, RadarMenuCloseMessage::handler);
        playToClient(ClientTacticalSprintSyncMessage.class, ClientTacticalSprintSyncMessage::encode, ClientTacticalSprintSyncMessage::decode, ClientTacticalSprintSyncMessage::handler);
        playToClient(VehiclesDataMessage.class, VehiclesDataMessage::encode, VehiclesDataMessage::decode, (msg, ctx) -> VehiclesDataMessage.handler(msg));
        playToClient(ClientSetMotionMessage.class, ClientSetMotionMessage::encode, ClientSetMotionMessage::decode, ClientSetMotionMessage::handler);
        playToClient(FinishAssemblingVehicleMessage.class, FinishAssemblingVehicleMessage::encode, FinishAssemblingVehicleMessage::decode, FinishAssemblingVehicleMessage::handler);
        playToClient(TDMSyncMessage.class, TDMSyncMessage::encode, TDMSyncMessage::decode, TDMSyncMessage::handler);
        playToClient(SoundClientMessage.class, SoundClientMessage::encode, SoundClientMessage::decode, SoundClientMessage::handler);

        playToServer(LaserShootMessage.class, LaserShootMessage::encode, LaserShootMessage::decode, LaserShootMessage::handler);
        playToServer(ShootMessage.class, ShootMessage::encode, ShootMessage::decode, ShootMessage::handler);
        playToServer(SeekingWeaponWarningMessage.class, SeekingWeaponWarningMessage::encode, SeekingWeaponWarningMessage::decode, SeekingWeaponWarningMessage::handler);
        playToServer(DoubleJumpMessage.INSTANCE, DoubleJumpMessage::handler);
        playToServer(ParachuteMessage.INSTANCE, ParachuteMessage::handler);
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
        playToServer(FireModeMessage.class, FireModeMessage::encode, FireModeMessage::decode, FireModeMessage::handler);
        playToServer(PlayerStopRidingMessage.class, PlayerStopRidingMessage::encode, PlayerStopRidingMessage::decode, PlayerStopRidingMessage::handler);
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
        playToServer(UnloadMessage.INSTANCE, UnloadMessage::handler);
        playToServer(AssembleVehicleMessage.class, AssembleVehicleMessage::encode, AssembleVehicleMessage::decode, AssembleVehicleMessage::handler);
        playToServer(WeaponZoomingMessage.class, WeaponZoomingMessage::encode, WeaponZoomingMessage::decode, WeaponZoomingMessage::handler);

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
