package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 开火按键按下/松开时的处理
 */
public record FireKeyMessage(int msgType, double power, boolean zoom) implements CustomPacketPayload {
    public static final Type<FireKeyMessage> TYPE = new Type<>(Mod.loc("fire"));

    public static final StreamCodec<ByteBuf, FireKeyMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            FireKeyMessage::msgType,
            ByteBufCodecs.DOUBLE,
            FireKeyMessage::power,
            ByteBufCodecs.BOOL,
            FireKeyMessage::zoom,
            FireKeyMessage::new
    );


    public static void handler(FireKeyMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.msgType, message.power, message.zoom);
    }

    public static void pressAction(Player player, int type, double power, boolean zoom) {
        if (player.isSpectator()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        handleGunBolt(player, stack);

        if (type == 0) {
            // 按下开火
            data.item.onFireKeyPress(data, player, zoom);
        } else if (type == 1) {
            // 松开开火
            data.item.onFireKeyRelease(data, player, power, zoom);
        }

        data.save();
    }

    private static void handleGunBolt(Player player, ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        if (data.defaultActionTime() > 0
                && data.ammo.get() > 0
                && data.bolt.actionTimer.get() == 0
                && !(data.reload.normal() || data.reload.empty())
                && !data.reloading()
                && !data.charging()
        ) {
            if (!player.getCooldowns().isOnCooldown(stack.getItem()) && data.bolt.needed.get()) {
                data.bolt.actionTimer.set(data.defaultActionTime() + 1);
                GunEventHandler.playGunBoltSounds(player);
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
