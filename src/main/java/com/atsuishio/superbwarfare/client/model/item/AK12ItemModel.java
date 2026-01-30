package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.AK12Item;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class AK12ItemModel extends CustomGunModel<AK12Item> {

    public static float rotXBipod = 0f;

    @Override
    public void setCustomAnimations(AK12Item animatable, long instanceId, AnimationState<AK12Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope1 = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone frontSight = getAnimationProcessor().getBone("qianjimiao");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        float posY = switch (type) {
            case 0 -> 0.781f;
            case 1 -> 0.371f;
            case 2 -> -0.021f;
            case 3 -> -0.01f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.52f;
            case 1 -> 0.6f;
            case 2 -> 0.77f;
            case 3 -> 0.84f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3f;
            case 1 -> 3.4f;
            case 2 -> 4.1f;
            case 3 -> 5.14f;
            default -> 0f;
        };

        if (type == 2) {
            frontSight.setPosY(-0.4f * (float) zp);
        }

        gun.setPosX(1.96f * (float) zp);

        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.5f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        scope1.setScaleZ(1f - (0.4f * (float) zp));
        scope2.setScaleZ(1f - (0.3f * (float) zp));
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

        ClientEventHandler.handleShootAnimation(shen, 0.95f, -0.95f, 0.85f, 0.8f, 0.9f, 1, 0.5f, 0.8f);
        CoreGeoBone shuan = getAnimationProcessor().getBone("shuan");
        shuan.setPosZ(3f * (float) ClientEventHandler.boltMove);

        CrossHairOverlay.gunRot = shen.getRotZ();

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.94 * zt);
        float numP = (float) (1 - 0.8 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.35f);
    }
}
