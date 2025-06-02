package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.entity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MORTAR.get(), MortarRenderer::new);
        event.registerEntityRenderer(ModEntities.SENPAI.get(), SenpaiRenderer::new);
        event.registerEntityRenderer(ModEntities.CLAYMORE.get(), ClaymoreRenderer::new);
        event.registerEntityRenderer(ModEntities.C_4.get(), C4Renderer::new);
        event.registerEntityRenderer(ModEntities.TASER_BULLET.get(), TaserBulletProjectileRenderer::new);
        event.registerEntityRenderer(ModEntities.GUN_GRENADE.get(), GunGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.TARGET.get(), TargetRenderer::new);
        event.registerEntityRenderer(ModEntities.DPS_GENERATOR.get(), DPSGeneratorRenderer::new);
        event.registerEntityRenderer(ModEntities.RPG_ROCKET.get(), RpgRocketRenderer::new);
        event.registerEntityRenderer(ModEntities.HELI_ROCKET.get(), HeliRocketRenderer::new);
        event.registerEntityRenderer(ModEntities.MORTAR_SHELL.get(), MortarShellRenderer::new);
        event.registerEntityRenderer(ModEntities.CANNON_SHELL.get(), CannonShellRenderer::new);
        event.registerEntityRenderer(ModEntities.PROJECTILE.get(), ProjectileEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.MK_42.get(), Mk42Renderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(ModEntities.HAND_GRENADE.get(), HandGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.RGO_GRENADE.get(), RgoGrenadeRenderer::new);
        event.registerEntityRenderer(ModEntities.MLE_1934.get(), Mle1934Renderer::new);
        event.registerEntityRenderer(ModEntities.JAVELIN_MISSILE.get(), JavelinMissileRenderer::new);
        event.registerEntityRenderer(ModEntities.LASER.get(), LaserEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.ANNIHILATOR.get(), AnnihilatorRenderer::new);
        event.registerEntityRenderer(ModEntities.SPEEDBOAT.get(), SpeedboatRenderer::new);
        event.registerEntityRenderer(ModEntities.WHEEL_CHAIR.get(), WheelChairRenderer::new);
        event.registerEntityRenderer(ModEntities.AH_6.get(), Ah6Renderer::new);
        event.registerEntityRenderer(ModEntities.FLARE_DECOY.get(), FlareDecoyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.SMOKE_DECOY.get(), SmokeDecoyEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.LAV_150.get(), Lav150Renderer::new);
        event.registerEntityRenderer(ModEntities.SMALL_CANNON_SHELL.get(), SmallCannonShellRenderer::new);
        event.registerEntityRenderer(ModEntities.TOM_6.get(), Tom6Renderer::new);
        event.registerEntityRenderer(ModEntities.MELON_BOMB.get(), MelonBombEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.BMP_2.get(), Bmp2Renderer::new);
        event.registerEntityRenderer(ModEntities.WG_MISSILE.get(), WgMissileRenderer::new);
        event.registerEntityRenderer(ModEntities.LASER_TOWER.get(), LaserTowerRenderer::new);
        event.registerEntityRenderer(ModEntities.YX_100.get(), Yx100Renderer::new);
        event.registerEntityRenderer(ModEntities.WATER_MASK.get(), WaterMaskEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISM_TANK.get(), PrismTankRenderer::new);
        event.registerEntityRenderer(ModEntities.SWARM_DRONE.get(), SwarmDroneRenderer::new);
        event.registerEntityRenderer(ModEntities.HPJ_11.get(), Hpj11Renderer::new);
        event.registerEntityRenderer(ModEntities.A_10A.get(), A10Renderer::new);
        event.registerEntityRenderer(ModEntities.MK_82.get(), Mk82Renderer::new);
        event.registerEntityRenderer(ModEntities.AGM_65.get(), Agm65Renderer::new);
        event.registerEntityRenderer(ModEntities.BLU_43.get(), Blu43Renderer::new);
        event.registerEntityRenderer(ModEntities.TM_62.get(), Tm62Renderer::new);
    }
}
