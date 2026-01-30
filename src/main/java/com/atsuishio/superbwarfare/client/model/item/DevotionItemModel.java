package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.DevotionItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class DevotionItemModel extends CustomGunModel<DevotionItem> {

    @Override
    public ResourceLocation getAnimationResource(DevotionItem animatable) {
        return Mod.loc("animations/devotion.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(DevotionItem animatable) {
        return Mod.loc("geo/devotion.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DevotionItem animatable) {
        return Mod.loc("textures/item/devotion.png");
    }

    @Override
    public ResourceLocation getLODModelResource(DevotionItem animatable) {
        return Mod.loc("geo/lod/devotion.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(DevotionItem animatable) {
        return Mod.loc("textures/item/lod/devotion.png");
    }

    @Override
    public void setCustomAnimations(DevotionItem animatable, long instanceId, AnimationState<DevotionItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        CoreGeoBone bolt = getAnimationProcessor().getBone("bolt2");

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double fp = ClientEventHandler.boltMove;

        gun.setPosX(2.17f * (float) zp);
        gun.setPosY(0.17f * (float) zp - (float) (0.5f * zpz));
        gun.setPosZ(8.8f * (float) zp + (float) (0.6f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (0.7f * (float) zp));

        CoreGeoBone shen = getAnimationProcessor().getBone("shen");

        ClientEventHandler.handleShootAnimation(shen, 1, -1, 1, 0.6f, 1, 1, 0.7f, 0.8f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        bolt.setPosZ(-2f * (float) fp);

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        if (isProne(player)) {
            l.setRotX(1.5f);
            r.setRotX(1.5f);
        }

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.82 * zt);
        float numP = (float) (1 - 0.78 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.55f);
    }
}
