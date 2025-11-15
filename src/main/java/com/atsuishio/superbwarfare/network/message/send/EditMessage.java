package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.event.LivingEventHandler.stopGunReloadSound;

public record EditMessage(int type, boolean add, boolean isVehicle) {

    public EditMessage(int type, boolean add) {
        this(type, add, false);
    }

    public static EditMessage decode(FriendlyByteBuf buffer) {
        return new EditMessage(buffer.readInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void encode(EditMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeBoolean(message.add);
        buffer.writeBoolean(message.isVehicle);
    }

    public static void handler(EditMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

            if (message.isVehicle && player.getVehicle() instanceof VehicleEntity vehicle) {
                if (message.type != 5) return;

                vehicle.modifyGunData(vehicle.getSeatIndex(player), data -> {
                    int size = data.getDefault().getAmmoConsumers().size();
                    stopGunReloadSound(player, data);
                    data.changeAmmoConsumer((data.selectedAmmoType.get() + (message.add ? 1 : -1) + size) % size, vehicle.getAmmoSupplier());
                });

                // TODO 替换成合适的音效
                SoundTool.playLocalSound(player, ModSounds.INTO_CANNON.get(), 1f, 1f);
            } else {
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
                        int size = data.getDefault().getAmmoConsumers().size();
                        data.changeAmmoConsumer((data.selectedAmmoType.get() + (message.add ? 1 : -1) + size) % size, player);
                    }
                }
                data.save();
                SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
            }
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


