package com.atsuishio.superbwarfare.compat.realcamera;

import com.atsuishio.superbwarfare.compat.CompatHolder;
import com.xtracr.realcamera.RealCameraCore;
import com.xtracr.realcamera.util.CrosshairUtil;
import net.minecraftforge.fml.ModList;

public class RealCameraCompatHolder {

    public static boolean hasMod() {
        return ModList.get().isLoaded(CompatHolder.REALCAMERA);
    }

    public static float getCompatMoveX(float moveX) {
        if (RealCameraCore.isActive()) {
            moveX += (float) CrosshairUtil.offset.x();
        }
        return moveX;
    }

    public static float getCompatMoveY(float moveY) {
        if (RealCameraCore.isActive()) {
            moveY -= (float) CrosshairUtil.offset.y();
        }
        return moveY;
    }
}
