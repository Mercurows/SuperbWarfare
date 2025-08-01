package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChangeAmmoTypeMessage(int index) {

    public static ChangeAmmoTypeMessage decode(FriendlyByteBuf buffer) {
        return new ChangeAmmoTypeMessage(buffer.readInt());
    }

    public static void encode(ChangeAmmoTypeMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.index);
    }

    public static void handler(ChangeAmmoTypeMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) return;

            var data = GunData.from(stack);
            data.withdrawAmmo(player);
            data.selectedAmmoType.set(Mth.clamp(message.index, 0, AttachmentType.values().length - 1));

            // TODO 修改显示
            player.displayClientMessage(Component.literal("selected index: " + message.index), true);
            SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
        });
        ctx.get().setPacketHandled(true);
    }
}
