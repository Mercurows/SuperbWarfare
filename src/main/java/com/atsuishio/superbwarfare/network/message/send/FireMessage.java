package com.atsuishio.superbwarfare.network.message.send;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.SpecialFireWeapon;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public record FireMessage(int msgType) implements CustomPacketPayload {
    public static final Type<FireMessage> TYPE = new Type<>(Mod.loc("fire"));

    public static final StreamCodec<ByteBuf, FireMessage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            FireMessage::msgType,
            FireMessage::new
    );


    public static void handler(FireMessage message, final IPayloadContext context) {
        pressAction(context.player(), message.msgType);
    }

    public static void pressAction(Player player, int type) {
        if (player.isSpectator()) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);
        final var tag = data.tag();

        handleGunBolt(player, stack);

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        if (type == 0) {
            if (tag.getDouble("PrepareTime") == 0 && data.reloading() && data.ammo() > 0) {
                tag.putBoolean("ForceStop", true);
            }

            if (cap != null) {
                cap.edit = false;
            }

            // 按下开火
            if (!(stack.getItem() instanceof SpecialFireWeapon specialFireWeapon)) {
                if (cap != null) cap.syncPlayerVariables(player);
                return;
            }
            specialFireWeapon.fireOnPress(player, data);

            if (cap != null) {
                cap.holdFire = true;
                cap.syncPlayerVariables(player);
            }
        } else if (type == 1) {
            if (cap != null) {
                cap.bowPullHold = false;
                cap.holdFire = false;
                cap.syncPlayerVariables(player);
            }

            // 松开开火
            if (stack.getItem() instanceof SpecialFireWeapon specialFireWeapon) {
                specialFireWeapon.fireOnRelease(player, data);
            }
        }
        data.save();
    }

    private static void handleGunBolt(Player player, ItemStack stack) {
        if (!stack.is(ModTags.Items.GUN)) return;
        var data = GunData.from(stack);

        if (data.bolt.defaultActionTime() > 0
                && data.ammo() > (stack.is(ModTags.Items.REVOLVER) ? -1 : 0)
                && data.bolt.actionTime() == 0
                && !(data.reload.normal()
                || data.reload.empty())
                && !data.reloading()
                && !data.charging()
        ) {
            if (!player.getCooldowns().isOnCooldown(stack.getItem()) && data.bolt.needed()) {
                data.bolt.setActionTime(data.bolt.defaultActionTime() + 1);
                GunEventHandler.playGunBoltSounds(player);
            }
        }
    }

    public static double perkSpeed(Perk perk) {
        if (perk instanceof AmmoPerk ammoPerk) {
            return ammoPerk.speedRate;
        }
        return 1;
    }

    public static void spawnBullet(Player player, final CompoundTag tag) {
        ItemStack stack = player.getMainHandItem();
        if (player.level().isClientSide()) return;
        var data = GunData.from(stack);

        var perk = data.perk.get(Perk.Type.AMMO);
        float headshot = (float) data.headshot();
        float velocity = 2 * (float) GunsTool.getGunDoubleTag(tag, "Power", 6) * (float) perkSpeed(perk);
        float bypassArmorRate = (float) data.bypassArmor();
        double damage;

        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE);
        boolean zoom = cap != null && cap.zoom;

        float spread;
        if (zoom) {
            spread = 0.01f;
            damage = 0.08333333 * data.damage() *
                    GunsTool.getGunDoubleTag(tag, "Power", 6);
        } else {
            spread = perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 0.5f : 2.5f;
            damage = (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 0.08333333 : 0.008333333) *
                    data.damage() *
                    GunsTool.getGunDoubleTag(tag, "Power", 6);
        }

        ProjectileEntity projectile = new ProjectileEntity(player.level())
                .shooter(player)
                .headShot(headshot)
                .zoom(zoom)
                .setGunItemId(stack);

        if (perk instanceof AmmoPerk ammoPerk) {
            int level = data.perk.getLevel(perk);

            bypassArmorRate += ammoPerk.bypassArmorRate + (perk == ModPerks.AP_BULLET.get() ? 0.05f * (level - 1) : 0);
            projectile.setRGB(ammoPerk.rgb);

            if (!ammoPerk.mobEffects.get().isEmpty()) {
                int amplifier;
                if (perk.descriptionId.equals("blade_bullet")) {
                    amplifier = level / 3;
                } else if (perk.descriptionId.equals("bread_bullet")) {
                    amplifier = 1;
                } else {
                    amplifier = level - 1;
                }

                ArrayList<MobEffectInstance> mobEffectInstances = new ArrayList<>();
                for (MobEffect effect : ammoPerk.mobEffects.get()) {
                    mobEffectInstances.add(new MobEffectInstance(Holder.direct(effect), 70 + 30 * level, amplifier));
                }
                projectile.effect(mobEffectInstances);
            }

            if (perk.descriptionId.equals("bread_bullet")) {
                projectile.knockback(level * 0.3f);
                projectile.forceKnockback();
            }
        }

        bypassArmorRate = Math.max(bypassArmorRate, 0);
        projectile.bypassArmorRate(bypassArmorRate);

        if (perk == ModPerks.SILVER_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.undeadMultiple(1.0f + 0.5f * level);
        } else if (perk == ModPerks.BEAST_BULLET.get()) {
            projectile.beast();
        } else if (perk == ModPerks.JHP_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.jhpBullet(level);
        } else if (perk == ModPerks.HE_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.heBullet(level);
        } else if (perk == ModPerks.INCENDIARY_BULLET.get()) {
            int level = data.perk.getLevel(perk);
            projectile.fireBullet(level, !zoom);
        }

        var dmgPerk = data.perk.get(Perk.Type.DAMAGE);
        if (dmgPerk == ModPerks.MONSTER_HUNTER.get()) {
            int perkLevel = data.perk.getLevel(dmgPerk);
            projectile.monsterMultiple(0.1f + 0.1f * perkLevel);
        }

        projectile.setPos(player.getX() - 0.1 * player.getLookAngle().x, player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);
        projectile.shoot(player, player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, (!zoom && perk == ModPerks.INCENDIARY_BULLET.get() ? 0.2f : 1) * velocity, spread);
        projectile.damage((float) damage);

        player.level().addFreshEntity(projectile);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
