package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.CustomRendererItem;
import com.atsuishio.superbwarfare.resource.gun.GunResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.ParametersAreNonnullByDefault;

@EventBusSubscriber(modid = Mod.MODID)
public abstract class GunGeoItem extends GunItem implements GeoItem, CustomRendererItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected final RandomSource random = RandomSource.create();

    public GunGeoItem(Properties properties) {
        super(properties.stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @OnlyIn(Dist.CLIENT)
    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        return PoseTool.pose(entityLiving, hand, stack);
    }

    @OnlyIn(Dist.CLIENT)
    protected PlayState animationPredicate(AnimationState<GunGeoItem> event) {
        var player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        var resource = GunResource.from(stack);
        var data = GunData.from(stack);

        var defaultResource = resource.compute();
        if (defaultResource == null) return PlayState.STOP;

        var animation = defaultResource.animation;
        if (animation == null || animation.idle == null) return PlayState.STOP;

        // Idle
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return event.setAndContinue(RawAnimation.begin().thenLoop(animation.idle));
        }

        // Edit
        if (animation.edit != null && ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay(animation.edit));
        }

        // Bolt
        if (animation.bolt != null && data.bolt.actionTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay(animation.bolt));
        }

        // Reload
        if (data.reloading()) {
            if (animation.reload != null) {
                return event.setAndContinue(RawAnimation.begin().thenPlay(animation.reload));
            } else if (animation.reloadNormal != null && data.reload.normal()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay(animation.reloadNormal));
            } else if (animation.reloadEmpty != null && data.reload.empty()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay(animation.reloadEmpty));
            }
        }

        // Melee
        if (animation.melee != null && ClientEventHandler.gunMelee > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay(animation.melee));
        }

        // Fire
        if (animation.fire != null && ClientEventHandler.holdingFireKey && data.canShoot(player)) {
            return event.setAndContinue(RawAnimation.begin().thenLoop(animation.fire));
        }

        // Run & Sprint
        if (player.isSprinting() && player.onGround() && ClientEventHandler.noSprintTicks == 0 && ClientEventHandler.drawTime < 0.01) {
            if (animation.run != null) {
                return event.setAndContinue(RawAnimation.begin().thenLoop(animation.run));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop(animation.idle));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "animationController", 1, this::animationPredicate));
    }

    @SubscribeEvent
    private static void registerGunExtensions(RegisterClientExtensionsEvent event) {
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof GunGeoItem gun) {
                event.registerItem(new IClientItemExtensions() {
                    private final BlockEntityWithoutLevelRenderer renderer = gun.getRenderer().get();

                    @Override
                    public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        return renderer;
                    }

                    @Override
                    @ParametersAreNonnullByDefault
                    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                        return gun.getArmPose(entityLiving, hand, stack);
                    }
                }, item);
            }
        }
    }
}
