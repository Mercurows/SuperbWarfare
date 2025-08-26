package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.machinegun.MinigunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

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
    public ResourceLocation getLODModelResource(MinigunItem animatable) {
        return Mod.loc("geo/lod/minigun.geo.json");
    }

    @Override
    public ResourceLocation getLODTextureResource(MinigunItem animatable) {
        return Mod.loc("textures/item/lod/minigun.png");
    }

    @Override
    public void setCustomAnimations(MinigunItem animatable, long instanceId, AnimationState<MinigunItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("barrel");
        GeoBone shen = getAnimationProcessor().getBone("shen");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        var data = GunData.from(stack);
        int rpm = data.get(GunProp.RPM);

        float heat = (float) data.heat.get();

        for (int i = 1; i <= 6; i++) {
            GeoBone bone = getAnimationProcessor().getBone("barrel" + i + "_illuminated");
            bone.setScaleZ(heat / 2);
        }

        rotZ += times * -0.14f * ((float) rpm / 1200) * ClientEventHandler.shootDelay;
        gun.setRotZ(rotZ);

        ClientEventHandler.handleShootAnimation(shen, 1, -0.4f, 1.2f, 1.3f, 1, 1, 0f, 0.7f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 7, 1, 3, false);

        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
