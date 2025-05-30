package com.atsuishio.superbwarfare.client.model.block;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class FuMO25Model extends GeoModel<FuMO25BlockEntity> {

    @Override
    public ResourceLocation getAnimationResource(FuMO25BlockEntity animatable) {
        return Mod.loc("animations/fumo_25.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(FuMO25BlockEntity animatable) {
        return Mod.loc("geo/fumo_25.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FuMO25BlockEntity animatable) {
        return Mod.loc("textures/block/fumo_25.png");
    }

    @Override
    public void setCustomAnimations(FuMO25BlockEntity animatable, long instanceId, AnimationState<FuMO25BlockEntity> animationState) {
        GeoBone bone = this.getAnimationProcessor().getBone("mian");
        if (bone == null) return;

        float targetDeg = getTick(animatable) * 1.8f; // 目标角度（0~360°）
        float currentDeg = animatable.yRot0 * Mth.RAD_TO_DEG; // 当前角度（弧度转角度）

        // 计算最短路径角度差（处理360°跳变）
        float diffDeg = Mth.wrapDegrees(targetDeg - currentDeg);

        // 应用插值
        float newDeg = currentDeg + diffDeg * 0.1f;

        // 转换为弧度并更新
        float newRad = newDeg * Mth.DEG_TO_RAD;
        animatable.yRot0 = newRad;
        bone.setRotY(newRad);
    }

    private float getTick(FuMO25BlockEntity animatable) {
        Integer tick = animatable.getAnimData(FuMO25BlockEntity.FUMO25_TICK);
        if (tick != null) {
            return tick.floatValue();
        }
        return 0;
    }
}