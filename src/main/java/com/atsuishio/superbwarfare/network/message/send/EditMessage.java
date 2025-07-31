package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EditMessage {

    private final int type;

    public EditMessage(int type) {
        this.type = type;
    }

    public static EditMessage decode(FriendlyByteBuf buffer) {
        return new EditMessage(buffer.readInt());
    }

    public static void encode(EditMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
    }

    public static void handler(EditMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> pressAction(context.getSender(), message.type));
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player, int type) {
        if (player == null) return;

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);

        switch (type) {
            case 0 -> {
                int att = data.attachment.get(AttachmentType.SCOPE);
                att++;
                att %= 4;
                data.attachment.set(AttachmentType.SCOPE, att);
            }
            case 1 -> {
                int att = data.attachment.get(AttachmentType.BARREL);
                att++;
                att %= 3;
                data.attachment.set(AttachmentType.BARREL, att);
            }
            case 2 -> {
                int att = data.attachment.get(AttachmentType.MAGAZINE);
                att++;
                att %= 3;
                data.withdrawAmmo(player);
                data.attachment.set(AttachmentType.MAGAZINE, att);
            }
            case 3 -> {
                int att = data.attachment.get(AttachmentType.STOCK);
                att++;
                att %= 3;
                data.attachment.set(AttachmentType.STOCK, att);
            }
            case 4 -> {
                int att = data.attachment.get(AttachmentType.GRIP);
                att++;
                att %= 4;
                data.attachment.set(AttachmentType.GRIP, att);
            }
        }
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
    }
}


