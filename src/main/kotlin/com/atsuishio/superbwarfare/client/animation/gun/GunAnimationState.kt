package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType

enum class GunAnimationState(val playType: AnimationPlayType) {
    IDLE(AnimationPlayType.LOOP),
    EDIT(AnimationPlayType.PLAY_ONCE_HOLD),
    BOLT(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD_NORMAL(AnimationPlayType.PLAY_ONCE_HOLD),
    RELOAD_EMPTY(AnimationPlayType.PLAY_ONCE_HOLD),
    PREPARE(AnimationPlayType.PLAY_ONCE_HOLD),
    PREPARE_LOAD(AnimationPlayType.PLAY_ONCE_HOLD),
    ITERATIVE(AnimationPlayType.LOOP),
    ITERATIVE_2(AnimationPlayType.LOOP),
    FINISH(AnimationPlayType.PLAY_ONCE_HOLD),
    MELEE(AnimationPlayType.PLAY_ONCE_HOLD),
    FIRE(AnimationPlayType.PLAY_ONCE_STOP),

    /**
     * 蓄力片段（`GunAnimation.Charge`）。**不参与** [resolveState] 的选状态，
     * 和 [FIRE] 一样是一条叠在基础状态上的层（见 `GeoGunAnimationInstance.updateChargeRunner`）。
     *
     * `PLAY_ONCE_HOLD` 对上去就是资源里那句 `"loop": "hold_on_last_frame"`：正向播到末帧停住，
     * 不循环。喷满到自动开火就是"停住"那一刻；中途松手则不走这条完结态，由
     * `updateChargeRunner` 冻住相位、把权重淡到 0（见 `applyCharge`）。
     */
    CHARGE(AnimationPlayType.PLAY_ONCE_HOLD),

    RUN(AnimationPlayType.LOOP);

    /**
     * 这些状态都属于"**换弹这一整套动作**"（拆成多段的那种也在此列）。
     *
     * 原先它是 `GeoGunAnimationInstance` 里的一个私有扩展函数；提上来是因为渲染侧也要问同一个问题
     * ——见 [takesHandAway]。⚠ 提上来时必须**删掉那个私有扩展**：同名的私有扩展会把成员遮蔽掉，
     * 两处各判一次，改了一处另一处不会跟着变。
     */
    val isReload: Boolean
        get() = this == RELOAD ||
                this == RELOAD_NORMAL ||
                this == RELOAD_EMPTY ||
                this == PREPARE ||
                this == PREPARE_LOAD ||
                this == ITERATIVE ||
                this == ITERATIVE_2 ||
                this == FINISH

    /**
     * 这一状态下主武器的动画会**自己决定手待在哪**（去抓弹匣 / 推枪机 / 挥刺刀 /
     * 把整枪连同手一起挪进"检视"姿势），所以部署中的副武器此时不该再牵着手臂走
     * （§11.11.7.4 / §11.11.11）。
     *
     * 判据是**实测的位移**（宿主 `ak_12` 的 `lefthand_pos` 相对 idle 的最大位移，
     * `build/verify/VerifyArmOverlay.java` 与 `VerifyEditArmYield.java`）：
     *
     * - `reload_empty` **1.55 方块**（手去抓弹匣）、`hit`（近战）**1.25**、`edit` **0.93**；
     * - 对照：`fire` 0.47（`ak_12.fire` 根本没 key `lefthand`，那 0.47 全来自 `root` 的枪身位移）、
     *   `change_fire_mode` 0.02。
     *
     * 也就是前三类里"手该待哪儿"的答案**只有主武器自己知道**，接管必须让位；
     * `FIRE` / `CHANGE_FIRE_MODE` 则**不该**让位 —— 它们的位移只是整枪在动、手跟着走，
     * 让位反而成了"每打一发手闪一下"。
     *
     * ⚠ `EDIT` 是**后补**的（五期）：改装动画把 `root` 连 `lefthand` 一起挪进检视姿势，
     * 不让位时副武器的挂点会把左手按在原处、随枪身拉开 **0.75 方块**，而且**持续整个改装过程**
     * （不是一瞬），所以这一条必须让位。
     *
     * ⚠ 这个成员只服务于**部署期间**那一条接管路径。曾经它还管着"装上配件就接管"（含非部署状态），
     * 那条路已废弃 —— 原因见 §11.11.7.4：非部署时主武器的动画在**中途**也可能挪动左手，
     * 常驻接管会把那些动作一起压掉。
     */
    val takesHandAway: Boolean
        get() = isReload || this == MELEE || this == EDIT
}
