package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.molang.MolangVariable;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.model.GeoModel;

import java.util.function.DoubleSupplier;

public abstract class CustomGunModel<T extends GunItem & GeoAnimatable> extends GeoModel<T> {

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

        // GunData

        var player = mc.player;
        if (player == null) return;

        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return;

        var data = GunData.from(stack);

        set(MolangVariable.SBW_IS_EMPTY, () -> data.isEmpty.get() ? 1 : 0);
    }

    private static void set(String key, DoubleSupplier value) {
        MathParser.setVariable(key, value);
    }
}
