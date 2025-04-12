package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.machinegun.MinigunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class MinigunItemModel extends GeoModel<MinigunItem> {

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
    public void setCustomAnimations(MinigunItem animatable, long instanceId, AnimationState animationState) {
        GeoBone gun = getAnimationProcessor().getBone("barrel");
        GeoBone shen = getAnimationProcessor().getBone("shen");

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);


        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int rpm = GunData.from(stack).rpm();

        gun.setRotZ(gun.getRotZ() + times * -0.07f * ((float) rpm / 1200) * ClientEventHandler.miniGunRot);

        shen.setPosX((float) (0.75f * ClientEventHandler.recoilHorizon * fpz * fp));
        shen.setPosY((float) (-0.03f * fp - 0.06f * fr));
        shen.setPosZ((float) (0.625 * fp + 0.34f * fr + 1.15 * fpz));
        shen.setRotX((float) (0.02f * fp + 0.02f * fr + 0.02f * fpz));
        shen.setRotY((float) (0.02f * ClientEventHandler.recoilHorizon * fpz));
        shen.setRotZ((float) ((0.02f + 0.02 * fr) * ClientEventHandler.recoilHorizon));

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
