package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.molang.MolangVariable;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.DoubleSupplier;

public abstract class CustomGunModel<T extends GunItem & GeoAnimatable> extends GeoModel<T> {

    public ResourceLocation getLODModelResource(T animatable) {
        return this.getModelResource(animatable, null);
    }

    public ResourceLocation getLODTextureResource(T animatable) {
        return this.getTextureResource(animatable, null);
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
        set(MolangVariable.SBW_IS_EMPTY, () -> data.isEmpty.get() ? 1 : 0);
        set(MolangVariable.SBW_SYSTEM_TIME, System::currentTimeMillis);
    }

    private static void set(String key, DoubleSupplier value) {
        MathParser.setVariable(key, value);
    }

    private void resetQueryValue() {
        set(MolangVariable.SBW_IS_EMPTY, () -> 0);
        set(MolangVariable.SBW_SYSTEM_TIME, () -> 0);
    }

    public boolean shouldCancelRender(ItemStack stack, AnimationState<T> animationState) {
        if (!(stack.getItem() instanceof GunItem)) return true;
        var item = animationState.getData(DataTickets.ITEMSTACK);
        if (item == null || GeoItem.getId(item) != GeoItem.getId(stack)) return true;
        return animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }
}
