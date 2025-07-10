package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.common.ammo.CannonShellItem;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity.COOL_DOWN;
import static com.atsuishio.superbwarfare.entity.vehicle.MortarEntity.FIRE_TIME;
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
                        if (entity instanceof MortarEntity mortarEntity) {
                            if (mortarEntity.stack.getItem() instanceof MortarShell && mortarEntity.getEntityData().get(FIRE_TIME) == 0) {
                                int randomNumber = (int) (Math.random() * 5) + 1;
                                Mod.queueServerWork(randomNumber, () -> mortarEntity.fire(player));
                            }
                        }
                        if (entity instanceof Mk42Entity mk42Entity) {
                            if (mk42Entity.stack.getItem() instanceof CannonShellItem && mk42Entity.getEntityData().get(COOL_DOWN) == 0) {
                                int randomNumber = (int) (Math.random() * 5) + 1;
                                var weaponType = mk42Entity.stack.is(ModItems.AP_5_INCHES.get()) ? 0 : 1;
                                mk42Entity.setWeaponIndex(0, weaponType);
                                Mod.queueServerWork(randomNumber, () -> mk42Entity.vehicleShoot(player, 0));
                            }
                        }
                        if (entity instanceof Mle1934Entity mle1934Entity) {
                            if (mle1934Entity.stack.getItem() instanceof CannonShellItem && mle1934Entity.getEntityData().get(COOL_DOWN) == 0) {
                                int randomNumber = (int) (Math.random() * 5) + 1;
                                var weaponType = mle1934Entity.stack.is(ModItems.AP_5_INCHES.get()) ? 0 : 1;
                                mle1934Entity.setWeaponIndex(0, weaponType);
                                Mod.queueServerWork(randomNumber, () -> mle1934Entity.vehicleShoot(player, 0));
                            }
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
