package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.smg.VectorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class VectorItemModel extends CustomGunModel<VectorItem> {
    public static float rotXSight = 0f;

    @Override
    public void setCustomAnimations(VectorItem animatable, long instanceId, AnimationState<VectorItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone kmj = getAnimationProcessor().getBone("kuaimanji");
        CoreGeoBone sight1fold = getAnimationProcessor().getBone("SightFold1");
        CoreGeoBone sight2fold = getAnimationProcessor().getBone("SightFold2");

        var data = GunData.from(stack);

        switch (data.selectedFireModeInfo().mode) {
            case SEMI -> kmj.setRotX(-120 * Mth.DEG_TO_RAD);
            case BURST -> kmj.setRotX(-60 * Mth.DEG_TO_RAD);
            case AUTO -> kmj.setRotX(0);
        }

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        float posY = switch (type) {
            case 1 -> 0.74f;
            case 2 -> 0.12f;
            default -> 0.07f;
        };

        gun.setPosX(2.356f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ((type == 2 ? 6 : 5) * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (0.5f * (float) zp));
        scope.setScaleZ(1f - (0.2f * (float) zp));

        CoreGeoBone shen;
        if (zt < 0.5) {
            shen = getAnimationProcessor().getBone("fireRootNormal");
        } else {
            shen = switch (type) {
                case 0 -> getAnimationProcessor().getBone("fireRoot0");
                case 1 -> getAnimationProcessor().getBone("fireRoot1");
                case 2 -> getAnimationProcessor().getBone("fireRoot2");
                default -> getAnimationProcessor().getBone("fireRootNormal");
            };
        }

        ClientEventHandler.handleShootAnimation(shen, 1f, -0.75f, 1.2f, 0.3f, 1.6f, 1f, 0.5f, 0.7f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        rotXSight = Mth.lerp(1.5f * times, rotXSight, type == 0 ? 0 : 90);
        sight1fold.setRotX(rotXSight * Mth.DEG_TO_RAD);
        sight2fold.setRotX(rotXSight * Mth.DEG_TO_RAD);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 1, 0, 3, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.92 * zt);
        float numP = (float) (1 - 0.88 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1.2f, 0.45f);
    }
}
