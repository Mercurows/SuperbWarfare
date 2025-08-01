package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

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
        data.save();
        SoundTool.playLocalSound(player, ModSounds.EDIT.get(), 1f, 1f);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}


