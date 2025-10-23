package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ZoomMessage(int msgType) implements CustomPacketPayload {
    public static final Type<ZoomMessage> TYPE = new Type<>(Mod.loc("zoom"));

    public static final StreamCodec<ByteBuf, ZoomMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ZoomMessage::msgType,
            ZoomMessage::new
    );

    public static void handler(ZoomMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        var vehicle = player.getVehicle();
        // 缩放音效播放条件: 载具是武器载具，且该位置有可用武器

        if (message.msgType == 0) {

            if (player.isPassenger()
                    && vehicle instanceof WeaponVehicleEntity weaponEntity
                    && vehicle instanceof VehicleEntity vehicleEntity
                    && weaponEntity.hasWeapon(vehicleEntity.getSeatIndex(player))
                    && vehicleEntity.banHand(player)
            ) {
                SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_IN.get(), 2, 1);
            }

        }

        if (message.msgType == 1) {
            if (player.isPassenger()
                    && vehicle instanceof WeaponVehicleEntity weaponEntity
                    && vehicle instanceof VehicleEntity vehicleEntity
                    && weaponEntity.hasWeapon(vehicleEntity.getSeatIndex(player))
                    && vehicleEntity.banHand(player)
            ) {
                SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_OUT.get(), 2, 1);
            }

            ItemStack stack = player.getMainHandItem();
            String origin = stack.getItem().getDescriptionId();
            String name = origin.substring(origin.lastIndexOf(".") + 1);
            var clientboundstopsoundpacket = new ClientboundStopSoundPacket(Mod.loc(name + "_lock"), SoundSource.PLAYERS);
            player.connection.send(clientboundstopsoundpacket);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
