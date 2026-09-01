package com.atsuishio.superbwarfare.data.gun

/**
 * Fires data-driven [GunActionStep] entries once when the current timeline
 * progress crosses their configured threshold.
 */
object GunActionStepExecutor {
    fun tickReload(data: GunData) {
        val reload = data.reload
        val steps = data.getDefault().actionSteps.list.filter { step ->
            when (step.timeline) {
                GunActionTimeline.RELOAD -> true
                GunActionTimeline.RELOAD_NORMAL -> reload.normal()
                GunActionTimeline.RELOAD_EMPTY -> reload.empty()
                GunActionTimeline.RELOAD_FINISH -> false
                GunActionTimeline.NO_AMMO -> false
                GunActionTimeline.BOLT -> false
            }
        }
        tick(steps, reload.previousProgress(), reload.currentProgress(), data)
    }

    fun triggerNoAmmo(data: GunData) {
        data.getDefault().actionSteps.list
            .filter { it.timeline == GunActionTimeline.NO_AMMO }
            .forEach { it.apply(data) }
    }

    fun tickReloadFinish(data: GunData) {
        val steps = data.getDefault().actionSteps.list.filter {
            it.timeline == GunActionTimeline.RELOAD_FINISH
        }
        tick(
            steps,
            data.reload.finishPreviousProgress(),
            data.reload.finishCurrentProgress(),
            data,
        )
    }

    fun tickBolt(data: GunData) {
        val steps = data.getDefault().actionSteps.list.filter { it.timeline == GunActionTimeline.BOLT }
        tick(steps, data.bolt.previousProgress(), data.bolt.currentProgress(), data)
    }

    private fun tick(
        steps: List<GunActionStep>,
        previousProgress: Float,
        currentProgress: Float,
        data: GunData,
    ) {
        for (step in steps) {
            if (previousProgress < step.progress && currentProgress >= step.progress) {
                step.apply(data)
            }
        }
    }
}
