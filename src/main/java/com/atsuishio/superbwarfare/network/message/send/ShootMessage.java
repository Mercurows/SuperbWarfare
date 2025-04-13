package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
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
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);
        var tag = data.tag();

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            int projectileAmount = data.projectileAmount();

            if (data.ammo.get() > 0) {
                // 空仓挂机
                if (data.ammo.get() == 1) {
                    data.holdOpen.set(true);
                }

                if (stack.is(ModTags.Items.REVOLVER)) {
                    data.canImmediatelyShoot.set(true);
                }

                // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
                if (data.defaultActionTime() > 0 && data.ammo.get() > (stack.is(ModTags.Items.REVOLVER) ? 0 : 1)) {
                    data.bolt.needed.set(true);
                }

                data.ammo.set(data.ammo.get() - 1);
                data.isEmpty.set(true);

                if (stack.getItem() == ModItems.M_60.get() && data.ammo.get() <= 5) {
                    data.hideBulletChain.set(true);
                }

                if (stack.getItem() == ModItems.HOMEMADE_SHOTGUN.get()) {
                    stack.hurtAndBreak(1, (ServerLevel) player.level(), player, p -> {
                    });
                    if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
                        ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x, player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                                player.getZ() + 1.8 * player.getLookAngle().z, 30, 0.4, 0.4, 0.4, 0.005, true, serverPlayer);
                    }
                }

                if (stack.getItem() == ModItems.SENTINEL.get()) {
                    var cap = stack.getCapability(Capabilities.EnergyStorage.ITEM);
                    if (cap != null) {
                        cap.extractEnergy(3000, false);
                    }
                }

                var perk = data.perk.get(Perk.Type.AMMO);

                for (int index0 = 0; index0 < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : projectileAmount); index0++) {
                    GunEventHandler.gunShoot(player, data, spared, zoom);
                }

                GunEventHandler.playGunSounds(player);
            }
        } else if (stack.is(ModItems.MINIGUN.get())) {
            var cap = player.getData(ModAttachments.PLAYER_VARIABLE).watch();

            if (cap.rifleAmmo > 0 || InventoryTool.hasCreativeAmmoBox(player)) {
                tag.putDouble("heat", (tag.getDouble("heat") + 0.1));
                if (tag.getDouble("heat") >= 50.5) {
                    tag.putDouble("overheat", 40);
                    player.getCooldowns().addCooldown(stack.getItem(), 40);
                    if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
                    }
                }

                var perk = data.perk.get(Perk.Type.AMMO);
                float pitch = tag.getDouble("heat") <= 40 ? 1 : (float) (1 - 0.025 * Math.abs(40 - tag.getDouble("heat")));

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
                    cap.rifleAmmo = cap.rifleAmmo - 1;
                    player.setData(ModAttachments.PLAYER_VARIABLE, cap);
                    cap.sync(player);
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
