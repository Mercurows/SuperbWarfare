package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.stream.StreamSupport;

public record RadarSetTargetMessage(UUID target) implements CustomPacketPayload {
    public static final Type<RadarSetTargetMessage> TYPE = new Type<>(Mod.loc("radar_set_target"));

    public static final StreamCodec<ByteBuf, RadarSetTargetMessage> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            RadarSetTargetMessage::target,
            RadarSetTargetMessage::new
    );

    public static void handler(RadarSetTargetMessage message, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();

        AbstractContainerMenu menu = player.containerMenu;
        if (menu instanceof FuMO25Menu fuMO25Menu) {
            if (!player.containerMenu.stillValid(player)) {
                return;
            }
            fuMO25Menu.getSelfPos().ifPresent(pos -> {
                var entities = StreamSupport.stream(EntityFindUtil.getEntities(player.level()).getAll().spliterator(), false)
                        .filter(e -> (e instanceof AutoAimableEntity autoAimableEntity && autoAimableEntity.getOwner() == player && autoAimableEntity.distanceTo(player) <= 24))
                        .toList();
                entities.forEach(e -> ((AutoAimableEntity) e).setTargetUUID(message.target.toString()));
            });
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
