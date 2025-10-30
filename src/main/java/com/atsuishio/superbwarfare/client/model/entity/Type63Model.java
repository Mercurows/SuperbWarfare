package com.atsuishio.superbwarfare.client.model.entity;

import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

import static com.atsuishio.superbwarfare.entity.vehicle.Type63Entity.LOADED_AMMO;

public class Type63Model extends VehicleModel<Type63Entity> {

    private final Pattern SHELL_PATTERN = Pattern.compile("^shell(?<id>\\d+)$");

    @Override
    public @Nullable TransformContext<Type63Entity> collectTransform(String boneName) {
        if (boneName.equals("shoulunx")) {
            return (bone, vehicle, state) -> bone.setRotX(-turretXRot * 3);
        }

        if (boneName.equals("shouluny")) {
            return (bone, vehicle, state) -> bone.setRotZ(-turretYRot * 6);
        }

        var matcher = SHELL_PATTERN.matcher(boneName);
        if (matcher.matches()) {
            return (bone, vehicle, state) -> {
                var items = vehicle.getEntityData().get(LOADED_AMMO);
                int i = Integer.parseInt(matcher.group("id"));
                bone.setHidden(items.get(i) == -1);
            };
        }

        return super.collectTransform(boneName);
    }
}
