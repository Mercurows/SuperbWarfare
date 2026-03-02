package com.atsuishio.superbwarfare.client.renderer.animations;

import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import com.maydaymemory.mae.basic.Pose;
import com.maydaymemory.mae.control.statemachine.AnimationStateMachine;

public class SenpaiAnimationInstance {
    private final SenpaiContext context;
    private final AnimationStateMachine<SenpaiContext> stateMachine;

    public SenpaiAnimationInstance(SenpaiEntity entity) {
        this.context = new SenpaiContext(entity);
        this.stateMachine = new AnimationStateMachine<>(SenpaiStates.IDLE, context, System::nanoTime);
        SenpaiStates.IDLE.onEnter(this.context, SenpaiStates.IDLE);
    }

    public void tick() {
        stateMachine.tick();
        context.tick();
    }

    public SenpaiContext getContext() {
        return context;
    }

    public Pose getPose() {
        return stateMachine.getPose();
    }
}
