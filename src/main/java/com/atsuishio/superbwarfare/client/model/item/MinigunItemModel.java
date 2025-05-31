package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.MinigunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

public class MinigunItemModel extends CustomGunModel<MinigunItem> {

    private static float rotZ = 0.0f;

    @Override
    public ResourceLocation getAnimationResource(MinigunItem animatable) {
        return Mod.loc("animations/minigun.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MinigunItem animatable) {
        return Mod.loc("geo/minigun.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MinigunItem animatable) {
        return Mod.loc("textures/item/minigun.png");
    }

    @Override
    public void setCustomAnimations(MinigunItem animatable, long instanceId, AnimationState<MinigunItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("barrel");
        CoreGeoBone shen = getAnimationProcessor().getBone("shen");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        var data = GunData.from(stack);
        int rpm = data.rpm();

        float heat = (float) data.heat.get();

        for (int i = 1; i <= 6; i++) {
            CoreGeoBone bone = getAnimationProcessor().getBone("barrel" + i + "_illuminated");
            bone.setScaleZ(heat / 2);
        }

        rotZ += times * -0.14f * ((float) rpm / 1200) * ClientEventHandler.shootDelay;
        gun.setRotZ(rotZ);

        shen.setPosX((float) (0.75f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (-0.03f * fp - 0.06f * fr));
        shen.setPosZ((float) (0.625 * fp + 0.34f * fr + 1.15 * fpz));
        shen.setRotX((float) (0.02f * fp + 0.02f * fr + 0.02f * fpz));
        shen.setRotY((float) (0.02f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.02f + 0.02 * fr) * ClientEventHandler.recoilHorizon));

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
