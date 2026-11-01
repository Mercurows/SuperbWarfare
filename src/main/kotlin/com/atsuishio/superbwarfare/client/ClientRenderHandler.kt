package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.ClientRenderHandler.OFFSET_TTL
import com.atsuishio.superbwarfare.client.ClientRenderHandler.bulletRenderOffset
import com.atsuishio.superbwarfare.client.ClientRenderHandler.muzzleOffsetFor
import com.atsuishio.superbwarfare.client.ClientRenderHandler.transformVirtualRenderPosition
import com.atsuishio.superbwarfare.client.ClientRenderHandler.virtualOffsetRate
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
import com.atsuishio.superbwarfare.client.renderer.projectile.ReforgingGravityBallRenderer
import com.atsuishio.superbwarfare.client.renderer.special.RadiationGlowRenderer
import com.atsuishio.superbwarfare.client.tooltip.*
import com.atsuishio.superbwarfare.client.tooltip.component.*
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.BedrockBoneCoordinateTool
import com.atsuishio.superbwarfare.tools.toVec3
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.*
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.registries.ForgeRegistries
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

    @SubscribeEvent
    fun registerGravityLensingShader(event: RegisterShadersEvent) {
        ReforgingGravityBallRenderer.registerLensingShader(event)
    }

    /** [bulletRenderOffset] 的采样时刻（`System.nanoTime()`），超过 [OFFSET_TTL] 就当它过期 */
    private var bulletRenderOffsetTime: Long = 0L

    /**
     * 本地玩家**枪口相对视角定位点的偏移**（世界轴向量），由枪械渲染在开火窗口里写进来
     * （`GeoGunRenderer.submitMuzzleSample`，取的是 `flare` / 配件枪口骨骼）。
     *
     * 两种消费方式，别混：
     * - **弹体**（`ProjectileEntityRenderer`、`SmallCannonShellEntityRenderer`、
     *   `ReforgingGravityBallRenderer`、`ReforgingLightningBallRenderer`）：调
     *   [transformVirtualRenderPosition]，按 [virtualOffsetRate] 在几 tick 内淡回真实位置；
     * - **光束**（`LaserBeamRenderer`）：调 [muzzleOffsetFor]，把偏移当**固定的初始起点**钉住，不淡出。
     *
     * 只在**第一人称**写：`submitMuzzleSample` 整个挂在 `transformType.firstPerson()` 分支里，
     * 所以这份偏移的参照点就是眼位（= 第一人称的相机），可以直接加到弹体出生点上。
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

    /**
     * [bulletRenderOffset] 里那份样本（前提是它属于**本地玩家自己刚打的这一发**），**不带**时间淡出。
     *
     * 这是给**光束**用的：`LaserBeamRenderer` 拿它当光束的**固定初始起点**
     * （`眼位 + 偏移`，一次钉死）。
     *
     * ⚠ 别把这里换成 [transformVirtualRenderPosition]：那个会在几 tick 内把偏移淡回真实位置，
     * 对一颗点状弹体是对的，对一条从枪口拉到落点的线就是"整根横移 + 命中封口跟着爬"。
     * 光束只有十来 tick 寿命，起点定下来就不该再动。
     */
    @JvmStatic
    fun muzzleOffsetFor(projectile: Projectile): Vec3? {
        if (!isOwnFreshProjectile(projectile)) return null
        return bulletRenderOffset
    }

    @SubscribeEvent
    fun registerTooltip(event: RegisterClientTooltipComponentFactoriesEvent) {
        event.register(GunImageComponent::class.java) { ClientGunImageTooltip(it) }
        event.register(CellImageComponent::class.java) { ClientCellImageTooltip(it) }
        event.register(ChargingStationImageComponent::class.java) { ClientChargingStationImageTooltip(it) }
        event.register(DogTagImageComponent::class.java) { ClientDogTagImageTooltip(it) }
        event.register(AttachmentImageComponent::class.java) { ClientAttachmentImageTooltip(it) }
    }

    @SubscribeEvent
    @Suppress("UNCHECKED_CAST")
    fun addRadiationGlowLayers(event: EntityRenderersEvent.AddLayers) {
        val renderers = ForgeRegistries.ENTITY_TYPES.values
            .map { it as EntityType<out LivingEntity> }
            .toList()
        renderers.forEach { attachRadiationGlowLayer(it, event) }

        for (skin in event.skins) {
            val renderer = event.getSkin<PlayerRenderer>(skin) ?: continue
            renderer.addLayer(RadiationGlowRenderer(renderer))
        }
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun attachRadiationGlowLayer(type: EntityType<out LivingEntity>, event: EntityRenderersEvent.AddLayers) {
        if (type == EntityType.ENDER_DRAGON) return
        val renderer = try {
            event.getRenderer(type) as? LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>
        } catch (_: Exception) {
            null
        }
        renderer?.addLayer(RadiationGlowRenderer(renderer))
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
        event.registerBlockEntityRenderer(ModBlockEntities.TRIPOD.get()) { _ -> TripodBlockEntityRenderer() }
        event.registerBlockEntityRenderer(ModBlockEntities.RESONANCE_BASE.get()) { _ -> ResonanceBaseBlockEntityRenderer() }
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
        event.registerBelowAll(CrossHairOverlay.ID, CrossHairOverlay)
        event.registerBelowAll(InvisibleBladeChargeOverlay.ID, InvisibleBladeChargeOverlay)
        event.registerBelowAll(HeatBarOverlay.ID, HeatBarOverlay)
        event.registerBelowAll(BayonetChargeOverlay.ID, BayonetChargeOverlay)
        event.registerAbove(Mod.loc(CrossHairOverlay.ID), GunShieldOverlay.ID, GunShieldOverlay)
        event.registerBelowAll(RedTriangleOverlay.ID, RedTriangleOverlay)
        event.registerBelowAll(HandsomeFrameOverlay.ID, HandsomeFrameOverlay)
        event.registerBelowAll(SpyglassRangeOverlay.ID, SpyglassRangeOverlay)
        event.registerBelowAll(TowOverlay.ID, TowOverlay)
        event.registerBelowAll(MortarInfoOverlay.ID, MortarInfoOverlay)
        event.registerBelowAll(DeployWeaponTipOverlay.ID, DeployWeaponTipOverlay)
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
