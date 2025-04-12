package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.special.BocekItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class BocekItemModel extends GeoModel<BocekItem> {

    @Override
    public ResourceLocation getAnimationResource(BocekItem animatable) {
        return Mod.loc("animations/bocek.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(BocekItem animatable) {
        return Mod.loc("geo/bocek.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BocekItem animatable) {
        return Mod.loc("textures/item/bocek.png");
    }

    @Override
    public void setCustomAnimations(BocekItem animatable, long instanceId, AnimationState animationState) {
        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone arrow = getAnimationProcessor().getBone("arrow");
        GeoBone rh = getAnimationProcessor().getBone("ys");
        GeoBone lun = getAnimationProcessor().getBone("hualun1");
        GeoBone shen = getAnimationProcessor().getBone("shen");
        GeoBone shen_pos = getAnimationProcessor().getBone("shen_pos");
        GeoBone xian = getAnimationProcessor().getBone("xian1");
        GeoBone xian2 = getAnimationProcessor().getBone("xian2");
        GeoBone fire = getAnimationProcessor().getBone("fire");
        GeoBone deng = getAnimationProcessor().getBone("deng");
        GeoBone deng2 = getAnimationProcessor().getBone("deng2");
        GeoBone deng3 = getAnimationProcessor().getBone("deng3");
        GeoBone lh = getAnimationProcessor().getBone("lh");
        GeoBone r = getAnimationProcessor().getBone("r");

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;
        double pp = ClientEventHandler.pullPos;
        double bp = ClientEventHandler.bowPos;
        double hp = ClientEventHandler.handPos;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;


        arrow.setPosZ(9f * (float) bp);
        rh.setPosZ(9f * (float) hp);
        rh.setRotZ((float) (160 * Mth.DEG_TO_RAD + 45 * Mth.DEG_TO_RAD * ClientEventHandler.handTimer));
        lun.setRotX(1.6f * (float) bp);

        xian.setRotX(0.56f * (float) bp);
        xian2.setRotX(-0.56f * (float) bp);
        xian.setScaleY(1f + (0.25f * (float) bp));
        xian2.setScaleY(1f + (0.25f * (float) bp));
        xian.setPosZ(9f * (float) bp);
        xian2.setPosZ(9f * (float) bp);

        gun.setScaleZ(1f - (0.2f * (float) pp));
        gun.setRotZ((float) (-0.1 + 0.2f * pp + 0.07142857142857143f * ClientEventHandler.pullTimer));
        gun.setRotX(0.01f * (float) pp);
        gun.setPosZ((float) (3 + -3f * pp - 2.142857142857143 * ClientEventHandler.pullTimer));
        gun.setPosY(0.1f * (float) pp);
        r.setScaleZ(1f - (0.2f * (float) pp));
        deng2.setRotX(1.6f * (float) bp);
        deng2.setPosZ(0.05f * (float) bp);
        deng3.setRotX(-1.6f * (float) bp);
        deng3.setPosZ(0.05f * (float) bp);
        deng.setScaleZ(1f + (0.07f * (float) bp));

        lh.setRotX(0.2f * (float) zp);
        shen_pos.setPosX(-3.64f * (float) zp);
        shen_pos.setPosY(6.46f * (float) zp - (float) (0.2f * zpz));
        shen_pos.setPosZ(6.4f * (float) zp + (float) (0.3f * zpz));
        r.setScaleZ(1f - (0.31f * (float) zp));
        shen.setRotZ(60 * Mth.DEG_TO_RAD * (float) zp + (float) (0.05f * zpz) - 0.2f);

        fire.setPosX((float) (0.75f * ClientEventHandler.recoilHorizon * fpz * fp));
        fire.setPosY((float) (-0.03f * fp - 0.06f * fr));
        fire.setPosZ((float) (0.625 * fp + 0.34f * fr + 0.95 * fpz));
        fire.setRotX((float) (0.001f * fp + 0.001f * fr + 0.001f * fpz));
        fire.setRotY((float) (0.01f * ClientEventHandler.recoilHorizon * fpz));
        fire.setRotZ((float) ((0.02f + 0.1 * fr) * ClientEventHandler.recoilHorizon));

        CrossHairOverlay.gunRot = fire.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
