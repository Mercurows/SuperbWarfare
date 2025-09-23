package com.atsuishio.superbwarfare.client.overlay;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.client.DisplayConfig;
import com.atsuishio.superbwarfare.entity.projectile.MineEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.tools.VectorUtil;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import top.theillusivec4.curios.api.CuriosApi;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.atsuishio.superbwarfare.client.RenderHelper.preciseBlit;

@OnlyIn(Dist.CLIENT)
public class IFFOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ID = Mod.loc("iff");

    public static final ResourceLocation FRIENDLY_INDICATOR = Mod.loc("textures/screens/teammate/friendly_indicator.png");
    public static final ResourceLocation FRIENDLY_AIRCRAFT = Mod.loc("textures/screens/teammate/friendly_aircraft.png");
    public static final ResourceLocation FRIENDLY_TANK = Mod.loc("textures/screens/teammate/friendly_tank.png");
    public static final ResourceLocation FRIENDLY_APC = Mod.loc("textures/screens/teammate/friendly_apc.png");
    public static final ResourceLocation FRIENDLY_AA = Mod.loc("textures/screens/teammate/friendly_aa.png");
    public static final ResourceLocation FRIENDLY_CAR = Mod.loc("textures/screens/teammate/friendly_car.png");
    public static final ResourceLocation FRIENDLY_ARTILLERY = Mod.loc("textures/screens/teammate/friendly_artillery.png");
    public static final ResourceLocation FRIENDLY_BOAT = Mod.loc("textures/screens/teammate/friendly_boat.png");
    public static final ResourceLocation FRIENDLY_DEFENSE = Mod.loc("textures/screens/teammate/friendly_defense.png");
    public static final ResourceLocation FRIENDLY_DRONE = Mod.loc("textures/screens/teammate/friendly_drone.png");
    public static final ResourceLocation FRIENDLY_HELICOPTER = Mod.loc("textures/screens/teammate/friendly_helicopter.png");
    public static final ResourceLocation FRIENDLY_MINE = Mod.loc("textures/screens/teammate/friendly_mine.png");

    @Override
    @ParametersAreNonnullByDefault
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!DisplayConfig.VEHICLE_INFO.get()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        var screenWidth = guiGraphics.guiWidth();
        var screenHeight = guiGraphics.guiHeight();
        var partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();


        if (player == null) return;

        CuriosApi.getCuriosInventory(player).flatMap(c -> c.findFirstCurio(ModItems.IFF.get())).ifPresent(s -> {
            List<Entity> entities = SeekTool.getTeammate(player, player.level());
            for (var e : entities) {
                if (e != null && e != player && VectorUtil.canSee(e.position()) && e != player.getVehicle()) {
                    Entity team = e;
                    if (e.getVehicle() != null) {
                        team = e.getVehicle();
                    }

                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

                    if (checkNoClip(player, team, cameraPos)) {
                        RenderSystem.setShaderColor(1, 1, 1, 1);
                    } else {
                        RenderSystem.setShaderColor(1, 1, 1, 0.4f);
                    }

                    Vec3 pos = new Vec3(Mth.lerp(partialTick, team.xo, team.getX()), Mth.lerp(partialTick, team.yo + team.getBbHeight() / 2, team.getY() + team.getBbHeight() / 2), Mth.lerp(partialTick, team.zo, team.getZ()));
                    Vec3 point = VectorUtil.worldToScreen(pos);
                    float xf = (float) point.x;
                    float yf = (float) point.y;
                    ResourceLocation icon = getResourceLocation(team);

                    preciseBlit(guiGraphics, icon, Mth.clamp(xf - 6, 0, screenWidth - 12), Mth.clamp(yf - 6, 0, screenHeight - 12), 0, 0, 12, 12, 12, 12);
                }
            }
        });
    }

    private static ResourceLocation getResourceLocation(Entity team) {
        ResourceLocation icon = FRIENDLY_INDICATOR;

        // TODO 载具类型判断没生效？
        if ((team instanceof VehicleEntity vehicle && vehicle.getVehicleType() == VehicleEntity.VehicleType.BOAT) || team instanceof Boat) {
            icon = FRIENDLY_BOAT;
        } else if (team instanceof VehicleEntity vehicle) {
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.AIRPLANE) {
                icon = FRIENDLY_AIRCRAFT;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.HELICOPTER) {
                icon = FRIENDLY_HELICOPTER;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.APC) {
                icon = FRIENDLY_APC;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.CAR) {
                icon = FRIENDLY_CAR;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.AA) {
                icon = FRIENDLY_AA;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.TANK) {
                icon = FRIENDLY_TANK;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.ARTILLERY) {
                icon = FRIENDLY_ARTILLERY;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.DRONE) {
                icon = FRIENDLY_DRONE;
            }
            if (vehicle.getVehicleType() == VehicleEntity.VehicleType.DEFENSE) {
                icon = FRIENDLY_DEFENSE;
            }
        } else if (team instanceof MineEntity) {
            icon = FRIENDLY_MINE;
        }
        return icon;
    }

    public static boolean checkNoClip(Player player, Entity teammate, Vec3 pos) {
        return player.level().clip(new ClipContext(pos, teammate.position(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player)).getType() != HitResult.Type.BLOCK;
    }

    public static double calculateAngle(Entity entityA, Camera camera) {
        Vec3 v1 = camera.getPosition().vectorTo(entityA.position());
        Vec3 v2 = new Vec3(camera.getLookVector());
        return VectorTool.calculateAngle(v1, v2);
    }
}
