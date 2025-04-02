package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.perk.PerkHelper;
import com.atsuishio.superbwarfare.tools.*;
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

public record ShootMessage(double spread) implements CustomPacketPayload {

    public static final Type<ShootMessage> TYPE = new Type<>(Mod.loc("shoot"));

    public static final StreamCodec<ByteBuf, ShootMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            ShootMessage::spread,
            ShootMessage::new
    );

    public static void handler(final ShootMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.spread);
    }

    public static void pressAction(Player player, double spared) {
        ItemStack stack = player.getMainHandItem();
        final var tag = NBTTool.getTag(stack);

        if (stack.is(ModTags.Items.NORMAL_GUN)) {
            int projectileAmount = GunsTool.getGunIntTag(tag, "ProjectileAmount", 1);

            if (GunsTool.getGunIntTag(tag, "Ammo") > 0) {
                // 空仓挂机
                if (GunsTool.getGunIntTag(tag, "Ammo") == 1) {
                    GunsTool.setGunBooleanTag(tag, "HoldOpen", true);
                }

                if (stack.is(ModTags.Items.REVOLVER)) {
                    tag.putBoolean("canImmediatelyShoot", false);
                }

                // 判断是否为栓动武器（BoltActionTime > 0），并在开火后给一个需要上膛的状态
                if (GunsTool.getGunIntTag(tag, "BoltActionTime") > 0 && GunsTool.getGunIntTag(tag, "Ammo") > (stack.is(ModTags.Items.REVOLVER) ? 0 : 1)) {
                    GunsTool.setGunBooleanTag(tag, "NeedBoltAction", true);
                }

                GunsTool.setGunIntTag(tag, "Ammo", GunsTool.getGunIntTag(tag, "Ammo") - 1);
                tag.putDouble("empty", 1);

                if (stack.getItem() == ModItems.M_60.get() && GunsTool.getGunIntTag(tag, "Ammo") <= 5) {
                    GunsTool.setGunBooleanTag(tag, "HideBulletChain", true);
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

                var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);

                for (int index0 = 0; index0 < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : projectileAmount); index0++) {
                    GunEventHandler.gunShoot(player, tag, spared);
                }

                GunEventHandler.playGunSounds(player);
                NBTTool.saveTag(stack, tag);
            }
        } else if (stack.is(ModItems.MINIGUN.get())) {
            var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
            if (cap == null) return;

            if (cap.rifleAmmo > 0 || InventoryTool.hasCreativeAmmoBox(player)) {
                tag.putDouble("heat", (tag.getDouble("heat") + 0.1));
                if (tag.getDouble("heat") >= 50.5) {
                    tag.putDouble("overheat", 40);
                    player.getCooldowns().addCooldown(stack.getItem(), 40);
                    if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                        SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
                    }
                }

                var perk = PerkHelper.getPerkByType(tag, Perk.Type.AMMO);
                float pitch = tag.getDouble("heat") <= 40 ? 1 : (float) (1 - 0.025 * Math.abs(40 - tag.getDouble("heat")));

                if (!player.level().isClientSide() && player instanceof ServerPlayer) {
                    float soundRadius = (float) GunsTool.getGunDoubleTag(tag, "SoundRadius");

                    player.playSound(ModSounds.MINIGUN_FIRE_3P.get(), soundRadius * 0.2f, pitch);
                    player.playSound(ModSounds.MINIGUN_FAR.get(), soundRadius * 0.5f, pitch);
                    player.playSound(ModSounds.MINIGUN_VERYFAR.get(), soundRadius, pitch);

                    if (perk == ModPerks.BEAST_BULLET.get()) {
                        player.playSound(ModSounds.HENG.get(), 4f, pitch);
                    }
                }

                GunEventHandler.gunShoot(player, tag, spared);
                if (!InventoryTool.hasCreativeAmmoBox(player)) {
                    cap.rifleAmmo = cap.rifleAmmo - 1;
                    cap.syncPlayerVariables(player);
                }

                NBTTool.saveTag(stack, tag);
            }
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
