package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ReloadType;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum ReloadMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                pressAction(context.getSender());
            }
        });
        context.setPacketHandled(true);
    }

    public static void pressAction(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return;

        var data = GunData.from(stack);
        if (data.useBackpackAmmo() || data.meleeOnly()) return;

        if (!player.isSpectator()
                && stack.getItem() instanceof GunItem
                && !GunData.from(stack).charging()
                && GunData.from(stack).reload.time() == 0
                && GunData.from(stack).bolt.actionTimer.get() == 0
                && !GunData.from(stack).reloading()
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
        }
    }
}
