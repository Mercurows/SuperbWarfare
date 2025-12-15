package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum ReloadMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<ReloadMessage> TYPE = new Type<>(Mod.loc("reload"));

    public static final StreamCodec<ByteBuf, ReloadMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler(final IPayloadContext context) {
        pressAction(context.player());
    }

    public static void pressAction(Player player) {
        if (player.getVehicle() instanceof VehicleEntity vehicle && vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
            vehicle.modifyGunData(vehicle.getSeatIndex(player), data -> GunEventHandler.INSTANCE.tryStartReload(vehicle.getAmmoSupplier(), data));
            return;
        }

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        GunEventHandler.INSTANCE.tryStartReload(player, GunData.from(stack));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
