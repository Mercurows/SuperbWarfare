package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.Qbz95Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class Qbz95ItemModel extends CustomGunModel<Qbz95Item> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    public static float rotXBipod = 0f;

    public static float lHandPosX = 0f;
    public static float lHandPosY = 0f;
    public static float lHandPosZ = 0f;
    public static float lHandRotX = 0f;
    public static float lHandRotY = 0f;
    public static float lHandRotZ = 0f;

    @Override
    public ResourceLocation getAnimationResource(Qbz95Item animatable) {
        return Mod.loc("animations/qbz_95.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Qbz95Item animatable) {
        return Mod.loc("geo/qbz_95.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Qbz95Item animatable) {
        return Mod.loc("textures/item/qbz_95.png");
    }

    @Override
    public void setCustomAnimations(Qbz95Item animatable, long instanceId, AnimationState<Qbz95Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone bolt = getAnimationProcessor().getBone("bolt2");
        CoreGeoBone button = getAnimationProcessor().getBone("button");
        CoreGeoBone button3 = getAnimationProcessor().getBone("button3");
        CoreGeoBone button6 = getAnimationProcessor().getBone("button6");
        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        float posYAlt = switch (type) {
            case 2 -> 0.85f;
            case 3 -> 0.9f;
            default -> 0f;
        };
        float posY = switch (type) {
            case 0 -> 0.535f;
            case 1 -> -0.155f;
            case 2 -> -0.975f + posYAlt;
            case 3 -> -0.64f + posYAlt;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 10.6f;
            case 1 -> 8.8f;
            case 2 -> 14.51f;
            case 3 -> 17.2f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.5f;
            case 1 -> 0.51f;
            case 2 -> 0.792f;
            case 3 -> 0.891f;
            default -> 0f;
        };

        gun.setPosX(3.71f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz) - posYAlt);
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));

        button.setScaleY(1f - (0.85f * (float) zp));
        button3.setScaleX(1f - (0.5f * (float) zp));
        button6.setScaleX(1f - (0.8f * (float) zp));

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

        fireRotY = (float) Mth.lerp(0.3f * times, fireRotY, 0.2f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.4f + 0.5 * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.15f * fp + 0.18f * fr));
        shen.setPosZ((float) (0.375 * fp + 0.44f * fr + 0.75 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.05f * fr + 0.01f * fpz));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        shen.setPosX((float) (shen.getPosX() * (1 - 0.1 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.8 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.1 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - (type == 3 ? 0.96 : type == 1 ? 0.8 : 0.9) * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - (type == 3 ? 0.95 : 0.9) * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.4 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();
        bolt.setPosZ(5f * (float) fp);

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        if (GunData.from(stack).holdOpen.get()) {
            bolt.setPosZ(5f);
        }

        CoreGeoBone flare = getAnimationProcessor().getBone("flare");
        flare.setPosZ(-2);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        float numR = (float) (1 - 0.975 * zt);
        float numP = (float) (1 - 0.97 * zt);

        CoreGeoBone leftHand = getAnimationProcessor().getBone("Lefthand");
        CoreGeoBone anim = getAnimationProcessor().getBone("anim");

        boolean isZooming = zt > 0 && anim.getPosZ() == 0;

        lHandPosX = Mth.lerp(1.5f * times, lHandPosX, isZooming ? 0 : leftHand.getPosX());
        lHandPosY = Mth.lerp(1.5f * times, lHandPosY, isZooming ? 0 : leftHand.getPosY());
        lHandPosZ = Mth.lerp(1.5f * times, lHandPosZ, isZooming ? 0 : leftHand.getPosZ());
        lHandRotX = Mth.lerp(1.5f * times, lHandRotX, isZooming ? -2.1f : leftHand.getRotX());
        lHandRotY = Mth.lerp(1.5f * times, lHandRotY, isZooming ? 0.2419f : leftHand.getRotY());
        lHandRotZ = Mth.lerp(1.5f * times, lHandRotZ, isZooming ? 2.9228f : leftHand.getRotZ());


        if (GunData.from(stack).reload.empty()) {
            leftHand.setPosX(lHandPosX);
            leftHand.setPosY(lHandPosY);
            leftHand.setPosZ(lHandPosZ);
            leftHand.setRotX(lHandRotX);
            leftHand.setRotY(lHandRotY);
            leftHand.setRotZ(lHandRotZ);
        }

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
