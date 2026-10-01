package com.atsuishio.superbwarfare.compat.acceleratedrendering

import com.atsuishio.superbwarfare.compat.acceleratedrendering.AcceleratedRenderingCompat.beginVanillaAcceleration
import com.atsuishio.superbwarfare.compat.acceleratedrendering.AcceleratedRenderingCompat.endVanillaAcceleration
import com.atsuishio.superbwarfare.compat.acceleratedrendering.AcceleratedRenderingCompat.isLoaded
import com.github.argon4w.acceleratedrendering.core.CoreFeature
import com.github.argon4w.acceleratedrendering.features.entities.AcceleratedEntityRenderingFeature
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.fml.ModList

@OnlyIn(Dist.CLIENT)
object AcceleratedRenderingCompat {

    private const val MOD_ID = "acceleratedrendering"

    private val backend: Backend? by lazy {
        if (ModList.get().isLoaded(MOD_ID)) Backend() else null
    }

    /**
     * 客户端初始化时调一次，把"AR 在不在"这件事定下来。
     *
     * 严格说 [isLoaded] 的 `by lazy` 已经够了，但**显式探一次**能让"AR 不在"这条分支的代价
     * 从"第一次渲染"提前到启动期，也免得某个渲染路径意外触发类加载。
     */
    fun init() {
        isLoaded
    }

    val isLoaded: Boolean
        get() = backend != null

    /**
     * 本帧的模型渲染会不会被 AR 接管。
     *
     * 判据与 TACZ 逐条一致：AR 加载完成 + 加速管线启用 + 当前处在被加速的渲染阶段
     * （世界 / 手 / GUI）。只有为真时才值得去关加速。
     */
    val shouldAccelerate: Boolean
        get() = backend?.accelerating() ?: false

    /**
     * 让 AR 在接下来这段里走原版管线（几何立即出图）；必须与 [endVanillaAcceleration] 配平。
     *
     * AR 内部是**栈**语义（`useVanillaPipeline()` push / `resetPipeline()` pop），
     * 所以嵌套使用是安全的，也不会覆盖别人设的状态。
     */
    fun beginVanillaAcceleration() {
        backend?.beginVanilla()
    }

    /** 还原 [beginVanillaAcceleration] */
    fun endVanillaAcceleration() {
        backend?.endVanilla()
    }

    /**
     * 所有 AR 类型出现的地方；只有确认 AR 存在时才会被实例化。
     */
    private class Backend {

        fun accelerating(): Boolean =
            CoreFeature.isLoaded() &&
                    AcceleratedEntityRenderingFeature.isEnabled() &&
                    AcceleratedEntityRenderingFeature.shouldUseAcceleratedPipeline() &&
                    (
                            CoreFeature.isRenderingLevel() ||
                                    CoreFeature.isRenderingHand() ||
                                    (CoreFeature.isRenderingGui() && AcceleratedEntityRenderingFeature.shouldAccelerateInGui())
                            )

        fun beginVanilla() {
            AcceleratedEntityRenderingFeature.useVanillaPipeline()
        }

        fun endVanilla() {
            AcceleratedEntityRenderingFeature.resetPipeline()
        }
    }
}
