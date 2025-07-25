package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.HomemadeShotgunItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Set;
import java.util.function.Supplier;

public class HomemadeShotgunItem extends GunItem {

    public HomemadeShotgunItem() {
        super(new Item.Properties().rarity(Rarity.COMMON));
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.HOMEMADE_SHOTGUN_RELOAD_EMPTY.get(), ModSounds.HOMEMADE_SHOTGUN_NORMAL.get());
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(HomemadeShotgunItemModel::new, 0, 0.05, 0.25, 1.2);
    }

    private PlayState idlePredicate(AnimationState<HomemadeShotgunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.homemade_shotgun.idle"));

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.homemade_shotgun.reload_empty"));
        }

        if (GunData.from(stack).reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.homemade_shotgun.reload_normal"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.homemade_shotgun.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.homemade_shotgun.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.homemade_shotgun.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 2, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/homemade_shotgun_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "Homemade Shotgun";
    }

    @Override
    public boolean isOpenBolt(ItemStack stack) {
        return true;
    }

    @Override
    public void beforeShoot(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom
    ) {
        super.beforeShoot(shooter, level, shootPosition, shootDirection, data, spread, zoom);

        if (shooter instanceof ServerPlayer serverPlayer) {
            ParticleTool.sendParticle(level, ParticleTypes.CLOUD, shooter.getX() + 1.8 * shooter.getLookAngle().x, shooter.getY() + shooter.getBbHeight() - 0.1 + 1.8 * shooter.getLookAngle().y,
                    shooter.getZ() + 1.8 * shooter.getLookAngle().z, 30, 0.4, 0.4, 0.4, 0.005, true, serverPlayer);
        }
    }
}
