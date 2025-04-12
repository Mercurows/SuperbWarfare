package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.item.gun.data.value.AttachmentType;
import com.atsuishio.superbwarfare.item.gun.sniper.SvdItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class SvdItemModel extends GeoModel<SvdItem> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    public static float rotXBipod = 0f;

    @Override
    public ResourceLocation getAnimationResource(SvdItem animatable) {
        return Mod.loc("animations/svd.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(SvdItem animatable) {
        return Mod.loc("geo/svd.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SvdItem animatable) {
        return Mod.loc("textures/item/svd.png");
    }

    @Override
    public void setCustomAnimations(SvdItem animatable, long instanceId, AnimationState animationState) {
        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone bolt = getAnimationProcessor().getBone("bolt");
        GeoBone cross3 = getAnimationProcessor().getBone("Cross3");

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
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

        float posX = switch (type) {
            case 0, 1 -> 1.701f;
            case 2 -> 1.531f;
            case 3 -> 1.708f;
            default -> 0f;
        };
        float posY = switch (type) {
            case 0 -> 1.02f;
            case 1 -> 0.04f;
            case 2 -> 0.12f;
            case 3 -> -0.13f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.4f;
            case 1 -> 0.45f;
            case 2 -> 0.85f;
            case 3 -> 0.95f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 7f;
            case 1 -> 7.5f;
            case 2 -> 12.85f;
            case 3 -> 14.08f;
            default -> 0f;
        };

        gun.setPosX(posX * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        cross3.setScaleX((float) (1f + (0.1 * zp)));
        cross3.setScaleY((float) (1f + (0.1 * zp)));

        var data = GunData.from(stack);

        GeoBone shen;
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

        fireRotY = (float) Mth.lerp(0.3f * times, fireRotY, 0.6f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.8f + 1 * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.15f * fp + 0.18f * fr));
        shen.setPosZ((float) (2.935 * fp + 0.23f * fr + 1.325 * fpz));
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

        bolt.setPosZ(4.5f * (float) fp);

        if (data.holdOpen.get()) {
            bolt.setPosZ(3.5f);
        }

        GeoBone l = getAnimationProcessor().getBone("l");
        GeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.96 * zt);
        float numP = (float) (1 - 0.9 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.65f);
    }
}
