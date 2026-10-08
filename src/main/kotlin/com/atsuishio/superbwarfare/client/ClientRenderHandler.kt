package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.ClientRenderHandler.OFFSET_TTL
import com.atsuishio.superbwarfare.client.ClientRenderHandler.bulletRenderOffset
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.decorator.ContainerItemDecorator
import com.atsuishio.superbwarfare.client.decorator.LuckyContainerItemDecorator
import com.atsuishio.superbwarfare.client.decorator.VehicleKeyItemDecorator
import com.atsuishio.superbwarfare.client.gun.MeleeClientHandler
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
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.atsuishio.superbwarfare.tools.toVec3
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
import org.joml.Vector2f
import org.joml.Vector3f
import top.theillusivec4.curios.api.client.CuriosRendererRegistry
import kotlin.math.atan2
import kotlin.math.min

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD,
    value = [Dist.CLIENT]
)
object ClientRenderHandler {

    /** [bulletRenderOffset] 的采样时刻（`System.nanoTime()`），超过 [OFFSET_TTL] 就当它过期 */
    private var bulletRenderOffsetTime: Long = 0L

    /**
     * 本地玩家**枪口相对视角定位点的偏移**（世界轴向量），由枪械渲染在开火窗口里写进来
     */
    var bulletRenderOffset: Vec3? = null
        set(value) {
            field = value
            bulletRenderOffsetTime = System.nanoTime()
        }

    private const val OFFSET_TTL = 300_000_000L

    private var muzzleDirectionTime: Long = 0L

    var muzzleDirection: Vec3? = null
        set(value) {
            field = value
            muzzleDirectionTime = System.nanoTime()
        }

    @JvmStatic
    fun freshMuzzleDirection(): Vector3f? {
        val direction = muzzleDirection ?: return null
        if (System.nanoTime() - muzzleDirectionTime > DIRECTION_TTL) return null
        if (direction.lengthSqr() < 1e-8) return null
        return Vector3f(
            direction.x.toFloat(),
            direction.y.toFloat(),
            direction.z.toFloat()
        ).normalize()
    }

    private const val DIRECTION_TTL = 200_000_000L
    private var scopeReticleScreenTime: Long = 0L

    var scopeReticleScreen: Vector2f? = null
        set(value) {
            field = value
            scopeReticleScreenTime = System.nanoTime()
        }

    @JvmStatic
    fun freshScopeReticleScreen(): Vector2f? {
        val screen = scopeReticleScreen ?: return null
        if (System.nanoTime() - scopeReticleScreenTime > RETICLE_TTL) return null
        return screen
    }

    private const val RETICLE_TTL = 200_000_000L

    private var gunRollTime: Long = 0L

    var gunRoll: Float = 0f
        set(value) {
            field = value
            gunRollTime = System.nanoTime()
        }

    @JvmStatic
    fun freshGunRoll(): Float? {
        if (System.nanoTime() - gunRollTime > GUN_ROLL_TTL) return null
        return gunRoll
    }

    private const val GUN_ROLL_TTL = 200_000_000L

    @JvmStatic
    fun shotAimOffset(): Vector2f? {
        val player = Minecraft.getInstance().player ?: return null
        val shot = GunItem.resolveShootDirection(freshMuzzleDirection()?.toVec3(), player)

        val view = BedrockBoneCoordinateTool
            .cameraRotationInverse(Minecraft.getInstance().gameRenderer.mainCamera)
            .invert()
            .transformDirection(
                shot.x.toFloat(), shot.y.toFloat(), shot.z.toFloat(), Vector3f()
            )

        val front = -view.z
        // 60° 的夹子已经挡住背向，这里只是兜底（前向分量非正时 atan2 的符号没有意义）
        if (front <= 1e-6f) return null

        return Vector2f(
            atan2(view.x.toDouble(), front.toDouble()).toFloat(),
            atan2(view.y.toDouble(), front.toDouble()).toFloat()
        )
    }

    private const val FADE_TICKS = 5.0

    @JvmStatic
    fun virtualOffsetRate(projectile: Projectile, partialTick: Float): Double {
        if (!isOwnFreshProjectile(projectile)) return 0.0

        // `- 1`:让第 0 tick 整体落在 0 之前（强度 1），淡出只发生在之后的 [FADE_TICKS] - 1 个 tick 里
        val age = (projectile.tickCount + partialTick - 1.0).coerceAtLeast(0.0)
        return 1 - AnimationCurves.EASE_OUT_CIRC.apply(min(1.0, age / (FADE_TICKS - 1)))
    }

    @JvmStatic
    fun hasVirtualOffset(projectile: Projectile, partialTick: Float): Boolean {
        return virtualOffsetRate(projectile, partialTick) > 0.0
    }

    /** 样本是不是给"本地玩家自己刚打的这一发"准备的：没过期 + 弹射物是本地玩家的 */
    private fun isOwnFreshProjectile(projectile: Projectile): Boolean {
        if (bulletRenderOffset == null) return false
        if (System.nanoTime() - bulletRenderOffsetTime > OFFSET_TTL) return false

        val player = Minecraft.getInstance().player ?: return false
        val owner = projectile.owner ?: return false
        return player.getUUID() == owner.getUUID()
    }

    @JvmStatic
    fun transformVirtualRenderPosition(stack: PoseStack, projectile: Projectile, partialTick: Float) {
        val offset = bulletRenderOffset ?: return

        val rate = virtualOffsetRate(projectile, partialTick)
        if (rate <= 0.0) return

        stack.translate(offset.x * rate, offset.y * rate, offset.z * rate)
    }

    @SubscribeEvent
    fun registerTooltip(event: RegisterClientTooltipComponentFactoriesEvent) {
        event.register(GunImageComponent::class.java) { ClientGunImageTooltip(it) }
        event.register(BocekImageComponent::class.java) { ClientBocekImageTooltip(it) }
        event.register(CellImageComponent::class.java) { ClientCellImageTooltip(it) }
        event.register(SentinelImageComponent::class.java) { ClientSentinelImageTooltip(it) }
        event.register(ChargingStationImageComponent::class.java) { ClientChargingStationImageTooltip(it) }
        event.register(DogTagImageComponent::class.java) { ClientDogTagImageTooltip(it) }
        event.register(AttachmentImageComponent::class.java) { ClientAttachmentImageTooltip(it) }
    }

    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(ModBlockEntities.CONTAINER.get()) { _ -> ContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.FUMO_25.get()) { _ -> FuMO25BlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get()) { _ -> ChargingStationBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CONTAINER.get()) { _ -> SmallContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.LUCKY_CONTAINER.get()) { _ -> LuckyContainerBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.VEHICLE_ASSEMBLING_TABLE.get()) { _ -> VehicleAssemblingTableBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get()) { _ -> BlueprintResearchTableBlockEntityRenderer() }
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
            GPWSOverlay.ID,
            GPWSOverlay
        )
        event.registerBelow(
            Mod.loc(GPWSOverlay.ID),
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
        event.register(ModItems.VEHICLE_KEY.get(), VehicleKeyItemDecorator())
    }

    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent?) {
        CuriosRendererRegistry.register(ModItems.PARACHUTE.get()) { ParachuteRenderer() }
        CuriosRendererRegistry.register(ModItems.THERMAL_IMAGING_GOGGLES.get()) { ThermalImagingGogglesRenderer() }

        // `/sbw melee force` 的客户端实现挂点（判定只在客户端做）
        MeleeClientHandler.installDebugHooks()
    }

    @SubscribeEvent
    fun registerLayer(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(ParachuteModel.LAYER_LOCATION) { ParachuteModel.createBodyLayer() }
        event.registerLayerDefinition(ThermalImagingGogglesModel.LAYER_LOCATION) { ThermalImagingGogglesModel.createBodyLayer() }
    }
}
