package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

public class RadarSetTargetMessage {

    private final UUID targetUUID;

    public RadarSetTargetMessage(UUID targetUUID) {
        this.targetUUID = targetUUID;
    }

    public static void encode(RadarSetTargetMessage message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.targetUUID);
    }

    public static RadarSetTargetMessage decode(FriendlyByteBuf buffer) {
        return new RadarSetTargetMessage(buffer.readUUID());
    }

    public static void handler(RadarSetTargetMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            AbstractContainerMenu menu = player.containerMenu;
            if (menu instanceof FuMO25Menu fuMO25Menu) {
                if (!player.containerMenu.stillValid(player)) {
                    return;
                }
                fuMO25Menu.getSelfPos().ifPresent(pos -> {
                    var entities = StreamSupport.stream(EntityFindUtil.getEntities(player.level()).getAll().spliterator(), false)
                            .filter(e -> (e instanceof AutoAimableEntity autoAimableEntity && autoAimableEntity.getOwner() == player && autoAimableEntity.distanceTo(player) <= 24))
                            .toList();
                    entities.forEach(e -> ((AutoAimableEntity) e).setTargetUUID(message.targetUUID.toString()));
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
