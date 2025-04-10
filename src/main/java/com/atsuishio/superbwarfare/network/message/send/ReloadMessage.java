package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ReloadMessage(int msgType) implements CustomPacketPayload {
    public static final Type<ReloadMessage> TYPE = new Type<>(Mod.loc("reload"));

    public static final StreamCodec<ByteBuf, ReloadMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            ReloadMessage::msgType,
            ReloadMessage::new
    );

    public static void handler(ReloadMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.msgType);
    }

    public static void pressAction(Player player, int type) {
        if (type != 0) return;
        var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();
        cap.edit = false;
        player.setData(ModAttachments.PLAYER_VARIABLE, cap);
        cap.sync(player);

        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        var data = GunData.from(stack);

        if (!player.isSpectator()
                && !data.charging()
                && !data.reloading()
                && data.reload.time() == 0
                && data.bolt.actionTime() == 0
        ) {
            boolean canSingleReload = gunItem.isIterativeReload(stack);
            boolean canReload = gunItem.isMagazineReload(stack) && !gunItem.isClipReload(stack);
            boolean clipLoad = data.ammo() == 0 && gunItem.isClipReload(stack);

            // 检查备弹
            boolean hasCreativeAmmoBox = player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()));

            if (!hasCreativeAmmoBox) {
                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && cap.shotgunAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && cap.sniperAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_HANDGUN_AMMO) && cap.handgunAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && cap.rifleAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && cap.heavyAmmo == 0) {
                    return;
                } else if (stack.getItem() == ModItems.TASER.get() && data.maxAmmo() == 0) {
                    return;
                } else if (stack.is(ModTags.Items.LAUNCHER) && data.maxAmmo() == 0) {
                    return;
                }
            }

            if (canReload || clipLoad) {
                int magazine = data.magazine();
                var extra = (gunItem.isOpenBolt(stack) && gunItem.hasBulletInBarrel(stack)) ? 1 : 0;
                var maxAmmo = magazine + extra;

                if (data.ammo() < maxAmmo) {
                    data.reload.reloadStarter.markStart();
                }
                return;
            }

            if (canSingleReload && data.ammo() < data.magazine()) {
                data.reload.singleReloadStarter.markStart();
            }
            data.save();
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
