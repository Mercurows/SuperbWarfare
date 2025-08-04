package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.RpgItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.LauncherImageComponent;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RpgItem extends GunItem {

    public RpgItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return RpgItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<RpgItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.idle"));

        if (ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.rpg.edit"));
        }

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.rpg.reload"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.RPG_RELOAD_EMPTY.get());
    }

    @Override
    public ResourceLocation getGunIcon(ItemStack stack) {
        int i = GunData.from(stack).selectedAmmoType.get();
        if (i == 0) {
            return Mod.loc("textures/gun_icon/rpg_tbg_icon.png");
        }
        return Mod.loc("textures/gun_icon/rpg_standard_icon.png");
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new LauncherImageComponent(pStack));
    }

    @Override
    public boolean shootBullet(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        if (data.reloading()) return false;
        if (!super.shootBullet(shooter, level, shootPosition, shootDirection, data, spread, zoom, uuid)) return false;

        if (shooter != null) {
            ParticleTool.sendParticle(level, ParticleTypes.CLOUD, shooter.getX() + 1.8 * shooter.getLookAngle().x,
                    shooter.getY() + shooter.getBbHeight() - 0.1 + 1.8 * shooter.getLookAngle().y,
                    shooter.getZ() + 1.8 * shooter.getLookAngle().z,
                    30, 0.4, 0.4, 0.4, 0.005, true);
        }

        data.isEmpty.set(true);
        data.closeHammer.set(true);

        return true;
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);
        behaviors.put(84, data -> data.isEmpty.set(false));
        behaviors.put(16, data -> data.closeHammer.set(false));
    }

    @Override
    public boolean canEditAttachments(ItemStack stack) {
        return true;
    }
}