package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

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
    public @Nullable TransformContext<TruckEntity> collectTransform(String boneName) {
        if (boneName.equals("control")) {
            return (control, vehicle, state) -> control.setRotY(12 * Mth.lerp(state.getPartialTick(), vehicle.rudderRotO, vehicle.getRudderRot()));
        }

        return super.collectTransform(boneName);
    }

    @Override
    public boolean hasWheel() {
        return true;
    }
}
