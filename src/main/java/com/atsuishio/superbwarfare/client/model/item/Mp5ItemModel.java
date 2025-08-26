package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.smg.Mp5Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Mp5ItemModel extends CustomGunModel<Mp5Item> {

    @Override
    public ResourceLocation getAnimationResource(Mp5Item animatable) {
        return Mod.loc("animations/mp_5.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Mp5Item animatable) {
        return Mod.loc("geo/mp_5.geo.json");
    }

    @Override
    public ResourceLocation getLODModelResource(Mp5Item animatable) {
        return Mod.loc("geo/lod/mp_5.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Mp5Item animatable) {
        return Mod.loc("textures/item/mp_5.png");
    }

    @Override
    public ResourceLocation getLODTextureResource(Mp5Item animatable) {
        return Mod.loc("textures/item/lod/mp_5.png");
    }

    @Override
    public void setCustomAnimations(Mp5Item animatable, long instanceId, AnimationState<Mp5Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone scope2 = getAnimationProcessor().getBone("Scope2");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 15 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        float posY = switch (type) {
            case 1 -> 0.37f;
            case 2 -> -0.62f;
            default -> 1.07f;
        };
        float scaleZ = switch (type) {
            case 0, 1 -> 0.3f;
            case 2 -> 0.8f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 4f;
            case 1 -> 5.1f;
            case 2 -> 8f;
            default -> 0f;
        };

        gun.setPosX(2.71f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        scope2.setScaleZ(1f - (0.8f * (float) zp));

        GeoBone shen;
        if (zt < 0.5) {
            shen = getAnimationProcessor().getBone("fireRootNormal");
        } else {
            shen = switch (type) {
                case 0 -> getAnimationProcessor().getBone("fireRoot0");
                case 1 -> getAnimationProcessor().getBone("fireRoot1");
                case 2 -> getAnimationProcessor().getBone("fireRoot2");
                default -> getAnimationProcessor().getBone("fireRootNormal");
            };
        }

        ClientEventHandler.handleShootAnimation(shen, 0.45f, -0.95f, 0.85f, 0.8f, 0.5f, 0.3f, 0.5f, 0.85f);

        CrossHairOverlay.gunRot = shen.getRotZ();

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.92 * zt);
        float numP = (float) (1 - 0.88 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1.2f, 0.45f);
    }
}
