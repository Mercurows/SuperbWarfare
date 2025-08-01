package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

public class EditMessage {

    private final int type;
    private final boolean add;

    public EditMessage(int type, boolean add) {
        this.type = type;
        this.add = add;
    }

    public static EditMessage decode(FriendlyByteBuf buffer) {
        return new EditMessage(buffer.readInt(), buffer.readBoolean());
    }

    public static void encode(EditMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeBoolean(message.add);
    }

    public static void handler(EditMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem gunItem)) return;
            var data = GunData.from(stack);

            switch (message.type) {
                case 0 -> {
                    int att = data.attachment.get(AttachmentType.BARREL);
                    att = setAttachment(gunItem.getValidBarrels(), att, message.add);
                    data.attachment.set(AttachmentType.BARREL, att);
                }
                case 1 -> {
                    int att = data.attachment.get(AttachmentType.SCOPE);
                    att = setAttachment(gunItem.getValidScopes(), att, message.add);
                    data.attachment.set(AttachmentType.SCOPE, att);
                }
                case 2 -> {
                    int att = data.attachment.get(AttachmentType.GRIP);
                    att = setAttachment(gunItem.getValidGrips(), att, message.add);
                    data.attachment.set(AttachmentType.GRIP, att);
                }
                case 3 -> {
                    int att = data.attachment.get(AttachmentType.STOCK);
                    att = setAttachment(gunItem.getValidStocks(), att, message.add);
                    data.attachment.set(AttachmentType.STOCK, att);
                }
                case 4 -> {
                    int att = data.attachment.get(AttachmentType.MAGAZINE);
                    att = setAttachment(gunItem.getValidMagazines(), att, message.add);
                    data.withdrawAmmo(player);
                    data.attachment.set(AttachmentType.MAGAZINE, att);
                }
                case 5 -> {
                    data.withdrawAmmo(player);
                    var diff = message.add ? 1 : -1;
                    var selectedAmmoType = data.selectedAmmoType.get() + diff;
                    data.changeAmmoConsumer(selectedAmmoType);

                    // TODO 修改显示
                    player.displayClientMessage(Component.literal("selected index: " + selectedAmmoType), true);
                    SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
                }
            }
            SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
        });
        context.setPacketHandled(true);
    }

    private static int setAttachment(int[] arr, int value, boolean add) {
        if (arr.length == 0) return 0;
        int[] sorted = Arrays.copyOf(arr, arr.length);
        Arrays.sort(sorted);
        int index = Arrays.binarySearch(sorted, value);
        if (index < 0) index = -index - 1;
        if (add) index = (index + 1) % arr.length;
        else index = (index + arr.length - 1) % arr.length;
        return sorted[index];
    }
}


