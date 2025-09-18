package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.animation.AnimationHelper;
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
    public ResourceLocation getLODModelResource(RpgItem animatable) {
        return Mod.loc("geo/lod/rpg.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(RpgItem animatable) {
        return Mod.loc("textures/item/lod/rpg.png");
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

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        ClientEventHandler.handleShootAnimation(shen, 1, -0.4f, 1.2f, 1.3f, 1, 1, 0.5f, 0.7f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        gun.setPosX(0.91f * (float) zp);
        gun.setPosY(-0.04f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(2f * (float) zp + (float) (0.15f * zpz));
        gun.setRotZ(0.45f * (float) zp + (float) (0.02f * zpz));
        gun.setScaleZ(1f - (0.5f * (float) zp));

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, true);

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.82 * zt);
        float numP = (float) (1 - 0.78 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
