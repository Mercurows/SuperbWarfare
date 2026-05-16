package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.config.server.VehicleConfig
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.tools.*
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.VectorTool.lerpGetEntityBoundingBoxCenter
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Team
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
object VehicleTeamOverlay : CommonOverlay("vehicle_team") {
    override fun shouldRender() = super.shouldRender() && DisplayConfig.VEHICLE_INFO.get()

    private var lookingEntity: Entity? = null
    private var entityRange = 0.0
    private var lookAtEntity = false

    @SubscribeEvent
    fun onVehicleTeamOverlayClientTick(event: ClientTickEvent.Post) {
        val player = localPlayer ?: return
        val camera = mc.gameRenderer.mainCamera
        var viewPos = camera.position
        var viewVec = Vec3(camera.lookVector)
        val distance = try {
            VehicleConfig.VEHICLE_INFO_DISPLAY_DISTANCE.get().toDouble()
        } catch (_: Exception) {
            196.0
        }

        lookingEntity = TraceTool.camerafFindLookingEntity(
            player,
            viewPos,
            viewVec,
            distance
        )

        (player.vehicle as? VehicleEntity)?.let { vehicle ->
            if (vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
                viewVec = vehicle.getShootDirectionForHud(player, 1f)
                viewPos = vehicle.getShootPosForHud(player, 1f)
                lookingEntity = TraceTool.camerafFindLookingEntity(
                    player,
                    viewPos,
                    viewVec,
                    distance
                )
            }
        }

        if (lookingEntity is VehicleEntity) {
            val decoy = TraceTool.findLookDecoy(player, viewPos, viewVec, distance)
            if (decoy == null) {
                lookAtEntity = true
                entityRange = player.distanceTo(lookingEntity!!).toDouble()
            } else {
                lookAtEntity = false
            }
        } else {
            lookAtEntity = false
        }
    }

    override fun RenderContext.render() {
        if (!lookAtEntity) return
        val lookingEntity = lookingEntity as VehicleEntity

        val stack = player.mainHandItem
        val usingDrone = stack.`is`(ModItems.MONITOR.get())
                && NBTTool.getTag(stack).getBoolean("Using")
                && NBTTool.getTag(stack).getBoolean("Linked")

        val poseStack = guiGraphics.pose()
        if (!usingDrone) {
            val pos = lerpGetEntityBoundingBoxCenter(lookingEntity, partialTick)
                .add(Vec3(0.0, lookingEntity.bbHeight / 2 + 0.5, 0.0))

            val centerPos = lerpGetEntityBoundingBoxCenter(lookingEntity, partialTick)

            if (pos.canBeSeen()) {
                val point = pos.worldToScreen()

                val x = point.x.toFloat()
                val y = point.y.toFloat()

                poseStack.pushPose()
                poseStack.translate(x, y - 12, 0f)

                val size = ((50 / ClientEventHandler.fov) * 0.9f * max((512 - entityRange) / 512, 0.1)
                    .coerceIn(0.4, 1.0)).toFloat()
                poseStack.scale(size, size, size)
                val font = mc.font

                var color = -1

                if (lookingEntity is DroneEntity) {
                    val controller = EntityFindUtil.findPlayer(
                        lookingEntity.level(),
                        lookingEntity.getEntityData().get(DroneEntity.CONTROLLER)
                    )
                    if (controller != null) {
                        color = controller.teamColor

                        val team: Team? = controller.team
                        if (team is PlayerTeam) {
                            val info =
                                "${lookingEntity.displayName?.string} ${controller.displayName?.string}${if (controller.team == null) "" else " <${team.displayName.string}>"}"
                            guiGraphics.drawString(
                                font,
                                Component.literal(info),
                                -font.width(info) / 2,
                                -13,
                                color,
                                false
                            )
                        } else {
                            val info = "${lookingEntity.displayName?.string} ${controller.displayName?.string}"
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
                    val player1 = lookingEntity.owner
                    if (player1 is Player) {
                        color = player1.teamColor
                        val team: Team? = player1.team
                        if (team is PlayerTeam) {
                            val info =
                                "${lookingEntity.displayName?.string} ${player1.displayName?.string}${if (player1.team == null) "" else " <${team.displayName.string}>"}"
                            guiGraphics.drawString(
                                font,
                                Component.literal(info),
                                -font.width(info) / 2,
                                -13,
                                color,
                                false
                            )
                        } else {
                            val info = "${lookingEntity.displayName?.string} ${player1.displayName?.string}"
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
                        color = player1.teamColor
                        val team: Team? = player1.team
                        if (team is PlayerTeam) {
                            val info =
                                "${lookingEntity.displayName?.string} ${player1.displayName?.string}${if (player1.team == null) "" else " <${team.displayName.string}>"}"
                            guiGraphics.drawString(
                                font,
                                Component.literal(info),
                                -font.width(info) / 2,
                                -13,
                                color,
                                false
                            )
                        } else {
                            val info = "${lookingEntity.displayName?.string} ${player1.displayName?.string}"
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
                val health = lookingEntity.health
                val maxHealth = lookingEntity.getMaxHealth()
                RenderHelper.fill(
                    guiGraphics,
                    RenderType.guiOverlay(),
                    -40f,
                    -1.5f,
                    -40 + 80 * ((if (lookingEntity.isWreck) (health + maxHealth) else health) / maxHealth),
                    1.5f,
                    0f,
                    argb
                )

                poseStack.popPose()
            }

            if (lookingEntity is AutoAimableEntity && centerPos.canBeSeen() && player.distanceTo(lookingEntity) < 4) {
                val point = centerPos.worldToScreen()

                val x = point.x.toFloat()
                val y = point.y.toFloat()

                poseStack.pushPose()
                poseStack.translate(x, y - 12, 0f)

                val font = mc.font
                val owner: Entity? = lookingEntity.owner

                if (owner != null) {
                    val color: Int = owner.teamColor
                    val active: Boolean = lookingEntity.active

                    val info =
                        if (active) "tips.superbwarfare.auto_aimable_entity.active" else "tips.superbwarfare.auto_aimable_entity.inactive"
                    val component: Component = Component.translatable(info)
                    guiGraphics.drawString(font, component, -font.width(component) / 2, -5, color, false)

                    val ownerInfo: Component = Component.literal("[${owner.displayName?.string}]")
                    guiGraphics.drawString(font, ownerInfo, -font.width(ownerInfo) / 2, 5, color, false)
                }

                poseStack.popPose()
            }
        }
    }
}
