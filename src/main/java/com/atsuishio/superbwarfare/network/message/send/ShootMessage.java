package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ShootMessage(double spread, boolean zoom) implements CustomPacketPayload {

    public static final Type<ShootMessage> TYPE = new Type<>(Mod.loc("shoot"));

    public static final StreamCodec<ByteBuf, ShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShootMessage::spread,
            ByteBufCodecs.BOOL,
            ShootMessage::zoom,
            ShootMessage::new
    );

    public static void handler(final ShootMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.spread, message.zoom);
    }

    public static void pressAction(Player player, double spared, boolean zoom) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;
        var data = GunData.from(stack);
        var tag = data.tag();

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            int projectileAmount = data.projectileAmount();

            if (data.ammo.get() > 0) {
                // 空仓挂机
                if (data.ammo.get() == 1) {
                    data.holdOpen.set(true);
                }

                // TODO 替换左轮判断方式
                if (stack.is(ModTags.Items.REVOLVER)) {
                    data.canImmediatelyShoot.set(true);
                }

                // TODO 替换左轮判断方式
                // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
                if (data.defaultActionTime() > 0 && data.ammo.get() > (stack.is(ModTags.Items.REVOLVER) ? 0 : 1)) {
                    data.bolt.needed.set(true);
                }

                // 调用事件
                data.item.onShoot(data, player);

                data.ammo.set(data.ammo.get() - 1);
                data.isEmpty.set(true);

                var perk = data.perk.get(Perk.Type.AMMO);

                for (int index0 = 0; index0 < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : projectileAmount); index0++) {
                    GunEventHandler.gunShoot(player, data, spared, zoom);
                }

                GunEventHandler.playGunSounds(player, zoom);
            }
        } else if (stack.is(ModItems.MINIGUN.get())) {
            if (data.hasAmmo(player)) {
                // TODO 替换为通用过热处理
                tag.putDouble("heat", (tag.getDouble("heat") + 0.1));
                if (tag.getDouble("heat") >= 50.5) {
                    tag.putDouble("overheat", 40);
                    player.getCooldowns().addCooldown(stack.getItem(), 40);
                    if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
                    }
                }

                float pitch = tag.getDouble("heat") <= 40 ? 1 : (float) (1 - 0.025 * Math.abs(40 - tag.getDouble("heat")));
                var perk = data.perk.get(Perk.Type.AMMO);

                if (!player.level().isClientSide() && player instanceof ServerPlayer) {
                    float soundRadius = (float) data.soundRadius();

                    player.playSound(ModSounds.MINIGUN_FIRE_3P.get(), soundRadius * 0.2f, pitch);
                    player.playSound(ModSounds.MINIGUN_FAR.get(), soundRadius * 0.5f, pitch);
                    player.playSound(ModSounds.MINIGUN_VERYFAR.get(), soundRadius, pitch);

                    if (perk == ModPerks.BEAST_BULLET.get()) {
                        player.playSound(ModSounds.HENG.get(), 4f, pitch);
                    }
                }

                GunEventHandler.gunShoot(player, data, spared, false);
                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    data.consumeAmmo(player, 1);
                }
            }
        }
        data.save();
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
