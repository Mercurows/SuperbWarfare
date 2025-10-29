package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.animation.AnimationState;

import static com.atsuishio.superbwarfare.entity.vehicle.TruckEntity.GREEN;

public class TruckModel extends VehicleModel<TruckEntity> {

    @Override
    public ResourceLocation getTextureResource(TruckEntity entity) {
        if (entity.getEntityData().get(GREEN)) {
            return Mod.loc("textures/entity/truck_green.png");
        }
        return Mod.loc("textures/entity/truck_red.png");
    }

    @Override
    public void setCustomAnimations(TruckEntity vehicle, long instanceId, AnimationState<TruckEntity> animationState) {
        super.setCustomAnimations(vehicle, instanceId, animationState);
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        // 方向盘

        var control = getAnimationProcessor().getBone("control");

        if (control != null) {
            control.setRotY(12 * Mth.lerp(partialTick, vehicle.rudderRotO, vehicle.getRudderRot()));
        }
    }

    @Override
    public boolean hasWheel() {
        return true;
    }
}
