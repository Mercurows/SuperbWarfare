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
        event.registerEntityRenderer(ModEntities.MORTAR.get(), ::MortarRenderer)
        event.registerEntityRenderer(ModEntities.SENPAI.get(), ::SenpaiRenderer)
        event.registerEntityRenderer(ModEntities.CLAYMORE.get(), ::ClaymoreRenderer)
        event.registerEntityRenderer(ModEntities.C4.get(), ::C4Renderer)
        event.registerEntityRenderer(ModEntities.TASER_BULLET.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.GUN_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.TARGET.get(), ::TargetRenderer)
        event.registerEntityRenderer(ModEntities.DPS_GENERATOR.get(), ::DPSGeneratorRenderer)
        event.registerEntityRenderer(ModEntities.RPG_ROCKET_TBG.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.RPG_ROCKET_STANDARD.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.SMALL_ROCKET.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.MEDIUM_ROCKET.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.MORTAR_SHELL.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.CANNON_SHELL.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.PROJECTILE.get(), ::ProjectileEntityRenderer)
        event.registerEntityRenderer(ModEntities.MK_42.get(), ::Mk42Renderer)
        event.registerEntityRenderer(ModEntities.DRONE.get(), ::DroneRenderer)
        event.registerEntityRenderer(ModEntities.HAND_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.RGO_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.M18_SMOKE_GRENADE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.MLE_1934.get(), ::Mle1934Renderer)
        event.registerEntityRenderer(ModEntities.JAVELIN_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.ANNIHILATOR.get(), ::AnnihilatorRenderer)
        event.registerEntityRenderer(ModEntities.SPEEDBOAT.get(), ::SpeedboatRenderer)
        event.registerEntityRenderer(ModEntities.TINY_SPEEDBOAT.get(), ::TinySpeedboatRenderer)
        event.registerEntityRenderer(ModEntities.WHEEL_CHAIR.get(), ::WheelChairRenderer)
        event.registerEntityRenderer(ModEntities.AH_6.get(), ::Ah6Renderer)
        event.registerEntityRenderer(ModEntities.FLARE_DECOY.get(), ::FlareDecoyEntityRenderer)
        event.registerEntityRenderer(
            ModEntities.WHITE_PHOSPHORUS_PROJECTILE.get(),
            ::WhitePhosphorusProjectileEntityRenderer
        )
        event.registerEntityRenderer(ModEntities.PRISMATIC_BOLT.get(), ::PrismaticBoltEntityRenderer)
        event.registerEntityRenderer(ModEntities.SMOKE_DECOY.get(), ::SmokeDecoyEntityRenderer)
        event.registerEntityRenderer(ModEntities.LAV_150.get(), ::Lav150Renderer)
        event.registerEntityRenderer(ModEntities.SMALL_CANNON_SHELL.get(), ::SmallCannonShellEntityRenderer)
        event.registerEntityRenderer(ModEntities.TOM_6.get(), ::Tom6Renderer)
        event.registerEntityRenderer(ModEntities.MELON_BOMB.get(), ::MelonBombEntityRenderer)
        event.registerEntityRenderer(ModEntities.BMP_2.get(), ::Bmp2Renderer)
        event.registerEntityRenderer(ModEntities.WIRE_GUIDE_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.LASER_TOWER.get(), ::LaserTowerRenderer)
        event.registerEntityRenderer(ModEntities.YX_100.get(), ::Yx100Renderer)
        event.registerEntityRenderer(ModEntities.PRISM_TANK.get(), ::PrismTankRenderer)
        event.registerEntityRenderer(ModEntities.SWARM_DRONE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.HPJ_11.get(), ::Hpj11Renderer)
        event.registerEntityRenderer(ModEntities.A_10A.get(), ::A10Renderer)
        event.registerEntityRenderer(ModEntities.MK_82.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.SC_50.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.SC_250.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.AGM_65.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.BLU_43.get(), ::Blu43Renderer)
        event.registerEntityRenderer(ModEntities.TM_62.get(), ::Tm62Renderer)
        event.registerEntityRenderer(ModEntities.PTKM_1R.get(), ::Ptkm1rRenderer)
        event.registerEntityRenderer(ModEntities.PTKM_PROJECTILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.TYPE_63.get(), ::Type63Renderer)
        event.registerEntityRenderer(ModEntities.MEDICAL_KIT.get(), ::MedicalKitEntityRenderer)
        event.registerEntityRenderer(ModEntities.BL_132.get(), ::Bl132Renderer)
        event.registerEntityRenderer(ModEntities.GRAPESHOT.get(), ::GrapeshotRenderer)
        event.registerEntityRenderer(ModEntities.SUPER_STAR_PROJECTILE.get(), ::SuperStarProjectileRenderer)
        event.registerEntityRenderer(
            ModEntities.VEHICLE_ASSEMBLING_TABLE.get(),
            ::VehicleAssemblingTableVehicleRenderer
        )
        event.registerEntityRenderer(ModEntities.WAVEFORCE_TOWER.get(), ::WaveforceTowerRenderer)
        event.registerEntityRenderer(ModEntities.IGLA_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.RU_9M336_MISSILE.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.TRUCK.get(), ::TruckRenderer)
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP.get(), ::SodayoPickUpRenderer)
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP_HMG.get(), ::SodayoPickUpHmgRenderer)
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP_ROCKET.get(), ::SodayoPickUpRocketRenderer)
        event.registerEntityRenderer(ModEntities.SODAYO_PICK_UP_TOW.get(), ::SodayoPickUpTowRenderer)
        event.registerEntityRenderer(ModEntities.TOW.get(), ::TowRenderer)
        event.registerEntityRenderer(ModEntities.STEEL_COIL.get(), ::SteelCoilRenderer)
        event.registerEntityRenderer(ModEntities.MI_28.get(), ::Mi28Renderer)
        event.registerEntityRenderer(ModEntities.KH_39.get(), ::BasicProjectileRenderer)
        event.registerEntityRenderer(ModEntities.PLZ_05.get(), ::Plz05Renderer)
        event.registerEntityRenderer(ModEntities.LAV_AD.get(), ::LavAdRenderer)
        event.registerEntityRenderer(ModEntities.KV_16.get(), ::Kv16Renderer)
        event.registerEntityRenderer(ModEntities.JU_87.get(), ::Ju87Renderer)
        event.registerEntityRenderer(ModEntities.T_90A.get(), ::T90aRenderer)
        event.registerEntityRenderer(ModEntities.M_1A_2.get(), ::M1A2Renderer)
        event.registerEntityRenderer(ModEntities.BRADLEY.get(), ::BradleyRenderer)
        event.registerEntityRenderer(ModEntities.TURRET_WRECK.get(), ::TurretWreckRenderer)
        event.registerEntityRenderer(ModEntities.LAV_25.get(), ::Lav25Renderer)
        event.registerEntityRenderer(ModEntities.ZTZ_99A.get(), ::Ztz99aRenderer)
    }
}