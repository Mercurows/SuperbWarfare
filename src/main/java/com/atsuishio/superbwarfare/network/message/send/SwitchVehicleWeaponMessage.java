package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SwitchVehicleWeaponMessage(int index, double value, boolean isScroll) implements CustomPacketPayload {
    public static final Type<SwitchVehicleWeaponMessage> TYPE = new Type<>(Mod.loc("switch_vehicle_weapon"));

    public static final StreamCodec<ByteBuf, SwitchVehicleWeaponMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SwitchVehicleWeaponMessage::index,
            ByteBufCodecs.DOUBLE,
            SwitchVehicleWeaponMessage::value,
            ByteBufCodecs.BOOL,
            SwitchVehicleWeaponMessage::isScroll,
            SwitchVehicleWeaponMessage::new
    );

    public static void handler(SwitchVehicleWeaponMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        if (player.getVehicle() instanceof WeaponVehicleEntity weaponVehicle && weaponVehicle.isDriver(player)) {
            var value = message.isScroll ? (Mth.clamp(message.value > 0 ? Mth.ceil(message.value) : Mth.floor(message.value), -1, 1)) : message.value;
            weaponVehicle.changeWeapon(message.index, (int) value, message.isScroll);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
