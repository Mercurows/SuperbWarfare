package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
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
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (cap != null) {
            cap.edit = false;
            cap.syncPlayerVariables(player);
        }

        ItemStack stack = player.getMainHandItem();
        var tag = NBTTool.getTag(stack);
        var data = tag.getCompound("GunData");

        if (!player.isSpectator()
                && stack.getItem() instanceof GunItem gunItem
                && !GunsTool.getGunBooleanTag(tag, "Charging")
                && GunsTool.getGunIntTag(tag, "ReloadTime") == 0
                && GunsTool.getGunIntTag(tag, "BoltActionTick") == 0
                && !GunsTool.getGunBooleanTag(tag, "Reloading")
        ) {
            boolean canSingleReload = gunItem.isIterativeReload(stack);
            boolean canReload = gunItem.isMagazineReload(stack) && !gunItem.isClipReload(stack);
            boolean clipLoad = GunsTool.getGunIntTag(tag, "Ammo", 0) == 0 && gunItem.isClipReload(stack);

            // 检查备弹
            boolean hasCreativeAmmoBox = player.getInventory().hasAnyMatching(item -> item.is(ModItems.CREATIVE_AMMO_BOX.get()));

            if (!hasCreativeAmmoBox && cap != null) {
                if (stack.is(ModTags.Items.USE_SHOTGUN_AMMO) && cap.shotgunAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_SNIPER_AMMO) && cap.sniperAmmo == 0) {
                    return;
                } else if ((stack.is(ModTags.Items.USE_HANDGUN_AMMO) || stack.is(ModTags.Items.SMG)) && cap.handgunAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_RIFLE_AMMO) && cap.rifleAmmo == 0) {
                    return;
                } else if (stack.is(ModTags.Items.USE_HEAVY_AMMO) && cap.heavyAmmo == 0) {
                    return;
                } else if (stack.getItem() == ModItems.TASER.get() && GunsTool.getGunIntTag(tag, "MaxAmmo") == 0) {
                    return;
                } else if (stack.is(ModTags.Items.LAUNCHER) && GunsTool.getGunIntTag(tag, "MaxAmmo") == 0) {
                    return;
                }
            }

            if (canReload || clipLoad) {
                int magazine = GunsTool.getGunIntTag(tag, "Magazine", 0);
                int ammo = GunsTool.getGunIntTag(tag, "Ammo", 0);
                int customMagazine = GunsTool.getGunIntTag(tag, "CustomMagazine", 0);

                if (gunItem.isOpenBolt(stack)) {
                    if (gunItem.hasBulletInBarrel(stack)) {
                        if (ammo < magazine + customMagazine + 1) {
                            data.putBoolean("StartReload", true);
                        }
                    } else if (ammo < magazine + customMagazine) {
                        data.putBoolean("StartReload", true);
                    }
                } else if (ammo < magazine + customMagazine) {
                    data.putBoolean("StartReload", true);
                }
                NBTTool.saveTag(stack, tag);
                return;
            }

            if (canSingleReload
                    && GunsTool.getGunIntTag(tag, "Ammo", 0)
                    < GunsTool.getGunIntTag(tag, "Magazine", 0)
                    + GunsTool.getGunIntTag(tag, "CustomMagazine", 0)) {

                data.putBoolean("start_single_reload", true);
            }
            NBTTool.saveTag(stack, tag);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
