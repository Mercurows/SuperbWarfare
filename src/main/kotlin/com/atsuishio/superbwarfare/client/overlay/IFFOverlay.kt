package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.*
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Camera
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import top.theillusivec4.curios.api.CuriosApi

@OnlyIn(Dist.CLIENT)
object IFFOverlay : CommonOverlay("iff") {
    val FRIENDLY_INDICATOR: ResourceLocation = loc("textures/overlay/teammate/friendly_indicator.png")
    val FRIENDLY_AIRCRAFT: ResourceLocation = loc("textures/overlay/teammate/friendly_aircraft.png")
    val FRIENDLY_TANK: ResourceLocation = loc("textures/overlay/teammate/friendly_tank.png")
    val FRIENDLY_APC: ResourceLocation = loc("textures/overlay/teammate/friendly_apc.png")
    val FRIENDLY_AA: ResourceLocation = loc("textures/overlay/teammate/friendly_aa.png")
    val FRIENDLY_CAR: ResourceLocation = loc("textures/overlay/teammate/friendly_car.png")

    @JvmField
    val FRIENDLY_ARTILLERY: ResourceLocation = loc("textures/overlay/teammate/friendly_artillery.png")
    val FRIENDLY_BOAT: ResourceLocation = loc("textures/overlay/teammate/friendly_boat.png")
    val FRIENDLY_DEFENSE: ResourceLocation = loc("textures/overlay/teammate/friendly_defense.png")
    val FRIENDLY_DRONE: ResourceLocation = loc("textures/overlay/teammate/friendly_drone.png")
    val FRIENDLY_HELICOPTER: ResourceLocation = loc("textures/overlay/teammate/friendly_helicopter.png")
    val FRIENDLY_MINE: ResourceLocation = loc("textures/overlay/teammate/friendly_mine.png")

    override fun shouldRender() = super.shouldRender() && DisplayConfig.VEHICLE_INFO.get()

    override fun RenderContext.render() {
        CuriosApi.getCuriosInventory(player).ifPresent { c ->
            c.findFirstCurio(ModItems.IFF.get()).ifPresent { _ ->
                val entities = SeekTool.Builder(player)
                    .friendly()
                    .build()
                for (e in entities) {
                    if (e != null && e !== player && e.position().canBeSeen() && e !== player.vehicle) {
                        var team: Entity? = e
                        if (e.vehicle != null) {
                            team = e.vehicle
                        }

                        RenderSystem.disableDepthTest()
                        RenderSystem.depthMask(false)
                        RenderSystem.enableBlend()
                        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
                        RenderSystem.blendFuncSeparate(
                            GlStateManager.SourceFactor.SRC_ALPHA,
                            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                            GlStateManager.SourceFactor.ONE,
                            GlStateManager.DestFactor.ZERO
                        )

                        if (checkNoClip(player, team!!, cameraPos)) {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        } else {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f)
                        }

                        val pos = VectorTool.lerpGetEntityBoundingBoxCenter(team, partialTick)
                        val point = pos.worldToScreen()
                        val xf = point.x.toFloat()
                        val yf = point.y.toFloat()
                        val icon: ResourceLocation = getResourceLocation(team)

                        RenderHelper.preciseBlit(
                            guiGraphics,
                            icon,
                            Mth.clamp(xf - 6, 0f, (screenWidth - 12).toFloat()),
                            Mth.clamp(yf - 6, 0f, (screenHeight - 12).toFloat()),
                            0f,
                            0f,
                            12f,
                            12f,
                            12f,
                            12f
                        )
                    }
                }
            }
        }
    }


    private fun getResourceLocation(entity: Entity): ResourceLocation {
        var icon: ResourceLocation = FRIENDLY_INDICATOR

        if (entity is Boat) {
            icon = FRIENDLY_BOAT
        } else if (entity is VehicleEntity) {
            icon = when (entity.vehicleType) {
                VehicleType.AIRPLANE -> FRIENDLY_AIRCRAFT
                VehicleType.HELICOPTER -> FRIENDLY_HELICOPTER
                VehicleType.APC -> FRIENDLY_APC
                VehicleType.CAR -> FRIENDLY_CAR
                VehicleType.AA -> FRIENDLY_AA
                VehicleType.TANK -> FRIENDLY_TANK
                VehicleType.ARTILLERY -> FRIENDLY_ARTILLERY
                VehicleType.DRONE -> FRIENDLY_DRONE
                VehicleType.BOAT -> FRIENDLY_BOAT
                VehicleType.DEFENSE -> FRIENDLY_DEFENSE
                else -> FRIENDLY_INDICATOR
            }
        } else if (entity.type.`is`(ModTags.EntityTypes.MINE)) {
            icon = FRIENDLY_MINE
        }
        return icon
    }

    fun checkNoClip(player: Player, teammate: Entity, pos: Vec3): Boolean {
        return player.level().clip(
            ClipContext(
                pos, teammate.position(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player
            )
        ).type != HitResult.Type.BLOCK
    }

    fun calculateAngle(entityA: Entity, camera: Camera): Double {
        val v1 = camera.position.vectorTo(entityA.position())
        val v2 = Vec3(camera.lookVector)
        return v1.angleTo(v2)
    }
}
