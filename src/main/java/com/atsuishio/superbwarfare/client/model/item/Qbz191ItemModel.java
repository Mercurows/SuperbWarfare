package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.Qbz191Item;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class Qbz191ItemModel extends CustomGunModel<Qbz191Item> {

    public static float rotXSight = 0f;
    public static float rotXBipod = 0f;

    @Override
    public void setCustomAnimations(Qbz191Item animatable, long instanceId, AnimationState<Qbz191Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone sight1fold = getAnimationProcessor().getBone("sight1fold");
        CoreGeoBone sight2fold = getAnimationProcessor().getBone("sight2fold");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        rotXSight = Mth.lerp(1.5f * times, rotXSight, type == 0 ? 0 : 90);

        float posY = switch (type) {
            case 0 -> 0.52f;
            case 1 -> 0.63f;
            case 2 -> 0.43f;
            case 3 -> 0.27f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0, 1 -> 0.5f;
            case 2 -> 0.85f;
            case 3 -> 0.94f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3.4f;
            case 1 -> 3.5f;
            case 2 -> 6.6f;
            case 3 -> 6.4f;
            default -> 0f;
        };

        sight1fold.setRotX(rotXSight * Mth.DEG_TO_RAD);
        sight2fold.setRotX(rotXSight * Mth.DEG_TO_RAD);

        gun.setPosX(3.21f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.2f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        gun.setRotZ((float) (0.05f * zpz));
        scope.setScaleZ(1f - (0.4f * (float) zp));
        scope2.setScaleZ(1f - (0.27f * (float) zp));
        scope3.setScaleZ(1f + (0.2f * (float) zp));

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

        ClientEventHandler.handleShootAnimation(shen, 0.95f, -0.95f, 0.85f, 0.8f, 0.9f, 1, 0.5f, 0.75f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        CoreGeoBone bolt = getAnimationProcessor().getBone("bolt");
        bolt.setPosZ(3f * (float) ClientEventHandler.boltMove);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 2, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.985 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.55f);
    }
}
