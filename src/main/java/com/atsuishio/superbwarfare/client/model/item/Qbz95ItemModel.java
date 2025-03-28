package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.rifle.Qbz95Item;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class Qbz95ItemModel extends GeoModel<Qbz95Item> {

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
        return ModUtils.loc("animations/qbz95.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Qbz95Item animatable) {
        return ModUtils.loc("geo/qbz95.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Qbz95Item animatable) {
        return ModUtils.loc("textures/item/qbz95.png");
    }

    @Override
    public void setCustomAnimations(Qbz95Item animatable, long instanceId, AnimationState animationState) {
        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone bolt = getAnimationProcessor().getBone("bolt2");
        GeoBone button = getAnimationProcessor().getBone("button");
        GeoBone button3 = getAnimationProcessor().getBone("button3");
        GeoBone button6 = getAnimationProcessor().getBone("button6");
        GeoBone cross1 = getAnimationProcessor().getBone("Cross1");
        GeoBone cross2 = getAnimationProcessor().getBone("Cross2");
        GeoBone cross3 = getAnimationProcessor().getBone("Cross3");
        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getGameTimeDeltaTicks(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double swayX = ClientEventHandler.swayX;
        double swayY = ClientEventHandler.swayY;
        float moveRotZ = (float) ClientEventHandler.moveRotZ;
        float movePosX = (float) ClientEventHandler.movePosX;
        float movePosY = (float) ClientEventHandler.movePosY;
        double mph = ClientEventHandler.movePosHorizon;
        double vY = ClientEventHandler.velocityY;
        double turnRotX = ClientEventHandler.turnRot[0];
        double turnRotY = ClientEventHandler.turnRot[1];
        double turnRotZ = ClientEventHandler.turnRot[2];
        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);

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

        NBTTool.getTag(stack).putBoolean("HoloHidden", gun.getPosX() < 3.1);

        GeoBone shen;
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

        cross1.setPosY(-0.75f * (float) fpz);
        cross2.setPosY(-0.7f * (float) fpz);
        cross3.setPosY(-0.2f * (float) fpz);

        GeoBone l = getAnimationProcessor().getBone("l");
        GeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        if (GunsTool.getGunBooleanTag(stack, "HoldOpen")) {
            bolt.setPosZ(5f);
        }

        GeoBone flare = getAnimationProcessor().getBone("flare");
        int BarrelType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.BARREL);

        if (BarrelType == 1) {
            flare.setPosZ(-2);
        }

        GeoBone root = getAnimationProcessor().getBone("root");
        root.setPosX((float) (movePosX + 20 * ClientEventHandler.drawTime + 9.3f * mph));
        root.setPosY((float) (swayY + movePosY - 40 * ClientEventHandler.drawTime - 2f * vY));
        root.setRotX((float) (swayX - Mth.DEG_TO_RAD * 60 * ClientEventHandler.drawTime + Mth.DEG_TO_RAD * turnRotX - 0.15f * vY));
        root.setRotY((float) (0.2f * movePosX + Mth.DEG_TO_RAD * 300 * ClientEventHandler.drawTime + Mth.DEG_TO_RAD * turnRotY));
        root.setRotZ((float) (0.2f * movePosX + moveRotZ + Mth.DEG_TO_RAD * 90 * ClientEventHandler.drawTime + 2.7f * mph + Mth.DEG_TO_RAD * turnRotZ));

        float numR = (float) (1 - 0.975 * zt);
        float numP = (float) (1 - 0.97 * zt);

        GeoBone leftHand = getAnimationProcessor().getBone("Lefthand");
        GeoBone anim = getAnimationProcessor().getBone("anim");

        boolean isZooming = zt > 0 && anim.getPosZ() == 0;

        lHandPosX = Mth.lerp(1.5f * times, lHandPosX, isZooming ? 0 : leftHand.getPosX());
        lHandPosY = Mth.lerp(1.5f * times, lHandPosY, isZooming ? 0 : leftHand.getPosY());
        lHandPosZ = Mth.lerp(1.5f * times, lHandPosZ, isZooming ? 0 : leftHand.getPosZ());
        lHandRotX = Mth.lerp(1.5f * times, lHandRotX, isZooming ? -2.1f : leftHand.getRotX());
        lHandRotY = Mth.lerp(1.5f * times, lHandRotY, isZooming ? 0.2419f : leftHand.getRotY());
        lHandRotZ = Mth.lerp(1.5f * times, lHandRotZ, isZooming ? 2.9228f : leftHand.getRotZ());


        if (NBTTool.getTag(stack).getBoolean("is_empty_reloading")) {
            leftHand.setPosX(lHandPosX);
            leftHand.setPosY(lHandPosY);
            leftHand.setPosZ(lHandPosZ);
            leftHand.setRotX(lHandRotX);
            leftHand.setRotY(lHandRotY);
            leftHand.setRotZ(lHandRotZ);
        }

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
