package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.Mod
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
     * （`GeoGunRenderer.submitMuzzleOffset`）。
     *
     * 子弹是按服务器给的坐标画的，而服务器把它从 `Vec3(x, eyeY, z)` —— **眼睛**上打出去
     * （`GunItem.shoot`），所以刚出膛那几帧它画在脸上而不是枪管上。补上"枪口 − 视角定位点"
     * 这段偏差，它就正好落在枪管上：定位点每帧被 `applyFirstPersonPositioningTransform`
     * 钉在相机原点上，所以这段差值就是枪口相对玩家视角的位置。
     *
     * ⚠ 存的是**偏移量（向量）而不是枪口的世界坐标**，这是它和"记一个目标点再蹭过去"的关键区别。
     * 枪口长在跟着视角转的枪上，一旦换算成世界坐标，那个点就在开火那一瞬间定死了：玩家一转头，
     * 子弹就被拽向一个固定的世界位置 —— 看上去正是"没在枪口上，而且随视角上下左右转乱跑"。
     * 偏移量不会：它跟着枪走，加到子弹自己的位置上，永远只补那一段固定偏差。
     * 同一条教训见 `LaserSightCapture.Beam`：挂在枪上的东西留在渲染空间里用，只有向量/标量才去过世界坐标。
     *
     * 只给"刚出膛"的弹射物用，所以带有效期：开火窗口一过枪械渲染就不再刷新它，
     * 这份偏移会在 [OFFSET_TTL] 内过期作废，免得把不相干的弹射物（别人打的、自己扔的手雷）也挪过去。
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
            Math.atan2(view.x.toDouble(), front.toDouble()).toFloat(),
            Math.atan2(view.y.toDouble(), front.toDouble()).toFloat()
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
