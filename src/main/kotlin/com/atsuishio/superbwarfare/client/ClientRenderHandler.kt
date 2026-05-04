package com.atsuishio.superbwarfare.client

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
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent
import top.theillusivec4.curios.api.client.CuriosRendererRegistry
import kotlin.math.min

@EventBusSubscriber(Dist.CLIENT)
object ClientRenderHandler {
    // TODO 正确赋值该变量
    @JvmStatic
    var bulletRenderOffset: Vec3? = null

    /**
     * 修改子弹类实体的虚拟渲染位置
     */
    @JvmStatic
    fun transformVirtualRenderPosition(stack: PoseStack, projectile: Projectile, partialTick: Float) {
        if (bulletRenderOffset == null) return

        val player = localPlayer
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
    fun registerRenderers(event: RegisterRenderers) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get()) { _ -> ContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get()) { _ -> FuMO25BlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get()) { _ -> ChargingStationBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CONTAINER.get()) { _ -> SmallContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.LUCKY_CONTAINER.get()) { _ -> LuckyContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get()) { _ -> VehicleAssemblingTableBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get()) { _ -> BlueprintResearchTableBlockEntityRenderer() }
    }

    @SubscribeEvent
    fun registerOverlays(event: RegisterGuiLayersEvent) {
        event.registerBelowAll(KillMessageOverlay.ID, KillMessageOverlay)
        event.registerBelow(KillMessageOverlay.ID, ArmorPlateOverlay.ID, ArmorPlateOverlay)
        event.registerBelow(ArmorPlateOverlay.ID, AmmoBarOverlay.ID, AmmoBarOverlay)
        event.registerBelow(AmmoBarOverlay.ID, IFFOverlay.ID, IFFOverlay)
        event.registerBelow(IFFOverlay.ID, VehicleTeamOverlay.ID, VehicleTeamOverlay)
        event.registerBelow(VehicleTeamOverlay.ID, JavelinHudOverlay.ID, JavelinHudOverlay)
        event.registerBelow(JavelinHudOverlay.ID, IglaHudOverlay.ID, IglaHudOverlay)
        event.registerBelow(IglaHudOverlay.ID, VehicleHudOverlay.ID, VehicleHudOverlay)
        event.registerBelow(VehicleHudOverlay.ID, VehicleMainWeaponHudOverlay.ID, VehicleMainWeaponHudOverlay)
        event.registerBelow(
            VehicleMainWeaponHudOverlay.ID,
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
    fun onClientSetup(event: FMLClientSetupEvent) {
        CuriosRendererRegistry.register(ModItems.PARACHUTE.get()) { ParachuteRenderer() }
        CuriosRendererRegistry.register(ModItems.THERMAL_IMAGING_GOGGLES.get()) { ThermalImagingGogglesRenderer() }
    }

    @SubscribeEvent
    fun registerLayer(event: RegisterLayerDefinitions) {
        event.registerLayerDefinition(ParachuteModel.LAYER_LOCATION) { ParachuteModel.createBodyLayer() }
        event.registerLayerDefinition(ThermalImagingGogglesModel.LAYER_LOCATION) { ThermalImagingGogglesModel.createBodyLayer() }
    }
}