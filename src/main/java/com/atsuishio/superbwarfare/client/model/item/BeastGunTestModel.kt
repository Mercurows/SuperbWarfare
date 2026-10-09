package com.atsuishio.superbwarfare.client.model.item

import com.atsuishio.superbwarfare.item.gun.special.BeastGunTestItem
import software.bernie.geckolib.core.animation.AnimationState

object BeastGunTestModel : CustomGunModel<BeastGunTestItem>() {

    override fun setCustomAnimations(
        animatable: BeastGunTestItem,
        instanceId: Long,
        animationState: AnimationState<BeastGunTestItem>
    ) {
    }
}
