package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.common.ammo.MortarShell;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.entity.vehicle.MortarEntity.FIRE_TIME;
import static com.atsuishio.superbwarfare.item.ArtilleryIndicator.TAG_MORTARS;

public class ArtilleryIndicatorFireMessage {

    private final int type;

    public ArtilleryIndicatorFireMessage(int type) {
        this.type = type;
    }

    public static ArtilleryIndicatorFireMessage decode(FriendlyByteBuf buffer) {
        return new ArtilleryIndicatorFireMessage(buffer.readInt());
    }

    public static void encode(ArtilleryIndicatorFireMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
    }

    public static void handler(ArtilleryIndicatorFireMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                Player player = context.getSender();

                ItemStack stack = player.getMainHandItem();

                if (stack.is(ModItems.ARTILLERY_INDICATOR.get())) {
                    ListTag tags = stack.getOrCreateTag().getList(TAG_MORTARS, Tag.TAG_COMPOUND);
                    for (int i = 0; i < tags.size(); i++) {
                        var tag = tags.getCompound(i);
                        Entity entity = EntityFindUtil.findEntity(player.level(), tag.getString("UUID"));
                        if (entity instanceof MortarEntity mortarEntity) {
                            if (mortarEntity.stack.getItem() instanceof MortarShell && mortarEntity.getEntityData().get(FIRE_TIME) == 0) {
                                int randomNumber = (int) (Math.random() * 5) + 1;
                                Mod.queueServerWork(randomNumber, () -> {
                                    mortarEntity.fire(player);
                                });
                            }
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
