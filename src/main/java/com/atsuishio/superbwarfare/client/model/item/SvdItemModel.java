package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.SvdItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class SvdItemModel extends CustomGunModel<SvdItem> {
    public static float rotXBipod = 0f;

    @Override
    public void setCustomAnimations(SvdItem animatable, long instanceId, AnimationState<SvdItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone bolt = getAnimationProcessor().getBone("bolt");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fp = ClientEventHandler.boltMove;

        var data = GunData.from(stack);
        int type = data.attachment.get(AttachmentType.SCOPE);

        float posX = switch (type) {
            case 0, 1 -> 1.701f;
            case 2 -> 1.531f;
            case 3 -> 1.708f;
            default -> 0f;
        };
        float posY = switch (type) {
            case 0 -> 1.02f;
            case 1 -> 0.04f;
            case 2 -> 0.12f;
            case 3 -> -0.13f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.4f;
            case 1 -> 0.45f;
            case 2 -> 0.85f;
            case 3 -> 0.95f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 7f;
            case 1 -> 7.5f;
            case 2 -> 12.85f;
            case 3 -> 14.08f;
            default -> 0f;
        };

        gun.setPosX(posX * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));

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

        ClientEventHandler.handleShootAnimation(shen, 0.8f, 0.5f, 2f, 1.5f, 2f, 1.4f, 0.4f, 0.7f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        bolt.setPosZ(4.5f * (float) fp);

        if (GunData.from(stack).holdOpen.get()) {
            bolt.setPosZ(3.5f);
        }

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.96 * zt);
        float numP = (float) (1 - 0.9 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.65f);
    }
}
