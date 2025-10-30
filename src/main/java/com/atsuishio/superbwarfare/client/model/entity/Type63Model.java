package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.LOADED_AMMO;

public class Type63Model extends VehicleModel<Type63Entity> {

    @Override
    public void setCustomAnimations(Type63Entity vehicle, long instanceId, AnimationState<Type63Entity> animationState) {
        super.setCustomAnimations(vehicle, instanceId, animationState);

        this.getAnimationProcessor().getRegisteredBones().forEach(bone -> {
            var name = bone.getName();

            if (name.equals("shoulunx")) {
                bone.setRotX(-turretXRot * 3);
            }

            if (name.equals("shouluny")) {
                bone.setRotZ(-turretYRot * 6);
            }

            if (name.startsWith("shell") && name.length() > 5) {
                var items = vehicle.getEntityData().get(LOADED_AMMO);
                int i = Integer.parseInt(name.substring(5));
                bone.setHidden(items.get(i) == -1);
            }
        });
    }

    @Override
    public boolean hasWheel() {
        return true;
    }
}
