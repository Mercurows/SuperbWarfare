package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.sniper.AwmItem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class AwmItemModel extends CustomGunModel<AwmItem> {

    public static float rotXBipod = 0f;
    public static float rotXSight = 0f;

    @Override
    public void setCustomAnimations(AwmItem animatable, long instanceId, AnimationState<AwmItem> animationState) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (shouldCancelRender(stack, animationState)) return;

        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");
        GeoBone scope = getAnimationProcessor().getBone("Scope1");
        GeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        GeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        GeoBone button = getAnimationProcessor().getBone("button");
        GeoBone button6 = getAnimationProcessor().getBone("button6");
        GeoBone button7 = getAnimationProcessor().getBone("button7");
        GeoBone strike = getAnimationProcessor().getBone("jizhen");

        var data = GunData.from(stack);
        int type = data.attachment.get(AttachmentType.SCOPE);

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;

        float posY = switch (type) {
            case 0 -> 0.15f;
            case 1 -> 0.28f;
            case 2 -> -0.06f;
            case 3 -> 0.135f;
            default -> 0f;
        };
        float scaleZ = switch (type) {
            case 0 -> 0.55f;
            case 1 -> 0.5f;
            case 2 -> 0.9f;
            case 3 -> 0.91f;
            default -> 0f;
        };
        float posZ = switch (type) {
            case 0 -> 3.5f;
            case 1 -> 2.5f;
            case 2 -> 5.5f;
            case 3 -> 6.7f;
            default -> 0f;
        };

        gun.setPosX(2.71f * (float) zp);
        gun.setPosY(posY * (float) zp - (float) (0.2f * zpz));
        gun.setPosZ(posZ * (float) zp + (float) (0.3f * zpz));
        gun.setScaleZ(1f - (scaleZ * (float) zp));
        gun.setRotZ((float) (0.02f * zpz));
        scope.setScaleZ(1f - (0.6f * (float) zp));
        scope2.setScaleZ(1f - (0.2f * (float) zp));
        scope3.setScaleZ(1f - (0.2f * (float) zp));
        button.setScaleY(1f - (0.85f * (float) zp));
        button6.setScaleX(1f - (0.8f * (float) zp));
        button7.setScaleX(1f - (0.8f * (float) zp));

        ClientEventHandler.gunRootMove(getAnimationProcessor(), 0, 0, 0, false);

        GeoBone shen = getAnimationProcessor().getBone("fire");

        ClientEventHandler.handleShootAnimation(shen, 0.5f, 2f, 3f, 2.5f, 0.3f, 0.5f, 0.4f, 0.45f);

        GeoBone l = getAnimationProcessor().getBone("l");
        GeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        GeoBone sight1fold = getAnimationProcessor().getBone("SightFold1");
        GeoBone sight2fold = getAnimationProcessor().getBone("SightFold2");
        rotXSight = Mth.lerp(1.5f * times, rotXSight, type == 0 ? 0 : 90);
        sight1fold.setRotX(rotXSight * Mth.DEG_TO_RAD);
        sight2fold.setRotX(rotXSight * Mth.DEG_TO_RAD);

        if (data.closeStrike.get()) {
            strike.setPosZ(-0.2f);
        }

        float numR = (float) (1 - 0.92 * zt);
        float numP = (float) (1 - 0.82 * zt);

        if (data.reload.time() > 0 || data.bolt.actionTimer.get() > 0) {
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
