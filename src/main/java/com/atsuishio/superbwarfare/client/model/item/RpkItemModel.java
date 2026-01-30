package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.RpkItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class RpkItemModel extends CustomGunModel<RpkItem> {

    @Override
    public void setCustomAnimations(RpkItem animatable, long instanceId, AnimationState<RpkItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone button = getAnimationProcessor().getBone("button");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone base = getAnimationProcessor().getBone("base");
        CoreGeoBone bone171 = getAnimationProcessor().getBone("bone171");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone shuan = getAnimationProcessor().getBone("shuan");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fp = ClientEventHandler.boltMove;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);


        float posYAlt = switch (type) {
            case 2, 3 -> 0.5f;
            default -> 0f;
        };
        float posY = switch (type) {
            case 0 -> 1.071f;
            case 1 -> -0.101f;
            case 2 -> 0.11f + posYAlt;
            case 3 -> 0.099f + posYAlt;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0, 1 -> 0.7f;
            case 2 -> 0.74f;
            case 3 -> 0.8f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3.3f;
            case 1 -> 4.2f;
            case 2 -> 4.4f;
            case 3 -> 4.6f;
            default -> 0f;
        };

        gun.setPosX(2.462f * (float) zp);
        gun.setPosY((posY) * (float) zp - (float) (0.2f * zpz) - posYAlt);
        gun.setPosZ(posZ * (float) zp + (float) (0.5f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        scope.setScaleZ(1f - (0.85f * (float) zp));
        button.setScaleX(1f - (0.3f * (float) zp));
        button.setScaleY(1f - (0.3f * (float) zp));
        button.setScaleZ(1f - (0.3f * (float) zp));
        scope2.setScaleZ(1f - (0.7f * (float) zp));
        base.setScaleZ(1f - (0.4f * (float) zp));
        bone171.setScaleY(1f - (0.55f * (float) zp));
        scope3.setScaleZ(1f - (0.7f * (float) zp));

        CoreGeoBone shen;
        if (zt < 0.5) {
            shen = getAnimationProcessor().getBone("fireRootNormal");
        } else {
            shen = switch (type) {
                case 0 -> getAnimationProcessor().getBone("fireRoot0");
                case 1 -> getAnimationProcessor().getBone("fireRoot1");
                case 2 -> getAnimationProcessor().getBone("fireRoot2");
                case 3 -> getAnimationProcessor().getBone("fireRoot3");
                default -> getAnimationProcessor().getBone("fireRootNormal");
            };
        }

        ClientEventHandler.handleShootAnimation(shen, 1, -1, 1, 1, 1, 1, 0.5f, 0.8f);

        CrossHairOverlay.gunRot = shen.getRotZ();
        shuan.setPosZ(3f * (float) fp);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.98 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.35f);
    }
}
