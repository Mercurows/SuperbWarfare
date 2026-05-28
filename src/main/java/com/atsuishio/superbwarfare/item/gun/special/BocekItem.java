package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.client.renderer.gun.BocekItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.BocekImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.NetworkRegistry;
import com.atsuishio.superbwarfare.network.message.receive.ShootClientMessage;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Optional;
import java.util.function.Supplier;

public class BocekItem extends GunGeoItem {

    public BocekItem() {
        super(new Item.Properties().rarity(Rarity.EPIC));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return BocekItemRenderer::new;
    }

    private PlayState idlePredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));

        if (ClientEventHandler.bowPull) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.bocek.pull"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.noSprintTicks == 0 && ClientEventHandler.drawTime < 0.01) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.run"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    private PlayState firePredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));

        var data = GunData.from(stack);

        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bocek.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));
    }

    private PlayState reloadPredicate(AnimationState<BocekItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.bocek.idle"));

        var data = GunData.from(stack);
        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.bocek.reload"));
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
    }

    @Override
    public boolean useSpecialFireProcedure(@NotNull GunData data) {
        return true;
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new BocekImageComponent(pStack));
    }

    @Override
    public void shoot(@NotNull ShootParameters parameters) {
    }

    @Override
    public void onFireKeyRelease(@NotNull GunData data, @NotNull Player player, double power, boolean zoom) {
        super.onFireKeyRelease(data, player, power, zoom);

        if (!data.hasEnoughAmmoToShoot(player)) return;

        var perk = data.perk.get(Perk.Type.AMMO);

        if (player instanceof ServerPlayer serverPlayer) {
            SoundTool.stopSound(serverPlayer, ModSounds.BOCEK_PULL_1P.getId(), SoundSource.PLAYERS);
            SoundTool.stopSound(serverPlayer, ModSounds.BOCEK_PULL_3P.getId(), SoundSource.PLAYERS);
            NetworkRegistry.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> serverPlayer), ShootClientMessage.INSTANCE);
        }

        if (power * 12 >= 6) {
            if (zoom) {
                spawnBullet(data, player, power, true);

                SoundTool.playLocalSound(player, ModSounds.BOCEK_ZOOM_FIRE_1P.get(), 10, 1);
                player.playSound(ModSounds.BOCEK_ZOOM_FIRE_3P.get(), 2, 1);
            } else {
                for (int i = 0; i < (perk instanceof AmmoPerk ammoPerk && ammoPerk.getSlug() ? 1 : 10); i++) {
                    spawnBullet(data, player, power, false);
                }

                SoundTool.playLocalSound(player, ModSounds.BOCEK_SHATTER_CAP_FIRE_1P.get(), 10, 1);
                player.playSound(ModSounds.BOCEK_SHATTER_CAP_FIRE_3P.get(), 2, 1);
            }

            if (perk == ModPerks.INSTANCE.getBEAST_BULLET().get()) {
                player.playSound(ModSounds.HENG.get(), 4f, 1f);

                if (player instanceof ServerPlayer serverPlayer) {
                    SoundTool.playLocalSound(serverPlayer, ModSounds.HENG.get(), 4f, 1f);
                }
            }

            data.ammo.set(data.ammo.get() - data.get(GunProp.AMMO_COST_PER_SHOOT));
            data.save();
        }
    }

    public void spawnBullet(GunData data, Player player, double power, boolean zoom) {
        ItemStack stack = data.stack;

        float headshot = data.get(GunProp.HEADSHOT).floatValue();
        float velocity = (float) (data.get(GunProp.VELOCITY) * power);
        float bypassArmorRate = data.get(GunProp.BYPASSES_ARMOR).floatValue();
        float explosionRadius = data.get(GunProp.EXPLOSION_RADIUS).floatValue();
        float explosionDamage = data.get(GunProp.EXPLOSION_DAMAGE).floatValue();
//        int projectileAmount = data.get(GunProp.PROJECTILE_AMOUNT);
        int projectileAmount = 10;

        double damage = data.get(GunProp.DAMAGE) * power;
        float spread = 0.01f;

        if (!zoom) {
//            spread = projectileAmount <= 1 ? 0.5f : 2.5f;
//            damage /= Math.max(1, projectileAmount);
            spread = 2.5f;
            damage /= projectileAmount;
        }

        ProjectileEntity projectile = new ProjectileEntity(player.level())
                .shooter(player)
                .headShot(headshot)
                .zoom(zoom)
                .bypassArmorRate(bypassArmorRate)
                .velocity(velocity)
                .setGunItemId(stack);

        projectile.setExplosionDamage(explosionDamage);
        projectile.setExplosionRadius(explosionRadius);

        for (Perk.Type type : Perk.Type.getEntries()) {
            var instance = data.perk.getInstances(type);
            instance.forEach(perk -> perk.perk().modifyProjectile(data, perk, projectile));
        }

        projectile.setPos(player.getX() - 0.1 * player.getLookAngle().x, player.getEyeY() - 0.1 - 0.1 * player.getLookAngle().y, player.getZ() + -0.1 * player.getLookAngle().z);
        projectile.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z, velocity, spread);
        projectile.damage((float) damage);

        player.level().addFreshEntity(projectile);
    }

    @OnlyIn(Dist.CLIENT)
    public IClientItemExtensions getClientExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = BocekItem.this.getRenderer().get();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return HumanoidModel.ArmPose.BOW_AND_ARROW;
            }
        };
    }
}