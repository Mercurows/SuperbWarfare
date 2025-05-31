package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.FireMode;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.Hk416Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class Hk416ItemModel extends CustomGunModel<Hk416Item> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    public static float rotXBipod = 0f;

    @Override
    public ResourceLocation getAnimationResource(Hk416Item animatable) {
        return Mod.loc("animations/m_4.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Hk416Item animatable) {
        return Mod.loc("geo/hk_416.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(Hk416Item animatable) {
        return Mod.loc("textures/item/hk_416.png");
    }

    @Override
    public void setCustomAnimations(Hk416Item animatable, long instanceId, AnimationState<Hk416Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone kuaimanji = getAnimationProcessor().getBone("kuaimanji");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);

        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 17 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        float posY = switch (type) {
            case 0 -> 1.04f;
            case 1 -> 0.52f;
            case 2 -> 0.385f;
            case 3 -> 0.46f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.2f;
            case 1 -> 0.4f;
            case 2 -> 0.8f;
            case 3 -> 0.9f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3f;
            case 1 -> 3.5f;
            case 2 -> 7.4f;
            case 3 -> 7.5f;
            default -> 0f;
        };

        gun.setPosX(3.3055f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setRotZ((float) (0.05f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        scope.setScaleZ(1f - (0.6f * (float) zp));
        scope2.setScaleZ(1f - (0.8f * (float) zp));
        scope3.setScaleZ(1f - (0.5f * (float) zp));

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

        fireRotY = (float) Mth.lerp(0.5f * times, fireRotY, 0.2f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.2f + 0.3 * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.15f * fp + 0.18f * fr));
        shen.setPosZ((float) (0.375 * fp + 0.44f * fr + 0.75 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.05f * fr + 0.01f * fpz));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        shen.setPosX((float) (shen.getPosX() * (1 - 0.1 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.8 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.1 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - (type == 3 ? 0.96 : type == 1 ? 0.8 : 0.9) * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - (type == 3 ? 0.95 : 0.9) * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.4 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        var mode = GunData.from(stack).fireMode.get();

        kuaimanji.setRotX(mode == FireMode.AUTO ? 90 * Mth.DEG_TO_RAD : 0);

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.985 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.55f);
    }
}
