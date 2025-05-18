package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.molang.MolangVariable;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.loading.math.MathParser;
import software.bernie.geckolib.loading.math.MolangQueries;
import software.bernie.geckolib.model.GeoModel;

public abstract class CustomGunModel<T extends GunItem & GeoAnimatable> extends GeoModel<T> {

    @Override
    public void applyMolangQueries(AnimationState<T> animationState, double animTime) {
        Minecraft mc = Minecraft.getInstance();

        MathParser.setVariable(MolangQueries.LIFE_TIME, () -> animTime / 20d);

        if (mc.level != null) {
            MathParser.setVariable(MolangQueries.ACTOR_COUNT, mc.level::getEntityCount);
            MathParser.setVariable(MolangQueries.TIME_OF_DAY, () -> mc.level.getDayTime() / 24000f);
            MathParser.setVariable(MolangQueries.MOON_PHASE, mc.level::getMoonPhase);
        }

        MathParser.setVariable(MolangVariable.SBW_SYSTEM_TIME, System::currentTimeMillis);
    }
}
