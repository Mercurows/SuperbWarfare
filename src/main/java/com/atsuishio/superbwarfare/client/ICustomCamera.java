package com.atsuishio.superbwarfare.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;

public interface ICustomCamera {

    static ICustomCamera of(Camera camera) {
        return (ICustomCamera) camera;
    }

    static Quaternionf getCameraRotation() {
        var mc = Minecraft.getInstance();
        return of(mc.gameRenderer.getMainCamera()).superbwarfare$getRotation();
    }

    Quaternionf superbwarfare$getRotation();
}
