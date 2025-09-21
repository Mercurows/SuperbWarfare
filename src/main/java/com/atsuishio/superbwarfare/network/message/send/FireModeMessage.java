package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public enum FireModeMessage implements CustomPacketPayload {
    INSTANCE;

    public static final Type<FireModeMessage> TYPE = new Type<>(Mod.loc("fire_mode"));

    public static final StreamCodec<ByteBuf, FireModeMessage> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void handler(final IPayloadContext context) {
        changeFireMode(context.player());
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
                data.save();
                return;
            }

            if (stack.getItem() == ModItems.SENTINEL.get()
                    && !player.isSpectator()
                    && !(player.getCooldowns().isOnCooldown(stack.getItem()))
                    && data.reload.time() == 0
                    && !data.charging()
            ) {
                for (var cell : player.getInventory().items) {
                    if (cell.is(ModItems.CELL.get())) {
                        var cap = cell.getCapability(Capabilities.EnergyStorage.ITEM);
                        if (cap != null && cap.getEnergyStored() > 0) {
                            data.charge.starter.markStart();
                        }
                    }
                }
            }

            if (stack.getItem() == ModItems.JAVELIN.get() && player instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.CANNON_ZOOM_OUT.get());
            }
            data.save();
        }
    }

    private static void playChangeModeSound(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SoundTool.playLocalSound(serverPlayer, ModSounds.FIRE_RATE.get());
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
