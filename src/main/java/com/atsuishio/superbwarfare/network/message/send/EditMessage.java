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
                    if (message.add) {
                        att = (att + 1) % gunItem.getBarrelCount();
                    } else {
                        att = (att + gunItem.getBarrelCount() - 1) % gunItem.getBarrelCount();
                    }
                    data.attachment.set(AttachmentType.BARREL, att);
                }
                case 1 -> {
                    int att = data.attachment.get(AttachmentType.SCOPE);
                    if (message.add) {
                        att = (att + 1) % gunItem.getScopeCount();
                    } else {
                        att = (att + gunItem.getScopeCount() - 1) % gunItem.getScopeCount();
                    }
                    data.attachment.set(AttachmentType.SCOPE, att);
                }
                case 2 -> {
                    int att = data.attachment.get(AttachmentType.GRIP);
                    if (message.add) {
                        att = (att + 1) % gunItem.getGripCount();
                    } else {
                        att = (att + gunItem.getGripCount() - 1) % gunItem.getGripCount();
                    }
                    data.attachment.set(AttachmentType.GRIP, att);
                }
                case 3 -> {
                    int att = data.attachment.get(AttachmentType.STOCK);
                    if (message.add) {
                        att = (att + 1) % gunItem.getStockCount();
                    } else {
                        att = (att + gunItem.getStockCount() - 1) % gunItem.getStockCount();
                    }
                    data.attachment.set(AttachmentType.STOCK, att);
                }
                case 4 -> {
                    int att = data.attachment.get(AttachmentType.MAGAZINE);
                    if (message.add) {
                        att = (att + 1) % gunItem.getMagazineCount();
                    } else {
                        att = (att + gunItem.getMagazineCount() - 1) % gunItem.getMagazineCount();
                    }
                    data.withdrawAmmo(player);
                    data.attachment.set(AttachmentType.MAGAZINE, att);
                }
                case 5 -> {
                    data.withdrawAmmo(player);
                    var diff = message.add ? 1 : -1;
                    var selectedAmmoType = Mth.clamp(data.selectedAmmoType.get() + diff, 0, data.ammoConsumers.size() - 1);
                    data.selectedAmmoType.set(Mth.clamp(selectedAmmoType, 0, AttachmentType.values().length - 1));

                    // TODO 修改显示
                    player.displayClientMessage(Component.literal("selected index: " + selectedAmmoType), true);
                    SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
                }
            }
            SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
        });
        context.setPacketHandled(true);
    }
}


