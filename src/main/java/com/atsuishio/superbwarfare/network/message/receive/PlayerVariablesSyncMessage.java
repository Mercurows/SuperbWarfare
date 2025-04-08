package com.atsuishio.superbwarfare.network.message.receive;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModCapabilities;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PlayerVariablesSyncMessage(int target, CompoundTag data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlayerVariablesSyncMessage> TYPE = new CustomPacketPayload.Type<>(Mod.loc("player_variable_sync"));

    public static final StreamCodec<ByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PlayerVariablesSyncMessage::target,
            ByteBufCodecs.COMPOUND_TAG,
            PlayerVariablesSyncMessage::data,
            PlayerVariablesSyncMessage::new
    );


    public static void handler(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
        var data = new PlayerVariable().readFromNBT(message.data());

        if (Minecraft.getInstance().player == null) return;
        var entity = Minecraft.getInstance().player.level().getEntity(message.target());
        if (entity == null) return;

        var variables = entity.getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        if (variables == null) return;

        variables.zoom = data.zoom;
        variables.holdFire = data.holdFire;
        variables.rifleAmmo = data.rifleAmmo;
        variables.handgunAmmo = data.handgunAmmo;
        variables.shotgunAmmo = data.shotgunAmmo;
        variables.sniperAmmo = data.sniperAmmo;
        variables.heavyAmmo = data.heavyAmmo;
        variables.bowPullHold = data.bowPullHold;
        variables.bowPull = data.bowPull;
        variables.playerDoubleJump = data.playerDoubleJump;
        variables.tacticalSprint = data.tacticalSprint;
        variables.tacticalSprintTime = data.tacticalSprintTime;
        variables.tacticalSprintExhaustion = data.tacticalSprintExhaustion;
        variables.breath = data.breath;
        variables.breathTime = data.breathTime;
        variables.breathExhaustion = data.breathExhaustion;
        variables.edit = data.edit;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}