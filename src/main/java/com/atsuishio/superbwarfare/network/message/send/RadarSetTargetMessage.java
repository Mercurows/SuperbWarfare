package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
            fuMO25Menu.getSelfPos().ifPresent(pos -> StreamSupport.stream(EntityFindUtil.getEntities(player.level()).getAll().spliterator(), false)
                    .filter(e -> (e instanceof LaserTowerEntity towerEntity && towerEntity.getOwner() == player && towerEntity.distanceTo(player) <= 16)
                            || (e instanceof Hpj11Entity hpj11Entity && hpj11Entity.getOwner() == player && hpj11Entity.distanceTo(player) <= 16))
                    .forEach(e -> setTarget(e, message.target.toString())));
        }
    }

    public static void setTarget(Entity e, String uuid) {
        if (e instanceof LaserTowerEntity laserTower) {
            laserTower.getEntityData().set(LaserTowerEntity.TARGET_UUID, uuid);
        } else if (e instanceof Hpj11Entity hpj11Entity) {
            hpj11Entity.getEntityData().set(Hpj11Entity.TARGET_UUID, uuid);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
