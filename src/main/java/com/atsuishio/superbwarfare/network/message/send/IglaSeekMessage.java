package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class IglaSeekMessage {

    private final int type;

    public IglaSeekMessage(int type) {
        this.type = type;
    }

    public static IglaSeekMessage decode(FriendlyByteBuf buffer) {
        return new IglaSeekMessage(buffer.readInt());
    }

    public static void encode(IglaSeekMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
    }

    public static void handler(IglaSeekMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (message.type == 0) {
                if (player.getMainHandItem().getItem() == ModItems.IGLA_9K38.get()) {
                    var handItem = player.getMainHandItem();
                    var tag = handItem.getOrCreateTag();
                    GunData data = GunData.from(handItem);

                    Entity seekingEntity = SeekTool.seekEntity(player, player.level(), 512, 20);
                    Entity targetEntity = EntityFindUtil.findEntity(player.level(), tag.getString("TargetEntity"));

                    if (seekingEntity != null) {
                        if (data.hasEnoughAmmoToShoot(player)) {
                            tag.putString("TargetEntity", seekingEntity.getStringUUID());
                            tag.putBoolean("Seeking", true);
                            if (!tag.getBoolean("Seeking")) {
                                tag.putInt("SeekTime", 0);
                            }

                            if (seekingEntity != targetEntity) {
                                tag.putBoolean("Seeking", false);
                                tag.putInt("SeekTime", 0);
                                var clientboundstopsoundpacket = new ClientboundStopSoundPacket(new ResourceLocation(Mod.MODID, "igla_9k38_lock"), SoundSource.PLAYERS);
                                player.connection.send(clientboundstopsoundpacket);
                            }
                        }

                    } else {
                        tag.putString("TargetEntity", "none");
                    }
                }
            }

            if (message.type == 1) {
                if (player.getMainHandItem().getItem() == ModItems.IGLA_9K38.get()) {
                    var handItem = player.getMainHandItem();
                    var tag = handItem.getOrCreateTag();
                    tag.putBoolean("Seeking", false);
                    tag.putInt("SeekTime", 0);
                    tag.putString("TargetEntity", "none");
                    var clientboundstopsoundpacket = new ClientboundStopSoundPacket(new ResourceLocation(Mod.MODID, "igla_9k38_lock"), SoundSource.PLAYERS);
                    player.connection.send(clientboundstopsoundpacket);
                }
            }
        });
        context.setPacketHandled(true);
    }

}
