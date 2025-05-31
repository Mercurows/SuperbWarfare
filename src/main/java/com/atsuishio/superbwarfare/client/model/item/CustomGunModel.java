package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.molang.MolangVariable;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.mixins.AnimationProcessorAccessor;
import com.atsuishio.superbwarfare.mixins.GeoModelAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.RenderUtil;

import java.util.function.DoubleSupplier;

public abstract class CustomGunModel<T extends GunItem & GeoAnimatable> extends GeoModel<T> {

    // TODO 优化这一坨
    @Override
    public void setCustomAnimations(T animatable, long instanceId, AnimationState<T> animationState) {
        Minecraft mc = Minecraft.getInstance();
        AnimatableManager<T> animatableManager = animatable.getAnimatableInstanceCache().getManagerForId(instanceId);
        Double currentTick = animationState.getData(DataTickets.TICK);

        if (currentTick == null)
            currentTick = RenderUtil.getCurrentTick();

//        if (animatableManager.getFirstTickTime() == -1)
//            animatableManager.startedAt(currentTick + mc.getFrameTime());

        double currentFrameTime = currentTick - animatableManager.getFirstTickTime();
        boolean isReRender = !animatableManager.isFirstTick() && currentFrameTime == animatableManager.getLastUpdateTime();

        if (isReRender && instanceId == ((GeoModelAccessor) this).getLastRenderedInstance())
            return;

        if (!mc.isPaused() || animatable.shouldPlayAnimsWhileGamePaused()) {
            animatableManager.updatedAt(currentFrameTime);

            double lastUpdateTime = animatableManager.getLastUpdateTime();
            ((GeoModelAccessor) this).setAnimTime(((GeoModelAccessor) this).getAnimTime() + lastUpdateTime - ((GeoModelAccessor) this).getLastGameTickTime());
            ((GeoModelAccessor) this).setLastGameTickTime(lastUpdateTime);
        }

        animationState.animationTick = ((GeoModelAccessor) this).getAnimTime();
        ((GeoModelAccessor) this).setLastRenderedInstance(instanceId);
        AnimationProcessor<T> processor = getAnimationProcessor();

        var model = ((AnimationProcessorAccessor<T>) processor).getModel();
        if (model instanceof CustomGunModel<T> customGunModel) {
            customGunModel.applyCustomMolangQueries(animationState, ((GeoModelAccessor) this).getAnimTime());
        }

        if (!processor.getRegisteredBones().isEmpty())
            processor.tickAnimation(animatable, this, animatableManager, ((GeoModelAccessor) this).getAnimTime(), animationState, crashIfBoneMissing());

        setCustomAnimations(animatable, instanceId, animationState);
    }

    @Override
    public void applyMolangQueries(AnimationState<T> animationState, double animTime) {
        Minecraft mc = Minecraft.getInstance();

        set(MolangQueries.LIFE_TIME, () -> animTime / 20d);

        if (mc.level != null) {
            set(MolangQueries.ACTOR_COUNT, mc.level::getEntityCount);
            set(MolangQueries.TIME_OF_DAY, () -> mc.level.getDayTime() / 24000f);
            set(MolangQueries.MOON_PHASE, mc.level::getMoonPhase);
        }

        set(MolangVariable.SBW_SYSTEM_TIME, System::currentTimeMillis);
    }

    public void applyCustomMolangQueries(AnimationState<T> animationState, double animTime) {
        this.applyMolangQueries(animationState, animTime);

        Minecraft mc = Minecraft.getInstance();

        // GunData

        var player = mc.player;
        if (player == null) {
            resetQueryValue();
            return;
        }

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) {
            resetQueryValue();
            return;
        }

        var item = animationState.getData(DataTickets.ITEMSTACK);
        if (item == null || GeoItem.getId(item) != GeoItem.getId(stack)) {
            resetQueryValue();
            return;
        }

        if (animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            resetQueryValue();
            return;
        }

        var data = GunData.from(stack);

        // TODO 实现正确的空判断，需要分离stack
        set(MolangVariable.SBW_IS_EMPTY, () -> data.isEmpty.get() ? 1 : 0);
    }

    private static void set(String key, DoubleSupplier value) {
        MathParser.setVariable(key, value);
    }

    private void resetQueryValue() {
        MathParser.setVariable(MolangVariable.SBW_IS_EMPTY, () -> 0);
    }

    public boolean shouldCancelRender(ItemStack stack, AnimationState<T> animationState) {
        if (!(stack.getItem() instanceof GunItem)) return true;
        var item = animationState.getData(DataTickets.ITEMSTACK);
        if (item == null || GeoItem.getId(item) != GeoItem.getId(stack)) return true;
        return animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
}
