package com.atsuishio.superbwarfare.network;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.atsuishio.superbwarfare.network.message.send.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import thedarkcolour.kotlinforforge.forge.ForgeKt;

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
        NetworkRegistryKt.register();

        playToServer(ShootMessage.class, ShootMessage::encode, ShootMessage::decode, ShootMessage::handler);
        playToServer(SeekingWeaponWarningMessage.class, SeekingWeaponWarningMessage::encode, SeekingWeaponWarningMessage::decode, SeekingWeaponWarningMessage::handler);
        playToServer(VehicleMovementMessage.class, VehicleMovementMessage::encode, VehicleMovementMessage::decode, VehicleMovementMessage::handler);
        playToServer(VehicleFireMessage.class, VehicleFireMessage::encode, VehicleFireMessage::decode, VehicleFireMessage::handler);
        playToServer(RadarChangeModeMessage.class, RadarChangeModeMessage::encode, RadarChangeModeMessage::decode, RadarChangeModeMessage::handler);
        playToServer(RadarSetParametersMessage.class, RadarSetParametersMessage::encode, RadarSetParametersMessage::decode, RadarSetParametersMessage::handler);
        playToServer(RadarSetPosMessage.class, RadarSetPosMessage::encode, RadarSetPosMessage::decode, RadarSetPosMessage::handler);
        playToServer(RadarSetTargetMessage.class, RadarSetTargetMessage::encode, RadarSetTargetMessage::decode, RadarSetTargetMessage::handler);
        playToServer(SetPerkLevelMessage.class, SetPerkLevelMessage::encode, SetPerkLevelMessage::decode, SetPerkLevelMessage::handler);
        playToServer(SwitchVehicleWeaponMessage.class, SwitchVehicleWeaponMessage::encode, SwitchVehicleWeaponMessage::decode, SwitchVehicleWeaponMessage::handler);
        playToServer(SwitchScopeMessage.class, SwitchScopeMessage::encode, SwitchScopeMessage::decode, SwitchScopeMessage::handler);
        playToServer(ReloadMessage.INSTANCE, ReloadMessage::handler);
        playToServer(PlayerStopRidingMessage.class, PlayerStopRidingMessage::encode, PlayerStopRidingMessage::decode, PlayerStopRidingMessage::handler);
        playToServer(ZoomMessage.class, ZoomMessage::encode, ZoomMessage::decode, ZoomMessage::handler);
        playToServer(SetFiringParametersMessage.INSTANCE, SetFiringParametersMessage::handler);
        playToServer(SensitivityMessage.class, SensitivityMessage::encode, SensitivityMessage::decode, SensitivityMessage::handler);
        playToServer(ShowChargingRangeMessage.class, ShowChargingRangeMessage::encode, ShowChargingRangeMessage::decode, ShowChargingRangeMessage::handler);
        playToServer(UnloadMessage.INSTANCE, UnloadMessage::handler);
        playToServer(WeaponZoomingMessage.class, WeaponZoomingMessage::encode, WeaponZoomingMessage::decode, WeaponZoomingMessage::handler);

        var registerContainerEvent = new RegisterContainersEvent();
        ForgeKt.getMOD_BUS().post(registerContainerEvent);
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
