package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.ArtilleryIndicator;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_CANNON;

public enum ArtilleryIndicatorFireMessage implements CustomPacketPayload {
    INSTANCE;
    public static final Type<ArtilleryIndicatorFireMessage> TYPE = new Type<>(Mod.loc("artillery_indicator_fire"));

    public static final StreamCodec<ByteBuf, ArtilleryIndicatorFireMessage> STREAM_CODEC = StreamCodec.unit(ArtilleryIndicatorFireMessage.INSTANCE);

    public static void handler(final IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();

        if (player.getMainHandItem().is(ModItems.MONITOR.get()) && player.getOffhandItem().is(ModItems.ARTILLERY_INDICATOR.get())) {
            stack = player.getOffhandItem();
        }

        if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
            var mainTag = NBTTool.getTag(stack);
            ListTag tags = mainTag.getList(TAG_CANNON, Tag.TAG_COMPOUND);
            if (tags.isEmpty()) {
                mainTag.remove(ArtilleryIndicator.TAG_TYPE);
                return;
            }

            for (int i = 0; i < tags.size(); i++) {
                var tag = tags.getCompound(i);
                Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));

                if (entity instanceof ArtilleryEntity artilleryEntity) {
                    var gunData = artilleryEntity.getGunData(0);
                    if (gunData != null && gunData.ammo.get() > 0) {
                        Mod.queueServerWork(i % 5 + 1, () -> {
                            artilleryEntity.vehicleShoot(player, "Main");
                            artilleryEntity.resetTarget("Main");
                        });
                    }
                }
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
