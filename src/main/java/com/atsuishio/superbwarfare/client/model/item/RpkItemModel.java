package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.machinegun.RpkItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class RpkItemModel extends GeoModel<RpkItem> {

    public static float scopeY = 0.2f;
    public static float scaleZAlt = 0.74f;
    public static float posZAlt = 4.25f;

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;

    @Override
    public ResourceLocation getAnimationResource(RpkItem animatable) {
        return Mod.loc("animations/ak.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RpkItem animatable) {
        return Mod.loc("geo/rpk.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpkItem animatable) {
        return Mod.loc("textures/item/rpk.png");
    }

    @Override
    public void setCustomAnimations(RpkItem animatable, long instanceId, AnimationState animationState) {
        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone scope = getAnimationProcessor().getBone("Scope1");
        GeoBone button = getAnimationProcessor().getBone("button");
        GeoBone scope2 = getAnimationProcessor().getBone("Scope2");
//        GeoBone scope3 = getAnimationProcessor().getBone("Scope3");
//        GeoBone cross3 = getAnimationProcessor().getBone("Cross3");
        GeoBone shuan = getAnimationProcessor().getBone("shuan");

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
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

        var data = GunData.from(stack);
        var tag = data.tag();
        int type = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);
        scopeY = Mth.lerp(times, scopeY, tag.getBoolean("ScopeAlt") ? -0.7f : 0.2f);
        scaleZAlt = Mth.lerp(times, scaleZAlt, tag.getBoolean("ScopeAlt") ? 0.45f : 0.74f);
        posZAlt = Mth.lerp(times, posZAlt, tag.getBoolean("ScopeAlt") ? 3.3f : 4.25f);


        float posYAlt = switch (type) {
            case 2, 3 -> 0.5f;
            default -> 0f;
        };
        float posY = switch (type) {
            case 0 -> 1.071f;
            case 1 -> -0.101f;
            case 2 -> scopeY + posYAlt;
            case 3 -> 0.099f + posYAlt;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0, 1 -> 0.7f;
            case 2 -> scaleZAlt;
            case 3 -> 0.78f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3.3f;
            case 1 -> 4.2f;
            case 2 -> posZAlt;
            case 3 -> 3.9f;
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
//        scope3.setScaleZ(1f - (0.7f * (float) zp));

        tag.putBoolean("HoloHidden", !(gun.getPosX() > 1.8));
        data.save();

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
        shen.setRotX((float) (0.01f * fp + 0.08f * fr + 0.01f * fpz));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        shen.setPosX((float) (shen.getPosX() * (1 - 0.4 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.8 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.6 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.9 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.85 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.4 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        shuan.setPosZ(2.4f * (float) fp);

        GeoBone root = getAnimationProcessor().getBone("root");
        root.setPosX((float) (movePosX + 20 * ClientEventHandler.drawTime + 9.3f * mph));
        root.setPosY((float) (swayY + movePosY - 40 * ClientEventHandler.drawTime - 2f * vY));
        root.setRotX((float) (swayX - Mth.DEG_TO_RAD * 60 * ClientEventHandler.drawTime + Mth.DEG_TO_RAD * turnRotX - 0.15f * vY));
        root.setRotY((float) (0.2f * movePosX + Mth.DEG_TO_RAD * 300 * ClientEventHandler.drawTime + Mth.DEG_TO_RAD * turnRotY));
        root.setRotZ((float) (0.2f * movePosX + moveRotZ + Mth.DEG_TO_RAD * 90 * ClientEventHandler.drawTime + 2.7f * mph + Mth.DEG_TO_RAD * turnRotZ));

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.98 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
//        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.35f);
    }
}
