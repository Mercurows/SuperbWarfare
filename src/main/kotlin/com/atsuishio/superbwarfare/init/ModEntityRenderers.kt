package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.renderer.entity.*
import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer
import com.atsuishio.superbwarfare.client.renderer.projectile.ProjectileEntityRenderer
import com.atsuishio.superbwarfare.client.renderer.projectile.SmallCannonShellEntityRenderer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModEntityRenderers {
    @SubscribeEvent
    fun registerEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.MORTAR.get()) { MortarRenderer(it) }
        event.registerEntityRenderer(ModEntities.SENPAI.get(), ::SenpaiRenderer)
        event.registerEntityRenderer(ModEntities.CLAYMORE.get()) { ClaymoreRenderer(it) }
        event.registerEntityRenderer(ModEntities.C4.get()) { C4Renderer(it) }
        event.registerEntityRenderer(ModEntities.TASER_BULLET.get()) { TaserBulletProjectileRenderer(it) }
        event.registerEntityRenderer(ModEntities.GUN_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.TARGET.get(), ::TargetRenderer)
        event.registerEntityRenderer(ModEntities.DPS_GENERATOR.get(), ::DPSGeneratorRenderer)
        event.registerEntityRenderer(ModEntities.RPG_ROCKET_TBG.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.RPG_ROCKET_STANDARD.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.SMALL_ROCKET.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.MEDIUM_ROCKET.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.MORTAR_SHELL.get()) { MortarShellRenderer(it) }
        event.registerEntityRenderer(ModEntities.CANNON_SHELL.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.PROJECTILE.get()) { ProjectileEntityRenderer(it) }
        event.registerEntityRenderer(ModEntities.MK_42.get()) { Mk42Renderer(it) }
        event.registerEntityRenderer(ModEntities.DRONE.get()) { DroneRenderer(it) }
        event.registerEntityRenderer(ModEntities.HAND_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.RGO_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.M18_SMOKE_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.MLE_1934.get()) { Mle1934Renderer(it) }
        event.registerEntityRenderer(ModEntities.JAVELIN_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.ANNIHILATOR.get()) { AnnihilatorRenderer(it) }
        event.registerEntityRenderer(ModEntities.SPEEDBOAT.get()) { SpeedboatRenderer(it) }
        event.registerEntityRenderer(ModEntities.TINY_SPEEDBOAT.get()) { TinySpeedboatRenderer(it) }
        event.registerEntityRenderer(ModEntities.WHEEL_CHAIR.get()) { WheelChairRenderer(it) }
        event.registerEntityRenderer(ModEntities.AH_6.get()) { Ah6Renderer(it) }
        event.registerEntityRenderer(ModEntities.FLARE_DECOY.get(), ::FlareDecoyEntityRenderer)
        event.registerEntityRenderer(
            ModEntities.WHITE_PHOSPHORUS_PROJECTILE.get(),
            ::WhitePhosphorusProjectileEntityRenderer
        )
        event.registerEntityRenderer(ModEntities.PRISMATIC_BOLT.get()) { PrismaticBoltEntityRenderer(it) }
        event.registerEntityRenderer(ModEntities.SMOKE_DECOY.get(), ::SmokeDecoyEntityRenderer)
        event.registerEntityRenderer(ModEntities.LAV_150.get()) { Lav150Renderer(it) }
        event.registerEntityRenderer(ModEntities.SMALL_CANNON_SHELL.get(), ::SmallCannonShellEntityRenderer)
        event.registerEntityRenderer(ModEntities.TOM_6.get()) { Tom6Renderer(it) }
        event.registerEntityRenderer(ModEntities.MELON_BOMB.get()) { MelonBombEntityRenderer(it) }
        event.registerEntityRenderer(ModEntities.BMP_2.get()) { Bmp2Renderer(it) }
        event.registerEntityRenderer(ModEntities.WIRE_GUIDE_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.LASER_TOWER.get()) { LaserTowerRenderer(it) }
        event.registerEntityRenderer(ModEntities.YX_100.get()) { Yx100Renderer(it) }
        event.registerEntityRenderer(ModEntities.PRISM_TANK.get()) { PrismTankRenderer(it) }
        event.registerEntityRenderer(ModEntities.SWARM_DRONE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.HPJ_11.get()) { Hpj11Renderer(it) }
        event.registerEntityRenderer(ModEntities.A_10A.get()) { A10Renderer(it) }
        event.registerEntityRenderer(ModEntities.MK_82.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.SC_50.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.SC_250.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.AGM_65.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.BLU_43.get()) { Blu43Renderer(it) }
        event.registerEntityRenderer(ModEntities.TM_62.get()) { Tm62Renderer(it) }
        event.registerEntityRenderer(ModEntities.PTKM_1R.get()) { Ptkm1rRenderer(it) }
        event.registerEntityRenderer(ModEntities.PTKM_PROJECTILE.get()) { PtkmProjectileRenderer(it) }
        event.registerEntityRenderer(ModEntities.TYPE_63.get()) { Type63Renderer(it) }
        event.registerEntityRenderer(ModEntities.MEDICAL_KIT.get()) { MedicalKitEntityRenderer(it) }
        event.registerEntityRenderer(ModEntities.BL_132.get()) { Bl132Renderer(it) }
        event.registerEntityRenderer(ModEntities.GRAPESHOT.get(), ::GrapeshotRenderer)
        event.registerEntityRenderer(ModEntities.SUPER_STAR_PROJECTILE.get(), ::SuperStarProjectileRenderer)
        event.registerEntityRenderer(ModEntities.VEHICLE_ASSEMBLING_TABLE.get()) {
            VehicleAssemblingTableVehicleRenderer(
                it
            )
        }
        event.registerEntityRenderer(ModEntities.WAVEFORCE_TOWER.get()) { WaveforceTowerRenderer(it) }
        event.registerEntityRenderer(ModEntities.IGLA_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.RU_9M336_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.TRUCK.get()) { TruckRenderer(it) }
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP.get()) { SodayoPickUpRenderer(it) }
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP_HMG.get()) { SodayoPickUpHmgRenderer(it) }
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP_ROCKET.get()) { SodayoPickUpRocketRenderer(it) }
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP_TOW.get()) { SodayoPickUpTowRenderer(it) }
        event.registerEntityRenderer(ModEntities.TOW.get()) { TowRenderer(it) }
        event.registerEntityRenderer(ModEntities.STEEL_COIL.get()) { SteelCoilRenderer(it) }
        event.registerEntityRenderer(ModEntities.MI_28.get()) { Mi28Renderer(it) }
        event.registerEntityRenderer(ModEntities.KH_39.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.PLZ_05.get()) { Plz05Renderer(it) }
        event.registerEntityRenderer(ModEntities.LAV_AD.get()) { LavAdRenderer(it) }
        event.registerEntityRenderer(ModEntities.KV_16.get()) { Kv16Renderer(it) }
        event.registerEntityRenderer(ModEntities.JU_87.get()) { Ju87Renderer(it) }
        event.registerEntityRenderer(ModEntities.T_90A.get()) { T90aRenderer(it) }
        event.registerEntityRenderer(ModEntities.M_1A_2.get()) { M1A2Renderer(it) }
        event.registerEntityRenderer(ModEntities.BRADLEY.get()) { BradleyRenderer(it) }
        event.registerEntityRenderer(ModEntities.TURRET_WRECK.get()) { TurretWreckRenderer(it) }
        event.registerEntityRenderer(ModEntities.LAV_25.get()) { Lav25Renderer(it) }
        event.registerEntityRenderer(ModEntities.ZTZ_99A.get()) { Ztz99aRenderer(it) }
    }
}