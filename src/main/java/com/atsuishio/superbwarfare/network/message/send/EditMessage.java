package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
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

public record EditMessage(int msgType, boolean add) implements CustomPacketPayload {
    public static final Type<EditMessage> TYPE = new Type<>(Mod.loc("edit"));

    public static final StreamCodec<ByteBuf, EditMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            EditMessage::msgType,
            ByteBufCodecs.BOOL,
            EditMessage::add,
            EditMessage::new
    );

    public static void handler(EditMessage message, final IPayloadContext context) {
        pressAction(context.player(), message);
    }

    public static void pressAction(Player player, EditMessage message) {
        if (player == null) return;

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
                var diff = message.add ? 1 : -1;
                var selectedAmmoType = data.selectedAmmoType.get() + diff;

                if (!player.isCreative()
                        && selectedAmmoType >= 0
                        && selectedAmmoType <= data.get(GunProp.AMMO_CONSUMER).size() - 1
                ) {
                    var currentConsumer = data.selectedAmmoConsumer();
                    var targetConsumer = data.get(GunProp.AMMO_CONSUMER).get(selectedAmmoType);
                    if (currentConsumer == targetConsumer) return;

                    var currentSlot = currentConsumer.ammoSlot;
                    var targetSlot = targetConsumer.ammoSlot;

                    if (currentSlot == null) currentSlot = "Default";
                    if (targetSlot == null) targetSlot = "Default";

                    if (currentSlot.equals(targetSlot)) {
                        data.withdrawAmmo(player);
                    } else {
                        var ammo = data.ammo.get();
                        var virtualAmmo = data.virtualAmmo.get();
                        data.ammoSlot.set(currentSlot, ammo, virtualAmmo);

                        data.ammo.set(data.ammoSlot.getAmmo(targetSlot));
                        data.virtualAmmo.set(data.ammoSlot.getVirtualAmmo(targetSlot));
                        data.ammoSlot.reset(targetSlot);
                    }
                }

                data.changeAmmoConsumer(selectedAmmoType);

                if (player.isCreative()) {
                    data.ammo.set(data.get(GunProp.MAGAZINE));
                }

                data.item.whenNoAmmo(data);

                data.closeHammer.set(false);
            }
        }
        data.save();
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
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


