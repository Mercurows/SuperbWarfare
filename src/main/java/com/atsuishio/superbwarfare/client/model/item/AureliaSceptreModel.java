package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.handgun.AureliaSceptre;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.*;

public class AureliaSceptreModel extends CustomGunModel<AureliaSceptre> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    public static float firePosMove = 0f;

    @Override
    public ResourceLocation getAnimationResource(AureliaSceptre animatable) {
        return Mod.loc("animations/aurelia_sceptre.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(AureliaSceptre animatable) {
        return Mod.loc("geo/aurelia_sceptre.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AureliaSceptre animatable) {
        return Mod.loc("textures/item/aurelia_sceptre.png");
    }

    @Override
    public void setCustomAnimations(AureliaSceptre animatable, long instanceId, AnimationState<AureliaSceptre> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        float times = 0.2f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone rootLeftHand = getAnimationProcessor().getBone("rootLeftHand");

        firePosMove = Mth.lerp((holdFire ? 5 : 2) * times, firePosMove, holdFire ? 1 : 0);

        rootLeftHand.setPosX((float) (-movePosX + 20 * drawTime + 9.3f * movePosHorizon));
        rootLeftHand.setPosY((float) (swayY - movePosY - 40 * drawTime - 2f * velocityY + 1 * firePosMove));
        rootLeftHand.setPosZ(-6 * firePosMove);
        rootLeftHand.setRotX((float) (swayX - Mth.DEG_TO_RAD * 60 * drawTime + Mth.DEG_TO_RAD * turnRot[0] - 0.15f * velocityY + 4.7144 * Mth.DEG_TO_RAD * firePosMove));
        rootLeftHand.setRotY((float) (0.2f * movePosX + Mth.DEG_TO_RAD * 300 * drawTime + Mth.DEG_TO_RAD * turnRot[1] + 5 * Mth.DEG_TO_RAD * firePosMove));
        rootLeftHand.setRotZ((float) (0.2f * movePosX + Mth.DEG_TO_RAD * 90 * drawTime + 2.7f * movePosHorizon + Mth.DEG_TO_RAD * turnRot[2] + -0.0102 * Mth.DEG_TO_RAD * firePosMove));

        GeoBone guashi = getAnimationProcessor().getBone("guashi");
        guashi.setPosZ((float) (-0.5f * velocityY));
        guashi.setPosY((float) (-1.5f * velocityY));

        GeoBone shuimu = getAnimationProcessor().getBone("shuimu");
        shuimu.setScaleZ((float) Mth.clamp(1 - 1.5f * velocityY, 0.5, 1.5));

        GeoBone camera = getAnimationProcessor().getBone("camera");
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
