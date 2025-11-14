package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record EditMessage(int msgType, boolean add, boolean isVehicle) implements CustomPacketPayload {
    public static final Type<EditMessage> TYPE = new Type<>(Mod.loc("edit"));

    public static final StreamCodec<ByteBuf, EditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            EditMessage::msgType,
            ByteBufCodecs.BOOL,
            EditMessage::add,
            ByteBufCodecs.BOOL,
            EditMessage::isVehicle,
            EditMessage::new
    );

    public static void handler(EditMessage message, final IPayloadContext context) {
        pressAction(context.player(), message);
    }

    public static void pressAction(Player player, EditMessage message) {
        if (player == null) return;
        if (message.isVehicle && player.getVehicle() instanceof VehicleEntity vehicle) {
            var data = vehicle.getGunData(player, vehicle.getSelectedWeapon(vehicle.getSeatIndex(player)));
            if (data == null) return;
            if (message.msgType != 5) return;

            int size = data.getDefault().getAmmoConsumers().size();
            data.changeAmmoConsumer((data.selectedAmmoType.get() + (message.add ? 1 : -1) + size) % size, player);
            data.save();

            // TODO 替换成合适的音效
            SoundTool.playLocalSound(player, ModSounds.INTO_CANNON.get(), 1f, 1f);
        } else {

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem gunItem)) return;
            var data = GunData.from(stack);

            switch (message.msgType) {
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
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
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


