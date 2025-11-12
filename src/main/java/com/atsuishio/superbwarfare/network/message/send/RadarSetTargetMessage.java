package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
                            .filter(e -> (e instanceof Hpj11Entity hpj11Entity && hpj11Entity.getOwner() == player && hpj11Entity.distanceTo(player) <= 24))
                            .toList();
                    entities.forEach(e -> setTarget(e, message.targetUUID.toString()));
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void setTarget(Entity e, String uuid) {
        if (e instanceof LaserTowerEntity laserTower) {
            laserTower.getEntityData().set(LaserTowerEntity.TARGET_UUID, uuid);
        } else if (e instanceof Hpj11Entity hpj11Entity) {
            hpj11Entity.getEntityData().set(Hpj11Entity.TARGET_UUID, uuid);
        } else if (e instanceof WaveforceTowerEntity waveforceTowerEntity) {
            waveforceTowerEntity.getEntityData().set(WaveforceTowerEntity.TARGET_UUID, uuid);
        }
    }
}
