package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.MinigunItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

public class MinigunItem extends GunItem implements GeoItem {
    @Override
    public int getCustomRPM(ItemStack stack) {
        return GunData.from(stack).data().getInt("CustomRPM");
    }

    private static final String TAG_HEAT = "heat";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public MinigunItem() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        return NBTTool.getTag(pStack).getDouble(TAG_HEAT) != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        return Math.round((float) NBTTool.getTag(pStack).getDouble(TAG_HEAT) * 13.0F / 51F);
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        double f = 1 - NBTTool.getTag(pStack).getDouble(TAG_HEAT) / 55.0F;
        return Mth.hsvToRgb((float) f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return MinigunItemRenderer::new;
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<MinigunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.run"));
            }
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 6, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        float yRot = entity.getYRot();
        if (yRot < 0) {
            yRot += 360;
        }
        yRot = yRot + 90 % 360;

        var leftPos = new Vector3d(1.2, -0.3, 0.3);
        if (entity.isSprinting()) {
            leftPos = new Vector3d(1., -0.4, -0.4);
        }

        leftPos.rotateZ(-entity.getXRot() * Mth.DEG_TO_RAD);
        leftPos.rotateY(-yRot * Mth.DEG_TO_RAD);

        double cooldown = 0;
        if (entity.wasInPowderSnow) {
            cooldown = 0.15;
        } else if (entity.isInWaterOrRain()) {
            cooldown = 0.04;
        } else if (entity.isOnFire() || entity.isInLava()) {
            cooldown = -0.1;
        }

        var data = GunData.from(stack);
        var tag = data.tag();
        if (entity instanceof ServerPlayer serverPlayer && entity.level() instanceof ServerLevel serverLevel && tag.getDouble("heat") > 4 && entity.isInWaterOrRain()) {
            if (entity.isInWater()) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.BUBBLE_COLUMN_UP,
                        entity.getX() + leftPos.x,
                        entity.getEyeY() + leftPos.y,
                        entity.getZ() + leftPos.z,
                        1, 0.1, 0.1, 0.1, 0.002, true, serverPlayer);
            }
            ParticleTool.sendParticle(serverLevel, ModParticleTypes.CUSTOM_CLOUD.get(),
                    entity.getX() + leftPos.x,
                    entity.getEyeY() + leftPos.y,
                    entity.getZ() + leftPos.z,
                    1, 0.1, 0.1, 0.1, 0.002, true, serverPlayer);
        }

        tag.putDouble("heat", Mth.clamp(tag.getDouble("heat") - 0.05 - cooldown, 0, 55));
        if (tag.getDouble("overheat") > 0) {
            tag.putDouble("overheat", (tag.getDouble("overheat") - 1));
        }

        data.save();
    }

    @Override
    public void onShoot(GunData data, Player player, double spread, boolean zoom) {
        var tag = data.tag();

        if (!data.hasBackupAmmo(player)) return;

        // TODO 替换为通用过热处理
        tag.putDouble("heat", (tag.getDouble("heat") + 0.1));
        if (tag.getDouble("heat") >= 50.5) {
            tag.putDouble("overheat", 40);
            player.getCooldowns().addCooldown(data.item(), 40);
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

        shootBullet(player, data, spread, zoom);
        data.consumeBackupAmmo(player, 1);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/minigun_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M134 MINIGUN";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return switch (perk.type) {
            case AMMO -> perk != ModPerks.MICRO_MISSILE.get() && perk != ModPerks.LONGER_WIRE.get();
            case FUNCTIONAL -> perk == ModPerks.FIELD_DOCTOR.get() || perk == ModPerks.INTELLIGENT_CHIP.get();
            case DAMAGE -> perk == ModPerks.MONSTER_HUNTER.get() || perk == ModPerks.KILLING_TALLY.get();
        };
    }

    @Override
    public boolean isAutoWeapon(ItemStack stack) {
        return true;
    }

}