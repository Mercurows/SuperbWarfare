package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.RemoteControllableTurret;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

public enum ArtilleryIndicatorFireMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                Player player = context.getSender();

                ItemStack stack = player.getMainHandItem();

                if (player.getMainHandItem().is(ModItems.MONITOR.get()) && player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
                    stack = player.getOffhandItem();
                }

                if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                    ListTag tags = stack.getOrCreateTag().getList(TAG_CANNON, Tag.TAG_COMPOUND);
                    for (int i = 0; i < tags.size(); i++) {
                        var tag = tags.getCompound(i);
                        Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));

                        if (entity instanceof RemoteControllableTurret turret && turret.canRemoteFire()) {
                            Mod.queueServerWork(i % 5 + 1, () -> turret.remoteFire(player));
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
