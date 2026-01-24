package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.decorator.ContainerItemDecorator
import com.atsuishio.superbwarfare.client.decorator.LuckyContainerItemDecorator
import com.atsuishio.superbwarfare.client.model.curio.ParachuteModel
import com.atsuishio.superbwarfare.client.model.curio.ThermalImagingGogglesModel
import com.atsuishio.superbwarfare.client.overlay.*
import com.atsuishio.superbwarfare.client.renderer.block.*
import com.atsuishio.superbwarfare.client.renderer.curio.ParachuteRenderer
import com.atsuishio.superbwarfare.client.renderer.curio.ThermalImagingGogglesRenderer
import com.atsuishio.superbwarfare.client.tooltip.*
import com.atsuishio.superbwarfare.client.tooltip.component.*
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.event.RegisterItemDecorationsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import top.theillusivec4.curios.api.client.CuriosRendererRegistry
import kotlin.math.min

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
object ClientRenderHandler {
    // TODO 正确赋值该变量
    var bulletRenderOffset: Vec3? = null

    /**
     * 修改子弹类实体的虚拟渲染位置
     */
    @JvmStatic
    fun transformVirtualRenderPosition(stack: PoseStack, projectile: Projectile, partialTick: Float) {
        if (bulletRenderOffset == null) return

        val player = Minecraft.getInstance().player
        if (player == null || projectile.owner == null || (player.getUUID() != projectile.owner!!.getUUID())) return

        val rate = 1 - AnimationCurves.EASE_OUT_CIRC.apply(min(1.0, (projectile.tickCount + partialTick) / 5.0))
        val offset = bulletRenderOffset!!.subtract(projectile.position()).multiply(rate, rate, rate)
        stack.translate(offset.x, offset.y, offset.z)
    }

    @SubscribeEvent
    fun registerTooltip(event: RegisterClientTooltipComponentFactoriesEvent) {
        event.register(GunImageComponent::class.java) { ClientGunImageTooltip(it) }
        event.register(BocekImageComponent::class.java) { ClientBocekImageTooltip(it) }
        event.register(CellImageComponent::class.java) { ClientCellImageTooltip(it) }
        event.register(SentinelImageComponent::class.java) { ClientSentinelImageTooltip(it) }
        event.register(ChargingStationImageComponent::class.java) { ClientChargingStationImageTooltip(it) }
        event.register(DogTagImageComponent::class.java) { ClientDogTagImageTooltip(it) }
    }

    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get()) { _ -> ContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get()) { _ -> FuMO25BlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get()) { _ -> ChargingStationBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CONTAINER.get()) { _ -> SmallContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.LUCKY_CONTAINER.get()) { _ -> LuckyContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get()) { _ -> VehicleAssemblingTableBlockEntityRenderer() }
    }

    @SubscribeEvent
    fun registerGuiOverlays(event: RegisterGuiOverlaysEvent) {
        event.registerBelowAll(KillMessageOverlay.ID, KillMessageOverlay)
        event.registerBelow(Mod.loc(KillMessageOverlay.ID), ArmorPlateOverlay.ID, ArmorPlateOverlay)
        event.registerBelow(Mod.loc(ArmorPlateOverlay.ID), AmmoBarOverlay.ID, AmmoBarOverlay)
        event.registerBelow(Mod.loc(AmmoBarOverlay.ID), IFFOverlay.ID, IFFOverlay)
        event.registerBelow(Mod.loc(IFFOverlay.ID), VehicleTeamOverlay.ID, VehicleTeamOverlay)
        event.registerBelow(Mod.loc(VehicleTeamOverlay.ID), JavelinHudOverlay.ID, JavelinHudOverlay)
        event.registerBelow(Mod.loc(JavelinHudOverlay.ID), IglaHudOverlay.ID, IglaHudOverlay)
        event.registerBelow(Mod.loc(IglaHudOverlay.ID), VehicleHudOverlay.ID, VehicleHudOverlay)
        event.registerBelow(Mod.loc(VehicleHudOverlay.ID), VehicleMainWeaponHudOverlay.ID, VehicleMainWeaponHudOverlay)
        event.registerBelow(
            Mod.loc(VehicleMainWeaponHudOverlay.ID),
            VehicleCrosshairOverlay.ID,
            VehicleCrosshairOverlay
        )
        event.registerBelowAll(StaminaOverlay.ID, StaminaOverlay)
        event.registerBelowAll(AmmoCountOverlay.ID, AmmoCountOverlay)
        event.registerBelowAll(ItemRendererFixOverlay.ID, ItemRendererFixOverlay)
        event.registerBelowAll(CrossHairOverlay.ID, CrossHairOverlay)
        event.registerBelowAll(HeatBarOverlay.ID, HeatBarOverlay)
        event.registerBelowAll(DroneHudOverlay.ID, DroneHudOverlay)
        event.registerBelowAll(RedTriangleOverlay.ID, RedTriangleOverlay)
        event.registerBelowAll(HandsomeFrameOverlay.ID, HandsomeFrameOverlay)
        event.registerBelowAll(SpyglassRangeOverlay.ID, SpyglassRangeOverlay)
        event.registerBelowAll(TowOverlay.ID, TowOverlay)
        event.registerBelowAll(MortarInfoOverlay.ID, MortarInfoOverlay)
        event.registerBelowAll(Type63InfoOverlay.ID, Type63InfoOverlay)
        event.registerBelowAll(SodayoRocketInfoOverlay.ID, SodayoRocketInfoOverlay)
    }

    @SubscribeEvent
    fun registerItemDecorations(event: RegisterItemDecorationsEvent) {
        event.register(ModItems.CONTAINER.get(), ContainerItemDecorator())
        event.register(ModItems.LUCKY_CONTAINER.get(), LuckyContainerItemDecorator())
    }

    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent?) {
        CuriosRendererRegistry.register(ModItems.PARACHUTE.get()) { ParachuteRenderer() }
        CuriosRendererRegistry.register(ModItems.THERMAL_IMAGING_GOGGLES.get()) { ThermalImagingGogglesRenderer() }
    }

    @SubscribeEvent
    fun registerLayer(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(ParachuteModel.LAYER_LOCATION) { ParachuteModel.createBodyLayer() }
        event.registerLayerDefinition(ThermalImagingGogglesModel.LAYER_LOCATION) { ThermalImagingGogglesModel.createBodyLayer() }
    }
}