package com.atsuishio.superbwarfare.client.screens;

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class FuMO25ScreenHelper {

    public static BlockPos pos = null;
    public static List<Entity> entities = null;

    public static final int TOLERANCE_DISTANCE = 16;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.side != LogicalSide.CLIENT) return;
        if (event.phase != TickEvent.Phase.END) return;
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
