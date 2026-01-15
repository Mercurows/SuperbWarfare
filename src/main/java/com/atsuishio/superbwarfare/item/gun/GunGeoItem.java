package com.atsuishio.superbwarfare.item.gun;

import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.CustomRendererItem;
import com.atsuishio.superbwarfare.resource.gun.GunResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber
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

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(this.getClientExtensions());
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

    @OnlyIn(Dist.CLIENT)
    public IClientItemExtensions getClientExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = GunGeoItem.this.getRenderer().get();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return PoseTool.pose(entityLiving, hand, stack);
            }
        };
    }
}
