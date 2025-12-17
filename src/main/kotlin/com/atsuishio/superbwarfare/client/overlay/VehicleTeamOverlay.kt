package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.NBTTool
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import com.atsuishio.superbwarfare.tools.VectorUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Team
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
object VehicleTeamOverlay : CommonOverlay("vehicle_team") {

    override fun shouldRender() = super.shouldRender() && DisplayConfig.VEHICLE_INFO.get()


    override fun RenderContext.render() {
        var viewVec = Vec3(camera.lookVector)
        val poseStack = guiGraphics.pose()

        val stack = player.mainHandItem

        var lookAtEntity = false

        var entityRange = 0.0
        var lookingEntity = TraceTool.camerafFindLookingEntity(
            player,
            cameraPos,
            viewVec,
            VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get().toDouble()
        )

        (player.vehicle as? VehicleEntity)?.let { vehicle ->
            lookingEntity = vehicle.getPlayerLookAtEntityOnVehicle(player, 512.0, partialTick)
            viewVec = vehicle.getViewVec(player, partialTick)
        }

        val decoy = TraceTool.findLookDecoy(player, cameraPos, viewVec, 512.0)

        if (decoy != null && decoy.type.`is`(ModTags.EntityTypes.DECOY)) return

        if (lookingEntity is SmokeDecoyEntity) return

        if (lookingEntity != null) {
            lookAtEntity = true
            entityRange = player.distanceTo(lookingEntity).toDouble()
        }

        val tag = NBTTool.getTag(stack)

        val usingDrone = stack.`is`(ModItems.MONITOR.get()) && tag.getBoolean("Using") && tag.getBoolean("Linked")
        val outOfRange = entityRange > VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get()

        if (lookAtEntity && lookingEntity is VehicleEntity && !usingDrone && !outOfRange) {
            val vehicle = lookingEntity
            if (entityRange > VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get()) return

            val pos = lerpGetEntityBoundingBoxCenter(lookingEntity, partialTick)
                .add(Vec3(0.0, lookingEntity.bbHeight / 2 + 0.5, 0.0))

            val centerPos = lerpGetEntityBoundingBoxCenter(lookingEntity, partialTick)

            if (VectorUtil.canSee(pos)) {
                val point = VectorUtil.worldToScreen(pos)

                val x = point.x.toFloat()
                val y = point.y.toFloat()

                poseStack.pushPose()
                poseStack.translate(x, y - 12, 0f)

                val size =
                    Mth.clamp((50 / VectorUtil.fov) * 0.9f * max((512 - entityRange) / 512, 0.1), 0.4, 1.0).toFloat()
                poseStack.scale(size, size, size)
                val font = Minecraft.getInstance().font

                var color = -1

                if (lookingEntity is DroneEntity) {
                    val controller = EntityFindUtil.findPlayer(
                        vehicle.level(),
                        vehicle.getEntityData().get(DroneEntity.CONTROLLER)
                    )
                    if (controller != null) {
                        color = controller.getTeamColor()

                        val team: Team? = player.team
                        if (team is PlayerTeam) {
                            val info =
                                lookingEntity.displayName!!.string + " " + controller.getDisplayName()!!
                                    .string + (if (controller.team == null) "" else " <" + team.displayName
                                    .string + ">")
                            guiGraphics.drawString(
                                font,
                                Component.literal(info),
                                -font.width(info) / 2,
                                -13,
                                color,
                                false
                            )
                        }
                    } else {
                        val info = lookingEntity.displayName!!.string
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false)
                    }
                } else if (lookingEntity is OwnableEntity) {
                    val player1 = vehicle.getOwner()
                    if (player1 is Player) {
                        color = player1.getTeamColor()
                        val team: Team? = player.team
                        if (team is PlayerTeam) {
                            val info = lookingEntity.displayName!!.string + " " + player1.getDisplayName()
                                ?.string + (if (player1.team == null) "" else " <" + team.displayName
                                .string + ">")
                            guiGraphics.drawString(
                                font,
                                Component.literal(info),
                                -font.width(info) / 2,
                                -13,
                                color,
                                false
                            )
                        }
                    } else {
                        val info = lookingEntity.displayName!!.string
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false)
                    }
                } else {
                    val player1 = lookingEntity.getFirstPassenger()
                    if (lookingEntity.maxPassengers > 0 && player1 is Player) {
                        color = player1.getTeamColor()
                        val team: Team? = player.team
                        if (team is PlayerTeam) {
                            val info = lookingEntity.displayName!!.string + " " + player1.getDisplayName()
                                ?.string + (if (player1.team == null) "" else " <" + team.displayName
                                .string + ">")
                            guiGraphics.drawString(
                                font,
                                Component.literal(info),
                                -font.width(info) / 2,
                                -13,
                                color,
                                false
                            )
                        }
                    } else {
                        val info = lookingEntity.displayName!!.string
                        guiGraphics.drawString(font, Component.literal(info), -font.width(info) / 2, -13, color, false)
                    }
                }

                val range = format1D(entityRange, "M")
                val argb = (255 shl 24) or color

                guiGraphics.drawString(font, Component.literal(range), -font.width(range) / 2, 7, color, false)

                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, -2f, 40.5f, 2f, 0f, -0x80000000)
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -41.5f, -3f, -40.5f, 3f, 0f, argb)
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, -3f, 40.5f, -2f, 0f, argb)
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), -40.5f, 2f, 40.5f, 3f, 0f, argb)
                RenderHelper.fill(guiGraphics, RenderType.guiOverlay(), 40.5f, -3f, 41.5f, 3f, 0f, argb)
                RenderHelper.fill(
                    guiGraphics,
                    RenderType.guiOverlay(),
                    -40f,
                    -1.5f,
                    -40 + 80 * (lookingEntity.health / lookingEntity.getMaxHealth()),
                    1.5f,
                    0f,
                    argb
                )

                poseStack.popPose()
            }

            if (lookingEntity is AutoAimableEntity && VectorUtil.canSee(centerPos) && player.distanceTo(vehicle) < 4) {
                val point = VectorUtil.worldToScreen(centerPos)

                val x = point.x.toFloat()
                val y = point.y.toFloat()

                poseStack.pushPose()
                poseStack.translate(x, y - 12, 0f)

                val font = Minecraft.getInstance().font
                val owner: Entity? = vehicle.getOwner()

                if (owner != null) {
                    val color: Int = owner.getTeamColor()
                    val active: Boolean = vehicle.active

                    val info =
                        if (active) "tips.superbwarfare.auto_aimable_entity.active" else "tips.superbwarfare.auto_aimable_entity.inactive"
                    val component: Component = Component.translatable(info)
                    guiGraphics.drawString(font, component, -font.width(component) / 2, -5, color, false)

                    val ownerInfo: Component = Component.literal("[" + owner.displayName!!.string + "]")
                    guiGraphics.drawString(font, ownerInfo, -font.width(ownerInfo) / 2, 5, color, false)
                }

                poseStack.popPose()
            }
        }
    }
}
