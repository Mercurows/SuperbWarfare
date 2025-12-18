package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 开火按键按下/松开时的处理
 */
public class FireKeyMessage {
    private final int type;
    private final double power;
    private final boolean zoom;

    public FireKeyMessage(int type, double power, boolean zoom) {
        this.type = type;
        this.power = power;
        this.zoom = zoom;
    }

    public static FireKeyMessage decode(FriendlyByteBuf buffer) {
        return new FireKeyMessage(buffer.readInt(), buffer.readDouble(), buffer.readBoolean());
    }

    public static void encode(FireKeyMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeDouble(message.power);
        buffer.writeBoolean(message.zoom);
    }

    public static void handler(FireKeyMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                pressAction(context.getSender(), message.type, message.power, message.zoom);
            }
        });
        context.setPacketHandled(true);
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

        if (data.compute().boltActionTime > 0
                && data.hasEnoughAmmoToShoot(player)
                && data.bolt.actionTimer.get() == 0
                && !data.reloading()
                && !data.charging()
        ) {
            if (!player.getCooldowns().isOnCooldown(stack.getItem()) && data.bolt.needed.get()) {
                data.startBolt();
                GunEventHandler.INSTANCE.playGunBoltSounds(player, data);
            }
        }
    }
}
