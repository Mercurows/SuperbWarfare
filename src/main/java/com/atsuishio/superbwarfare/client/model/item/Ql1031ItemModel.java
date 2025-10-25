package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.Ql1031Item;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class Ql1031ItemModel extends CustomGunModel<Ql1031Item> {

    public static float posYAlt = 0.4325f;
    public static float scaleZAlt = 0.88f;
    public static float posZAlt = 7.8f;
    public static float rotXSight = 0f;
    public static float rotXBipod = 0f;

    @Override
    public void setCustomAnimations(Ql1031Item animatable, long instanceId, AnimationState<Ql1031Item> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        CoreGeoBone gun = getAnimationProcessor().getBone("bone");
        CoreGeoBone scope = getAnimationProcessor().getBone("Scope1");
        CoreGeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        CoreGeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        CoreGeoBone sight2fold = getAnimationProcessor().getBone("SightFold2");

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getDeltaFrameTime(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        int type = GunData.from(stack).attachment.get(AttachmentType.SCOPE);

        posYAlt = Mth.lerp(times, posYAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? -0.7375f : 0.4325f);
        scaleZAlt = Mth.lerp(times, scaleZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 0.4f : 0.88f);
        posZAlt = Mth.lerp(times, posZAlt, stack.getOrCreateTag().getBoolean("ScopeAlt") ? 5.5f : 8.3f);
        rotXSight = Mth.lerp(1.5f * times, rotXSight, type == 0 ? 0 : 90);

        float posY = switch (type) {
            case 0 -> 0.68f;
            case 1 -> 0.0225f;
            case 2 -> posYAlt;
            case 3 -> 0.29f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.58f;
            case 1 -> 0.6f;
            case 2 -> scaleZAlt;
            case 3 -> 0.94f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 5.4f;
            case 1 -> 5.5f;
            case 2 -> posZAlt;
            case 3 -> 9.15f;
            default -> 0f;
        };
        
        sight2fold.setRotX(rotXSight * Mth.DEG_TO_RAD);

        gun.setPosX(2.71f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.2f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        gun.setRotZ((float) (0.05f * zpz));
        scope.setScaleZ(1f - (0.4f * (float) zp));
        scope2.setScaleZ(1f - (0.4f * (float) zp));
        scope3.setScaleZ(1f + (0.2f * (float) zp));
        
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


        var data = GunData.from(stack);
        if (data.selectedFireModeInfo().name.equals("Hold")) {
            ClientEventHandler.handleShootAnimation(shen, 1.25f, 2f, 3f, 2.5f, 1.3f, 1f, 0.7f, 0.55f);
        } else {
            switch (data.selectedFireModeInfo().mode) {
                case SEMI -> ClientEventHandler.handleShootAnimation(shen, 2.5f, 0.5f, 2f, 1.5f, 2f, 1.4f, 0.65f, 0.7f);
                case AUTO -> ClientEventHandler.handleShootAnimation(shen, 0.95f, -0.95f, 0.85f, 0.8f, 0.9f, 1, 0.6f, 0.75f);
            }
        }

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

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 2, 0, 0, false);

        CoreGeoBone main = getAnimationProcessor().getBone("0");

        float num = (float) (1 - 0.95 * zt);

        main.setRotX(num * main.getRotX());
        main.setRotY(num * main.getRotY());
        main.setRotZ(num * main.getRotZ());
        main.setPosX(num * main.getPosX());
        main.setPosY(num * main.getPosY());
        main.setPosZ(num * main.getPosZ());
    }
}
