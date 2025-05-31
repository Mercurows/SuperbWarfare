package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.rifle.M4Item;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class M4ItemModel extends CustomGunModel<M4Item> {

    public static float posYAlt = 0.5625f;
    public static float scaleZAlt = 0.88f;
    public static float posZAlt = 7.6f;
    public static float rotXSight = 0f;
    public static float rotXBipod = 0f;
    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;

    @Override
    public ResourceLocation getAnimationResource(M4Item animatable) {
        return Mod.loc("animations/m_4.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(M4Item animatable) {
        return Mod.loc("geo/m_4.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M4Item animatable) {
        return Mod.loc("textures/item/m_4.png");
    }

    @Override
    public void setCustomAnimations(M4Item animatable, long instanceId, AnimationState<M4Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone lh = getAnimationProcessor().getBone("Lefthand");
        CoreGeoBone sight1fold = getAnimationProcessor().getBone("sight1fold");
        CoreGeoBone sight2fold = getAnimationProcessor().getBone("sight2fold");
        CoreGeoBone button = getAnimationProcessor().getBone("button");
        CoreGeoBone button6 = getAnimationProcessor().getBone("button6");
        CoreGeoBone button7 = getAnimationProcessor().getBone("button7");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        double fpz = ClientEventHandler.firePosZ * 17 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        posYAlt = Mth.lerp(times, posYAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? -0.6875f : 0.5625f);
        scaleZAlt = Mth.lerp(times, scaleZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 0.4f : 0.88f);
        posZAlt = Mth.lerp(times, posZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 5.5f : 7.6f);
        rotXSight = Mth.lerp(1.5f * times, rotXSight, type == 0 ? 0 : 90);

        float posY = switch (type) {
            case 0 -> 0.65f;
            case 1 -> 0.2225f;
            case 2 -> posYAlt;
            case 3 -> 0.6525f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.2f;
            case 1 -> 0.4f;
            case 2 -> scaleZAlt;
            case 3 -> 0.94f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3f;
            case 1 -> 3.5f;
            case 2 -> posZAlt;
            case 3 -> 8.4f;
            default -> 0f;
        };

        sight1fold.setRotX(rotXSight * Mth.DEG_TO_RAD);
        sight2fold.setRotX(rotXSight * Mth.DEG_TO_RAD);

        gun.setPosX(2.935f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.2f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        gun.setRotZ((float) (0.05f * zpz));
        scope.setScaleZ(1f - (0.4f * (float) zp));
        scope2.setScaleZ(1f - (0.4f * (float) zp));
        scope3.setScaleZ(1f + (0.2f * (float) zp));

        button.setScaleY(1f - (0.85f * (float) zp));
        button6.setScaleX(1f - (0.5f * (float) zp));
        button7.setScaleX(1f - (0.5f * (float) zp));

        if (type == 3 && zt > 0.5) {
            lh.setPosY((float) (-zt * 4));
        }
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

        CoreGeoBone flare = getAnimationProcessor().getBone("flare");
        int BarrelType = GunData.from(stack).attachment.get(AttachmentType.BARREL);

        if (BarrelType == 1) {
            flare.setPosZ(-2);
        }

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

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
