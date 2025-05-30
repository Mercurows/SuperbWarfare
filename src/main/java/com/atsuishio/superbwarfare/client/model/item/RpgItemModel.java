package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.launcher.RpgItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class RpgItemModel extends CustomGunModel<RpgItem> {

    @Override
    public ResourceLocation getAnimationResource(RpgItem animatable) {
        return Mod.loc("animations/rpg.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(RpgItem animatable) {
        return Mod.loc("geo/rpg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RpgItem animatable) {
        return Mod.loc("textures/item/rpg.png");
    }

    @Override
    public void setCustomAnimations(RpgItem animatable, long instanceId, AnimationState<RpgItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("rpg");
        CoreGeoBone hammer = getAnimationProcessor().getBone("hammer");

        if (GunData.from(stack).closeHammer.get()) {
            hammer.setRotX(-90 * Mth.DEG_TO_RAD);
        }

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        shen.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (0.4f * fp + 0.44f * fr));
        shen.setPosZ((float) (5.825 * fp + 0.34f * fr + 2.35 * fpz));
        shen.setRotX((float) (0.02f * fp + 0.25f * fr + 0.01f * fpz));
        shen.setRotY((float) (0.1f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        shen.setPosX((float) (shen.getPosX() * (1 - 0.4 * zt)));
        shen.setPosY((float) (shen.getPosY() * (1 - 0.5 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.7 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.87 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.7 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.65 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        gun.setPosX(0.91f * (float) zp);
        gun.setPosY(-0.04f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(2f * (float) zp + (float) (0.15f * zpz));
        gun.setRotZ(0.45f * (float) zp + (float) (0.02f * zpz));
        gun.setScaleZ(1f - (0.5f * (float) zp));

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.82 * zt);
        float numP = (float) (1 - 0.78 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
