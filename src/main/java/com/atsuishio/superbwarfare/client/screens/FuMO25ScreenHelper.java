package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class FuMO25ScreenHelper {

    public static BlockPos pos = null;
    public static List<Entity> entities = null;

    public static final int TOLERANCE_DISTANCE = 16;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        if (player == null) return;
        var menu = player.containerMenu;
        if (!(menu instanceof FuMO25Menu fuMO25Menu)) return;
        if (pos == null) return;

        if (pos.distToCenterSqr(cameraPos) > TOLERANCE_DISTANCE * TOLERANCE_DISTANCE) {
            pos = BlockPos.containing(cameraPos);
        }

        if (fuMO25Menu.getEnergy() <= 0) {
            resetEntities();
            return;
        }

        var funcType = fuMO25Menu.getFuncType();
        entities = SeekTool.getEntitiesWithinRange(pos, player.level(), funcType == 1 ? FuMO25BlockEntity.MAX_RANGE : FuMO25BlockEntity.DEFAULT_RANGE);
    }

    public static void resetEntities() {
        if (entities != null) {
            entities = null;
        }
    }
}
