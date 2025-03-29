package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record AimVillagerMessage(int villagerId) implements CustomPacketPayload {
    public static final Type<AimVillagerMessage> TYPE = new Type<>(Mod.loc("aim_villager"));

    public static final StreamCodec<ByteBuf, AimVillagerMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            AimVillagerMessage::villagerId,
            AimVillagerMessage::new
    );

    public static void handler(AimVillagerMessage message, final IPayloadContext context) {
        var sender = context.player();

        Entity entity = sender.level().getEntity(message.villagerId);
        if (entity instanceof AbstractVillager abstractVillager) {
            if (entity instanceof Villager villager) {
                villager.getGossips().add(sender.getUUID(), GossipType.MINOR_NEGATIVE, 10);
            }
            abstractVillager.getBrain().setActiveActivityIfPossible(Activity.PANIC);
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
