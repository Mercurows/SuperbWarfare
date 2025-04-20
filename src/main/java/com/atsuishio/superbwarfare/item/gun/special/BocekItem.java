package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.player.PlayerVariable;
import com.atsuishio.superbwarfare.client.renderer.item.BocekItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.BocekImageComponent;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.InventoryTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

public class BocekItem extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public BocekItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return BocekItemRenderer::new;
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (ClientEventHandler.bowPull) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.bocek.pull"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    private PlayState firePredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (GunsTool.getGunIntTag(GunData.from(stack).tag, "ArrowEmpty") > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bocek.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    private PlayState reloadPredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        var data = GunData.from(stack);

        if (GunsTool.getGunIntTag(GunData.from(stack).tag, "ArrowEmpty") > 0 && data.ammo.get() == 0 && (data.countBackupAmmo(player) > 0 || InventoryTool.hasCreativeAmmoBox(player))) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bocek.reload"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    private PlayState editPredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (PlayerVariable.isEditing(player)) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bocek.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
        var fireController = new AnimationController<>(this, "fireController", 0, this::firePredicate);
        data.add(fireController);
        var reloadController = new AnimationController<>(this, "reloadController", 0, this::reloadPredicate);
        data.add(reloadController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        var data = GunData.from(stack);

        if (entity instanceof Player player) {
            if (GunsTool.getGunIntTag(data.tag, "ArrowEmpty") > 0) {
                GunsTool.setGunIntTag(data.tag, "ArrowEmpty", GunsTool.getGunIntTag(data.tag, "ArrowEmpty") - 1);
            }

            if (GunsTool.getGunIntTag(data.tag, "ArrowEmpty") == 0 && data.ammo.get() == 0 && data.hasBackupAmmo(player)) {
                data.consumeBackupAmmo(player, 1);
                data.ammo.set(1);
            }
            data.save();
        }
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/bocek_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "Bocek";
    }

    @Override
    public boolean isCustomizable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> 0.75;
            case 3 -> 1.75;
            default -> 0;
        };
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return switch (perk.type) {
            case AMMO -> !perk.descriptionId.equals("butterfly_bullet") && perk != ModPerks.MICRO_MISSILE.get();
            case FUNCTIONAL -> perk == ModPerks.FIELD_DOCTOR.get() || perk == ModPerks.INTELLIGENT_CHIP.get();
            case DAMAGE -> perk == ModPerks.MONSTER_HUNTER.get() || perk == ModPerks.KILLING_TALLY.get();
        };
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new BocekImageComponent(pStack));
    }

    @Override
    public String getAmmoDisplayName(GunData data) {
        return "Arrow";
    }

    @Override
    public void onShoot(GunData data, Player player, double spread, boolean zoom) {
    }

    @Override
    public void onFireKeyRelease(GunData data, Player player, double power, boolean zoom) {
        super.onFireKeyRelease(data, player, power, zoom);

        if (data.ammo.get() == 0) return;

        var perk = data.perk.get(Perk.Type.AMMO);

        if (player instanceof ServerPlayer serverPlayer) {
            SoundTool.stopSound(serverPlayer, ModSounds.BOCEK_PULL_1P.getId(), SoundSource.PLAYERS);
            SoundTool.stopSound(serverPlayer, ModSounds.BOCEK_PULL_3P.getId(), SoundSource.PLAYERS);
            PacketDistributor.sendToPlayer(serverPlayer, new ShootClientMessage(10));
        }

        if (power * 12 >= 6) {
            if (zoom) {
                spawnBullet(data, player, power, true);

                SoundTool.playLocalSound(player, ModSounds.BOCEK_ZOOM_FIRE_1P.get(), 10, 1);
                player.playSound(ModSounds.BOCEK_ZOOM_FIRE_3P.get(), 2, 1);
            } else {
                for (int i = 0; i < (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 1 : 10); i++) {
                    spawnBullet(data, player, power, false);
                }

                SoundTool.playLocalSound(player, ModSounds.BOCEK_SHATTER_CAP_FIRE_1P.get(), 10, 1);
                player.playSound(ModSounds.BOCEK_SHATTER_CAP_FIRE_3P.get(), 2, 1);
            }

            if (perk == ModPerks.BEAST_BULLET.get()) {
                player.playSound(ModSounds.HENG.get(), 4f, 1f);

                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.HENG.get(), 4f, 1f);
                }
            }

            GunsTool.setGunIntTag(data.tag, "ArrowEmpty", 7);
            data.ammo.set(data.ammo.get() - 1);
            data.save();
        }
    }


    public void spawnBullet(GunData data, Player player, double power, boolean zoom) {
        ItemStack stack = data.stack;

        var perk = data.perk.get(Perk.Type.AMMO);
        float headshot = (float) data.headshot();
        float velocity = (float) (24 * power * (float) perkSpeed(data));
        float bypassArmorRate = (float) data.bypassArmor();
        double damage;

        float spread;
        if (zoom) {
            spread = 0.01f;
            damage = 0.08333333 * data.damage() *
                    12 * power * perkDamage(perk);
        } else {
            spread = perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 0.5f : 2.5f;
            damage = (perk instanceof AmmoPerk ammoPerk && ammoPerk.slug ? 0.08333333 : 0.008333333) *
                    data.damage() * 12 * power * perkDamage(perk);
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
}