package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ReloadType;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum ReloadMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<ReloadMessage> TYPE = new Type<>(Mod.loc("reload"));

    public static final StreamCodec<ByteBuf, ReloadMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler(final IPayloadContext context) {
        pressAction(context.player());
    }

    public static void pressAction(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        var data = GunData.from(stack);
        if (data.useBackpackAmmo() || data.meleeOnly()) return;

        if (!player.isSpectator()
                && !data.charging()
                && !data.reloading()
                && data.reload.time() == 0
                && data.bolt.actionTimer.get() == 0
        ) {
            var reloadTypes = data.reloadTypes();
            boolean canSingleReload = reloadTypes.contains(ReloadType.ITERATIVE);
            boolean canReload = reloadTypes.contains(ReloadType.MAGAZINE) && !reloadTypes.contains(ReloadType.CLIP);
            boolean clipLoad = data.ammo.get() == 0 && reloadTypes.contains(ReloadType.CLIP);

            // 检查备弹
            if (!data.hasBackupAmmo(player)) return;

            if (canReload || clipLoad) {
                int magazine = data.get(GunProp.MAGAZINE);
                var extra = (gunItem.isOpenBolt(stack) && gunItem.hasBulletInBarrel(stack)) ? 1 : 0;
                var maxAmmo = magazine + extra;

                if (data.ammo.get() < maxAmmo) {
                    data.startReload();
                }
                return;
            }

            if (canSingleReload && data.ammo.get() < data.get(GunProp.MAGAZINE)) {
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
