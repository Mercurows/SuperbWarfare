package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FireModeMessage(boolean forward) {

    public static void encode(FireModeMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.forward());
    }

    public static FireModeMessage decode(FriendlyByteBuf buffer) {
        return new FireModeMessage(buffer.readBoolean());
    }

    public static void handler(FireModeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof GunItem)) {
                return;
            }
            var data = GunData.from(stack);

            var selectedFireMode = data.selectedFireMode.get();
            var fireModes = data.get(GunProp.AVAILABLE_FIRE_MODES);

            if (fireModes.size() > 1) {
                int mode = (selectedFireMode + (message.forward() ? -1 : 1) + fireModes.size()) % fireModes.size();
                data.selectedFireMode.set(mode);
                SoundTool.playLocalSound(player, ModSounds.FIRE_RATE.get());
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

            if (stack.getItem() == ModItems.JAVELIN.get()) {
                SoundTool.playLocalSound(player, ModSounds.CANNON_ZOOM_OUT.get());
            }
            data.save();
        });
        context.setPacketHandled(true);
    }
}
