package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public enum FireModeMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) return;

            changeFireMode(context.getSender());
        });
        context.setPacketHandled(true);
    }

    public static void changeFireMode(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem) {
            var data = GunData.from(stack);

            var selectedFireMode = data.selectedFireMode.get();
            var fireModes = data.get(GunProp.AVAILABLE_FIRE_MODES);

            if (fireModes.size() > 1) {
                data.selectedFireMode.set((selectedFireMode + 1) % fireModes.size());
                playChangeModeSound(player);
                return;
            }

            if (stack.getItem() == ModItems.SENTINEL.get()
                    && !player.isSpectator()
                    && !(player.getCooldowns().isOnCooldown(stack.getItem()))
                    && GunData.from(stack).reload.time() == 0
                    && !GunData.from(stack).charging()) {

                for (var cell : player.getInventory().items) {
                    if (cell.is(ModItems.CELL.get())) {
                        boolean[] flag = {false};
                        cell.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                                iEnergyStorage -> flag[0] = iEnergyStorage.getEnergyStored() >= 0
                        );

                        if (flag[0]) {
                            data.charge.starter.markStart();
                        }
                    }
                }
            }

            if (stack.getItem() == ModItems.JAVELIN.get() && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_ZOOM_OUT.get());
            }
        }
    }

    private static void playChangeModeSound(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SoundTool.playLocalSound(serverPlayer, ModSounds.FIRE_RATE.get());
        }
    }
}
