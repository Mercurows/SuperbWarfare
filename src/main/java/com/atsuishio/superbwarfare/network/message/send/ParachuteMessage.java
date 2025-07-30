package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.curio.ParachuteItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

public enum ParachuteMessage {
    INSTANCE;

    public static void handler(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) return;

            CuriosApi.getCuriosInventory(player).ifPresent(
                    c -> c.findFirstCurio(ModItems.PARACHUTE.get()).ifPresent(
                            s -> {
                                var stack = s.stack();
                                if (!player.getCooldowns().isOnCooldown(stack.getItem())) {
                                    if (!stack.getOrCreateTag().getBoolean(ParachuteItem.TAG_OPEN) && player.getDeltaMovement().y < -0.6 && player.fallDistance > 4) {
                                        stack.getOrCreateTag().putBoolean(ParachuteItem.TAG_OPEN, true);
                                        player.getCooldowns().addCooldown(stack.getItem(), 10);
                                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PARACHUTE_OPEN.get(), SoundSource.PLAYERS, 1f, 1);
                                    } else if (stack.getOrCreateTag().getBoolean(ParachuteItem.TAG_OPEN)) {
                                        stack.getOrCreateTag().putBoolean(ParachuteItem.TAG_OPEN, false);
                                        player.getCooldowns().addCooldown(stack.getItem(), 10);
                                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PARACHUTE_CLOSE.get(), SoundSource.PLAYERS, 1f, 1);
                                    }
                                }
                            }
                    )
            );
        });
        context.setPacketHandled(true);
    }
}
