package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.Mk14Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class Mk14ItemModel extends CustomGunModel<Mk14Item> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    public static float rotXBipod = 0f;

    @Override
    public ResourceLocation getAnimationResource(Mk14Item animatable) {
        return Mod.loc("animations/mk_14.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Mk14Item animatable) {
        return Mod.loc("geo/mk_14.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Mk14Item animatable) {
        return Mod.loc("textures/item/mk_14.png");
    }

    @Override
    public void setCustomAnimations(Mk14Item animatable, long instanceId, AnimationState<Mk14Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bones");
        CoreGeoBone action = getAnimationProcessor().getBone("action");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 13 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        int stockType = GunData.from(stack).attachment.get(AttachmentType.STOCK);
        int barrelType = GunData.from(stack).attachment.get(AttachmentType.BARREL);
        int gripType = GunData.from(stack).attachment.get(AttachmentType.GRIP);

        float posY = switch (type) {
            case 0 -> 1.68f;
            case 1 -> 0.23f;
            case 2 -> -0.05f;
            case 3 -> -0.07f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.4f;
            case 1 -> 0.5f;
            case 2 -> 0.8f;
            case 3 -> 0.84f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 2f;
            case 1 -> 3f;
            case 2 -> 5f;
            case 3 -> 5.2f;
            default -> 0f;
        };

        gun.setPosX(2.714f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        scope2.setScaleZ(1f - (0.8f * (float) zp));
        scope3.setScaleZ(1f - (0.55f * (float) zp));

        CoreGeoBone shen;
        if (zt < 0.5) {
            shen = getAnimationProcessor().getBone("fireRootNormal");
        } else {
            shen = switch (type) {
                case 0 -> getAnimationProcessor().getBone("fireRoot0");
                case 1 -> getAnimationProcessor().getBone("fireRoot1");
                case 2 -> getAnimationProcessor().getBone("fireRoot2");
                case 3 -> getAnimationProcessor().getBone("fireRoot3");
                default -> getAnimationProcessor().getBone("fireRootNormal");
            };
        }

        fireRotY = (float) Mth.lerp(0.3f * times, fireRotY, 0.2f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.4f + 0.5 * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.15f * fp + 0.18f * fr));
        shen.setPosZ((float) (1.935 * fp + 0.13f * fr + 0.725 * fpz));
        shen.setRotX((float) ((0.015f * fp + 0.12f * fr + 0.015f * fpz)));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        shen.setPosX((float) (shen.getPosX() * (1 - 0.4 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.8 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.6 * zt) * (barrelType == 1 ? 0.8 : 1.0) * (stockType == 2 ? 0.9 : 1.0) * (gripType == 1 ? 0.9 : 1.0) * (isProne(player) && gripType == 3 ? 0.9 : 1.0)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.8 * zt) * (barrelType == 1 ? 0.4 : 1.0) * (stockType == 2 ? 0.6 : 1.0) * (gripType == 1 ? 0.7 : 1.0) * (isProne(player) && gripType == 3 ? 0.1 : 1.0)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.85 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.4 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        action.setPosZ(2.5f * (float) fp);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.98 * zt);
        float numP = (float) (1 - 0.97 * zt);

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1.2f, 0.55f);

        CoreGeoBone shell = getAnimationProcessor().getBone("shell");

        if (GunData.from(stack).holdOpen.get()) {
            action.setPosZ(2.5f);
            shell.setScaleX(0);
            shell.setScaleY(0);
            shell.setScaleZ(0);
        } else {
            shell.setScaleX(1);
            shell.setScaleY(1);
            shell.setScaleZ(1);
        }
    }
}
