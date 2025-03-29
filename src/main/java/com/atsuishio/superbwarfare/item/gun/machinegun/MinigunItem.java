package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.MinigunItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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

public class MinigunItem extends GunItem implements GeoItem {

    private static final String TAG_HEAT = "heat";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public MinigunItem() {
        super(new Properties().stacksTo(1).rarity(ModRarity.getLegendary()));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        return NBTTool.getDouble(pStack, TAG_HEAT, 0) != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        return Math.round((float) NBTTool.getDouble(pStack, TAG_HEAT, 0) * 13.0F / 51F);
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        double f = 1 - NBTTool.getDouble(pStack, TAG_HEAT, 0) / 55.0F;
        return Mth.hsvToRgb((float) f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public GeoItemRenderer<? extends GunItem> getRenderer() {
        return new MinigunItemRenderer();
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<MinigunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return PlayState.STOP;

        if (player.isSprinting() && player.onGround() && player.getPersistentData().getDouble("noRun") == 0 && ClientEventHandler.drawTime < 0.01) {
            if (player.hasEffect(MobEffects.MOVEMENT_SPEED)) {
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
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);

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

        if (entity instanceof ServerPlayer serverPlayer && entity.level() instanceof ServerLevel serverLevel && NBTTool.getTag(itemstack).getDouble("heat") > 4 && entity.isInWaterOrRain()) {
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

        NBTTool.getTag(itemstack).putDouble("heat", Mth.clamp(NBTTool.getTag(itemstack).getDouble("heat") - 0.05 - cooldown, 0, 55));

        if (NBTTool.getTag(itemstack).getDouble("overheat") > 0) {
            NBTTool.getTag(itemstack).putDouble("overheat", (NBTTool.getTag(itemstack).getDouble("overheat") - 1));
        }
    }

    public static ItemStack getGunInstance() {
        ItemStack stack = new ItemStack(ModItems.MINIGUN.get());
        GunsTool.initCreativeGun(stack, ModItems.MINIGUN.getId().getPath());
        return stack;
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

    @Override
    public boolean useBackpackAmmo(ItemStack stack) {
        return true;
    }
}