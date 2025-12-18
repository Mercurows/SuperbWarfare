package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.handgun.TracheliumItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class TracheliumItemModel extends CustomGunModel<TracheliumItem> {

    public static float posYAlt = -0.83f;
    public static float scaleZAlt = 0.8f;
    public static float posZAlt = 13.7f;

    public static float rotXBipod = 0f;

    @Override
    public void setCustomAnimations(TracheliumItem animatable, long instanceId, AnimationState<TracheliumItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone hammer = getAnimationProcessor().getBone("jichui");
        CoreGeoBone lun = getAnimationProcessor().getBone("lun");
        CoreGeoBone barrel1 = getAnimationProcessor().getBone("Barrel1");
        CoreGeoBone barrel2 = getAnimationProcessor().getBone("Barrel2");
        CoreGeoBone flare = getAnimationProcessor().getBone("flare");
        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");

        float times = 0.4f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        var data = GunData.from(stack);
        int stockType = data.attachment.get(AttachmentType.STOCK);
        int barrelType = data.attachment.get(AttachmentType.BARREL);
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        int gripType = data.attachment.get(AttachmentType.GRIP);

        posYAlt = Mth.lerp(times, posYAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? -1.98f : -0.83f);
        scaleZAlt = Mth.lerp(times, scaleZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 0.4f : 0.8f);
        posZAlt = Mth.lerp(times, posZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 7.5f : 13.7f);

        float posY = switch (scopeType) {
            case 0, 3 -> 1.1f;
            case 1 -> -0.18f;
            case 2 -> posYAlt;
            default -> 0f;
        };
        float scaleZ = switch (scopeType) {
            case 0, 3 -> 0.2f;
            case 1 -> 0.6f;
            case 2 -> scaleZAlt;
            default -> 0f;
        };
        float posZ = switch (scopeType) {
            case 0, 3 -> 1f;
            case 1 -> 6f;
            case 2 -> posZAlt;
            default -> 0f;
        };

        float posZAlt = stockType == 2 ? 1 : 0;

        gun.setPosX((float) (3.48f * zp));
        gun.setPosY((float) (posY * zp - 0.2f * zpz));
        gun.setPosZ((float) (posZ * zp + 0.2f * zpz) + posZAlt);
        gun.setScaleZ((float) (1f - scaleZ * zp));

        scope2.setScaleZ(1f - (0.7f * (float) zp));

        CoreGeoBone shen;
        if (zt < 0.5) {
            shen = getAnimationProcessor().getBone("fireRootNormal");
        } else {
            shen = switch (scopeType) {
                case 0 -> getAnimationProcessor().getBone("fireRoot0");
                case 1 -> getAnimationProcessor().getBone("fireRoot1");
                case 2 -> getAnimationProcessor().getBone("fireRoot2");
                case 3 -> getAnimationProcessor().getBone("fireRoot3");
                default -> getAnimationProcessor().getBone("fireRootNormal");
            };
        }

        ClientEventHandler.handleShootAnimation(shen, 1.25f, -2f, 1.85f, 3.5f, 1.3f, 1f, 0.2f, 0.75f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        hammer.setRotX(50 * Mth.DEG_TO_RAD * (float) ClientEventHandler.revolverPreTime);
        lun.setRotZ(-60 * Mth.DEG_TO_RAD * (float) ClientEventHandler.revolverWheelPreTime);
        CoreGeoBone ammo = getAnimationProcessor().getBone("ammo");
        CoreGeoBone ammohole = getAnimationProcessor().getBone("ammohole");
        ammo.setRotZ(60 * Mth.DEG_TO_RAD * (float) ClientEventHandler.revolverWheelPreTime);
        ammohole.setRotZ(-60 * Mth.DEG_TO_RAD * (float) ClientEventHandler.revolverWheelPreTime);

        if (data.reload.empty()) {
            lun.setRotZ(0);
            ammo.setRotZ(0);
            ammohole.setRotZ(0);
        }

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 2, 0, 3, false);

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        float numR = (float) (1 - 0.22 * zt);
        float numP = (float) (1 - 0.48 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());

        barrel1.setPosZ((scopeType == 0 && gripType == 0) ? 17.9f : 0);
        barrel2.setPosZ((scopeType == 0 && gripType == 0) ? 15.3f : 3);

        float flarePosZ = 0;

        if (scopeType > 0 || gripType > 0) {
            if (barrelType == 1) {
                flarePosZ = -21;
            } else {
                flarePosZ = -18;
            }
        } else if (barrelType == 1) {
            flarePosZ = -3;
        }

        flare.setPosZ(flarePosZ);
    }
}
