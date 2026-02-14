package com.atsuishio.superbwarfare.client.model.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import net.minecraft.util.Mth
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.model.GeoModel

class FuMO25Model : GeoModel<FuMO25BlockEntity>() {
    override fun getAnimationResource(animatable: FuMO25BlockEntity) = loc("animations/fumo_25.animation.json")

    override fun getModelResource(animatable: FuMO25BlockEntity) = loc("geo/fumo_25.geo.json")

    override fun getTextureResource(animatable: FuMO25BlockEntity) = loc("textures/block/fumo_25.png")

    override fun setCustomAnimations(
        animatable: FuMO25BlockEntity,
        instanceId: Long,
        animationState: AnimationState<FuMO25BlockEntity>
    ) {
        val bone = this.animationProcessor.getBone("mian") ?: return

        val targetDeg = getTick(animatable) * 1.8f // 目标角度（0~360°）
        val currentDeg = animatable.yRot0 * Mth.RAD_TO_DEG // 当前角度（弧度转角度）

        // 计算最短路径角度差（处理360°跳变）
        val diffDeg = Mth.wrapDegrees(targetDeg - currentDeg)

        // 应用插值
        val newDeg = currentDeg + diffDeg * 0.1f

        // 转换为弧度并更新
        val newRad = newDeg * Mth.DEG_TO_RAD
        animatable.yRot0 = newRad
        bone.rotY = newRad
    }

    private fun getTick(animatable: FuMO25BlockEntity): Float {
        val tick = animatable.getAnimData(FuMO25BlockEntity.FUMO25_TICK)
        if (tick != null) {
            return tick.toFloat()
        }
        return 0f
    }
}