package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.M98bItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class M98bItemModel extends CustomGunModel<M98bItem> {

    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;
    public static float rotXBipod = 0f;
    public static float rotXSight = 0f;

    public static float posYAlt = 0.5625f;
    public static float scaleZAlt = 0.88f;
    public static float posZAlt = 7.6f;

    @Override
    public ResourceLocation getAnimationResource(M98bItem animatable) {
        return Mod.loc("animations/m_98b.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(M98bItem animatable) {
        return Mod.loc("geo/m_98b.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M98bItem animatable) {
        return Mod.loc("textures/item/m_98b.png");
    }

    @Override
    public void setCustomAnimations(M98bItem animatable, long instanceId, AnimationState<M98bItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone camera = getAnimationProcessor().getBone("camera");
        CoreGeoBone main = getAnimationProcessor().getBone("0");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone button = getAnimationProcessor().getBone("button");
        CoreGeoBone button6 = getAnimationProcessor().getBone("button6");
        CoreGeoBone button7 = getAnimationProcessor().getBone("button7");

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double fpz = ClientEventHandler.firePosZ * 7 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        posYAlt = Mth.lerp(times, posYAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? -0.9f : 0.05f);
        scaleZAlt = Mth.lerp(times, scaleZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 0.5f : 0.92f);
        posZAlt = Mth.lerp(times, posZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 2.5f : 5.5f);

        float posY = switch (type) {
            case 0 -> 0.07f;
            case 1 -> 0.008f;
            case 2 -> posYAlt;
            case 3 -> -0.2f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0, 1 -> 0.5f;
            case 2 -> scaleZAlt;
            case 3 -> 0.891f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0, 1 -> 2.5f;
            case 2 -> posZAlt;
            case 3 -> 6f;
            default -> 0f;
        };

        gun.setPosX(2.3f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        gun.setRotZ((float) (0.02f * zpz));
        scope.setScaleZ(1f - (0.6f * (float) zp));
        scope2.setScaleZ(1f - ((scaleZAlt - 0.3f) * (float) zp));
        scope3.setScaleZ(1f - (0.2f * (float) zp));
        button.setScaleY(1f - (0.85f * (float) zp));
        button6.setScaleX(1f - (0.8f * (float) zp));
        button7.setScaleX(1f - (0.8f * (float) zp));

        ClientEventHandler.gunRootMove(getAnimationProcessor());

        CoreGeoBone shen = getAnimationProcessor().getBone("fire");

        fireRotY = (float) Mth.lerp(0.3f * times, fireRotY, 0.2f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.4f + 0.5 * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.4f * fp + 0.44f * fr));
        shen.setPosZ((float) (2.825 * fp + 0.24f * fr + 1.25 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.08f * fr + 0.01f * fpz));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        shen.setPosX((float) (shen.getPosX() * (1 - 0.4 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.8 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.2 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - 0.8 * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - 0.85 * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.4 * zt)));

        CoreGeoBone l = getAnimationProcessor().getBone("l");
        CoreGeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        CoreGeoBone sight1fold = getAnimationProcessor().getBone("SightFold1");
        CoreGeoBone sight2fold = getAnimationProcessor().getBone("SightFold2");
        rotXSight = Mth.lerp(1.5f * times, rotXSight, type == 0 ? 0 : 90);
        sight1fold.setRotX(rotXSight * Mth.DEG_TO_RAD);
        sight2fold.setRotX(rotXSight * Mth.DEG_TO_RAD);

        float numR = (float) (1 - 0.9 * zt);
        float numP = (float) (1 - 0.68 * zt);

        if (GunData.from(stack).reload.time() > 0 || GunData.from(stack).bolt.actionTimer.get() > 0) {
            main.setRotX(numR * main.getRotX());
            main.setRotY(numR * main.getRotY());
            main.setRotZ(numR * main.getRotZ());
            main.setPosX(numP * main.getPosX());
            main.setPosY(numP * main.getPosY());
            main.setPosZ(numP * main.getPosZ());
            camera.setRotX(numR * camera.getRotX());
            camera.setRotY(numR * camera.getRotY());
            camera.setRotZ(numR * camera.getRotZ());
        }
        ClientEventHandler.handleReloadShake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
    }
}
