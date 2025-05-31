package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.MarlinItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class MarlinItemModel extends CustomGunModel<MarlinItem> {

    @Override
    public ResourceLocation getAnimationResource(MarlinItem animatable) {
        return Mod.loc("animations/marlin.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MarlinItem animatable) {
        return Mod.loc("geo/marlin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MarlinItem animatable) {
        return Mod.loc("textures/item/marlin.png");
    }

    @Override
    public void setCustomAnimations(MarlinItem animatable, long instanceId, AnimationState<MarlinItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");
        CoreGeoBone jichui = getAnimationProcessor().getBone("jichui");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 7 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        gun.setPosX(1.712f * (float) zp);
        gun.setPosY(1.06f * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(3f * (float) zp + (float) (0.2f * zpz));
        gun.setRotZ((float) (0.02f * zpz));
        gun.setScaleZ(1f - (0.2f * (float) zp));

        shen.setPosX((float) (0.95f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (0.4f * fp + 0.44f * fr));
        shen.setPosZ((float) (2.825 * fp + 0.17f * fr + 1.175 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.15f * fr + 0.01f * fpz));
        shen.setRotY((float) (0.1f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.08f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        shen.setPosX((float) (shen.getPosX() * (1 - 0.4 * zt)));
        shen.setPosY((float) (shen.getPosY() * (1 - 0.5 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.6 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.87 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.7 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.65 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        if (GunData.from(stack).isEmpty.get()) {
            jichui.setRotX(-0.52f);
        }

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.55 * zt);
        float numP = (float) (1 - 0.88 * zt);

        if (GunData.from(stack).reloading()) {
            main.setRotX(numR * main.getRotX());
            main.setRotY(numR * main.getRotY());
            main.setRotZ(numR * main.getRotZ());
            main.setPosX(numP * main.getPosX());
            main.setPosY(numP * main.getPosY());
            main.setPosZ(numP * main.getPosZ());
            camera.setRotX(numR * camera.getRotX());
            camera.setRotY(numR * camera.getRotY());
            camera.setRotZ(numR * camera.getRotZ());
        }
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
