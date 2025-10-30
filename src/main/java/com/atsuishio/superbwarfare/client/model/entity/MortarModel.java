package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;

import static com.atsuishio.superbwarfare.entity.vehicle.MortarEntity.INTELLIGENT;

public class MortarModel extends VehicleModel<MortarEntity> {

    @Override
    public @Nullable TransformContext<MortarEntity> collectTransform(String boneName) {
        if (boneName.equals("paoguan")) {
            return (bone, vehicle, state) -> {
                var jiaojia = getAnimationProcessor().getBone("jiaojia");

                var entityData = state.getData(DataTickets.ENTITY_MODEL_DATA);
                if (entityData != null) {
                    bone.setRotX((entityData.headPitch()) * Mth.DEG_TO_RAD);
                    jiaojia.setRotX(-2 * ((entityData.headPitch() - (10 - entityData.headPitch() * 0.1f)) * Mth.DEG_TO_RAD));
                }
            };
        }

        if (boneName.equals("monitor")) {
            return (bone, vehicle, state) -> bone.setHidden(!vehicle.getEntityData().get(INTELLIGENT));
        }

        return super.collectTransform(boneName);
    }
}
