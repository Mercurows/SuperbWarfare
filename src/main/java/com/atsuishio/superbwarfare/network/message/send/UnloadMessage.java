package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum UnloadMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;
            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) return;
            GunData data = GunData.from(stack);
            data.withdrawAmmo(player);
            data.save();
            SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
        });
        context.setPacketHandled(true);
    }
}
