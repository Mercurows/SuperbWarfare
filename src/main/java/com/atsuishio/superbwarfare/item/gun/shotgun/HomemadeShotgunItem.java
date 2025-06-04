package com.atsuishio.superbwarfare.item.gun.shotgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.HomemadeShotgunItemRenderer;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Set;
import java.util.function.Supplier;

public class HomemadeShotgunItem extends GunItem {

    public HomemadeShotgunItem() {
        super(new Item.Properties().durability(24).rarity(Rarity.COMMON));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return Math.round(13.0F - (float) pStack.getDamageValue() * 13.0F / (float) this.getMaxDamage(pStack));
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        float stackMaxDamage = this.getMaxDamage(pStack);
        float f = Math.max(0.0F, (stackMaxDamage - (float) pStack.getDamageValue()) / stackMaxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.HOMEMADE_SHOTGUN_RELOAD_EMPTY.get(), ModSounds.HOMEMADE_SHOTGUN_NORMAL.get());
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return HomemadeShotgunItemRenderer::new;
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
    public void beforeShoot(GunData data, Player player, double spread, boolean zoom) {
        super.beforeShoot(data, player, spread, zoom);

        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x, player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z, 30, 0.4, 0.4, 0.4, 0.005, true, serverPlayer);
        }
    }

    @Override
    public void afterShoot(GunData data, Player player) {
        super.afterShoot(data, player);

        var stack = data.stack();
        stack.hurtAndBreak(1, (ServerLevel) player.level(), player, p -> {
        });
    }
}
