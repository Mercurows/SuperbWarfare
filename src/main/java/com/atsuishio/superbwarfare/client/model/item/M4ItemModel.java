package com.atsuishio.superbwarfare.client.model.item;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.AnimationHelper;
import com.atsuishio.superbwarfare.client.overlay.CrossHairOverlay;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.gun.rifle.M4Item;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import static com.atsuishio.superbwarfare.event.ClientEventHandler.isProne;

public class M4ItemModel extends GeoModel<M4Item> {

    public static float posYAlt = 0.5625f;
    public static float scaleZAlt = 0.88f;
    public static float posZAlt = 7.6f;
    public static float rotXSight = 0f;
    public static float rotXBipod = 0f;
    public static float fireRotY = 0f;
    public static float fireRotZ = 0f;

    @Override
    public ResourceLocation getAnimationResource(M4Item animatable) {
        return Mod.loc("animations/m4.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(M4Item animatable) {
        return Mod.loc("geo/m4.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(M4Item animatable) {
        return Mod.loc("textures/item/m4.png");
    }

    @Override
    public void setCustomAnimations(M4Item animatable, long instanceId, AnimationState animationState) {
        GeoBone gun = getAnimationProcessor().getBone("bone");
        GeoBone scope = getAnimationProcessor().getBone("Scope1");
        GeoBone scope2 = getAnimationProcessor().getBone("Scope2");
        GeoBone scope3 = getAnimationProcessor().getBone("Scope3");
        GeoBone cross1 = getAnimationProcessor().getBone("Cross1");
        GeoBone cross2 = getAnimationProcessor().getBone("Cross2");
        GeoBone cross3 = getAnimationProcessor().getBone("Cross3");
        GeoBone lh = getAnimationProcessor().getBone("Lefthand");
        GeoBone crossAlt = getAnimationProcessor().getBone("CrossAlt");
        GeoBone sight1fold = getAnimationProcessor().getBone("sight1fold");
        GeoBone sight2fold = getAnimationProcessor().getBone("sight2fold");
        GeoBone button = getAnimationProcessor().getBone("button");
        GeoBone button6 = getAnimationProcessor().getBone("button6");
        GeoBone button7 = getAnimationProcessor().getBone("button7");

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ModTags.Items.GUN)) return;

        float times = 0.6f * (float) Math.min(Minecraft.getInstance().getTimer().getRealtimeDeltaTicks(), 0.8);
        double zt = ClientEventHandler.zoomTime;
        double zp = ClientEventHandler.zoomPos;
        double zpz = ClientEventHandler.zoomPosZ;
        double swayX = ClientEventHandler.swayX;
        double swayY = ClientEventHandler.swayY;
        float moveRotZ = (float) ClientEventHandler.moveRotZ;
        float movePosX = (float) ClientEventHandler.movePosX;
        float movePosY = (float) ClientEventHandler.movePosY;
        double mph = ClientEventHandler.movePosHorizon;
        double vY = ClientEventHandler.velocityY;
        double turnRotX = ClientEventHandler.turnRot[0];
        double turnRotY = ClientEventHandler.turnRot[1];
        double turnRotZ = ClientEventHandler.turnRot[2];
        double fpz = ClientEventHandler.firePosZ * 17 * times;
        double fp = ClientEventHandler.firePos;
        double fr = ClientEventHandler.fireRot;

        int type = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.SCOPE);

        posYAlt = Mth.lerp(times, posYAlt, NBTTool.getTag(stack).getBoolean("ScopeAlt") ? -0.6875f : 0.5625f);
        scaleZAlt = Mth.lerp(times, scaleZAlt, NBTTool.getTag(stack).getBoolean("ScopeAlt") ? 0.4f : 0.88f);
        posZAlt = Mth.lerp(times, posZAlt, NBTTool.getTag(stack).getBoolean("ScopeAlt") ? 5.5f : 7.6f);
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

        final var tag = NBTTool.getTag(stack);
        tag.putBoolean("HoloHidden", !(gun.getPosX() > 2.385));
        NBTTool.saveTag(stack, tag);

        if (type == 3 && zt > 0.5) {
            lh.setPosY((float) (-zt * 4));
        }
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

        fireRotY = (float) Mth.lerp(0.5f * times, fireRotY, 0.2f * ClientEventHandler.recoilHorizon * fpz);
        fireRotZ = (float) Mth.lerp(2f * times, fireRotZ, (0.2f + 0.3 * fpz) * ClientEventHandler.recoilHorizon);

        shen.setPosX(-0.4f * (float) (ClientEventHandler.recoilHorizon * (0.5 + 0.4 * ClientEventHandler.fireSpread)));
        shen.setPosY((float) (0.15f * fp + 0.18f * fr));
        shen.setPosZ((float) (0.375 * fp + 0.44f * fr + 0.75 * fpz));
        shen.setRotX((float) (0.01f * fp + 0.05f * fr + 0.01f * fpz));
        shen.setRotY(fireRotY);
        shen.setRotZ(fireRotZ);

        cross1.setPosY(-0.25f * (float) fpz);
        cross2.setPosY(-0.1f * (float) fpz);
        crossAlt.setPosY(-0.2f * (float) fpz);
        cross3.setPosY(-0.2f * (float) fpz);

        shen.setPosX((float) (shen.getPosX() * (1 - 0.1 * zt)));
        shen.setPosY((float) (shen.getPosY() * (-1 + 0.8 * zt)));
        shen.setPosZ((float) (shen.getPosZ() * (1 - 0.1 * zt)));
        shen.setRotX((float) (shen.getRotX() * (1 - (type == 3 ? 0.96 : type == 1 ? 0.8 : 0.9) * zt)));
        shen.setRotY((float) (shen.getRotY() * (1 - (type == 3 ? 0.95 : 0.9) * zt)));
        shen.setRotZ((float) (shen.getRotZ() * (1 - 0.4 * zt)));

        CrossHairOverlay.gunRot = shen.getRotZ();

        GeoBone flare = getAnimationProcessor().getBone("flare");
        int BarrelType = GunsTool.getAttachmentType(stack, GunsTool.AttachmentType.BARREL);

        if (BarrelType == 1) {
            flare.setPosZ(-2);
        }

        GeoBone l = getAnimationProcessor().getBone("l");
        GeoBone r = getAnimationProcessor().getBone("r");
        rotXBipod = Mth.lerp(1.5f * times, rotXBipod, isProne(player) ? -90 : 0);
        l.setRotX(rotXBipod * Mth.DEG_TO_RAD);
        r.setRotX(rotXBipod * Mth.DEG_TO_RAD);

        GeoBone root = getAnimationProcessor().getBone("root");
        root.setPosX((float) (movePosX + 20 * ClientEventHandler.drawTime + 9.3f * mph));
        root.setPosY((float) (swayY + movePosY - 40 * ClientEventHandler.drawTime - 2f * vY));
        root.setRotX((float) (swayX - Mth.DEG_TO_RAD * 60 * ClientEventHandler.drawTime + Mth.DEG_TO_RAD * turnRotX - 0.15f * vY));
        root.setRotY((float) (0.2f * movePosX + Mth.DEG_TO_RAD * 300 * ClientEventHandler.drawTime + Mth.DEG_TO_RAD * turnRotY));
        root.setRotZ((float) (0.2f * movePosX + moveRotZ + Mth.DEG_TO_RAD * 90 * ClientEventHandler.drawTime + 2.7f * mph + Mth.DEG_TO_RAD * turnRotZ));

        GeoBone camera = getAnimationProcessor().getBone("camera");
        GeoBone main = getAnimationProcessor().getBone("0");

        float numR = (float) (1 - 0.985 * zt);
        float numP = (float) (1 - 0.92 * zt);

        AnimationHelper.handleReloadShakeAnimation(stack, main, camera, numR, numP);
        ClientEventHandler.shake(Mth.RAD_TO_DEG * camera.getRotX(), Mth.RAD_TO_DEG * camera.getRotY(), Mth.RAD_TO_DEG * camera.getRotZ());
        AnimationHelper.handleShellsAnimation(getAnimationProcessor(), 1f, 0.55f);
    }
}
