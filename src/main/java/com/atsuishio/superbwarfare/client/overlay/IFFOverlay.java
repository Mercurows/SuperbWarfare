package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;
import static com.atsuishio.superbwarfare.client.overlay.SpyglassRangeOverlay.FRIENDLY_INDICATOR;

@OnlyIn(Dist.CLIENT)
public class IFFOverlay implements IGuiOverlay {

    public static final String ID = Mod.MODID + "_iff";

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!DisplayConfig.VEHICLE_INFO.get()) return;

        Minecraft mc = gui.getMinecraft();
        Player player = mc.player;
        Camera camera = mc.gameRenderer.getMainCamera();

        if (player == null) return;

        CuriosApi.getCuriosInventory(player).ifPresent(
                c -> c.findFirstCurio(ModItems.IFF.get()).ifPresent(
                        s -> {
                            List<Entity> entities = SeekTool.getTeammate(player, player.level());
                            for (var e : entities) {
                                if (e != null && e != player && VectorUtil.canSee(e.position())) {
                                    Entity team = e;
                                    if (e.getVehicle() != null) {
                                        team = e.getVehicle();
                                    }
                                    Vec3 pos = new Vec3(Mth.lerp(partialTick, team.xo, team.getX()), Mth.lerp(partialTick, team.yo + team.getBbHeight() / 2, team.getY() + team.getBbHeight() / 2), Mth.lerp(partialTick, team.zo, team.getZ()));
                                    Vec3 point = VectorUtil.worldToScreen(pos);
                                    float xf = (float) point.x;
                                    float yf = (float) point.y;

                                    preciseBlit(guiGraphics, FRIENDLY_INDICATOR, Mth.clamp(xf - 6, 0, screenWidth - 12), Mth.clamp(yf - 6, 0, screenHeight - 12), 0, 0, 12, 12, 12, 12);
                                }
                            }
                        }
                )
        );
    }

    public static double calculateAngle(Entity entityA, Camera camera) {
        Vec3 v1 = camera.getPosition().vectorTo(entityA.position());
        Vec3 v2 = new Vec3(camera.getLookVector());
        return VectorTool.calculateAngle(v1, v2);
    }
}
