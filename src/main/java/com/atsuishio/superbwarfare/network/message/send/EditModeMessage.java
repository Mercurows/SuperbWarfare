package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModCapabilities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record EditModeMessage(int msgType) implements CustomPacketPayload {
    public static final Type<EditModeMessage> TYPE = new Type<>(Mod.loc("edit_mode"));

    public static final StreamCodec<ByteBuf, EditModeMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            EditModeMessage::msgType,
            EditModeMessage::new
    );

    public static void handler(EditModeMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.msgType);
    }

    public static void pressAction(Player player, int type) {
        if (player == null) return;
        if (type != 0) return;

        ItemStack mainHandItem = player.getMainHandItem();
        if (!(mainHandItem.getItem() instanceof GunItem gunItem)) return;
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);

        if (gunItem.isCustomizable(mainHandItem) && cap != null) {
            if (!cap.edit) {
                SoundTool.playLocalSound(player, ModSounds.EDIT_MODE.get(), 1f, 1f);
            }
            cap.edit = !cap.edit;
            cap.syncPlayerVariables(player);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
