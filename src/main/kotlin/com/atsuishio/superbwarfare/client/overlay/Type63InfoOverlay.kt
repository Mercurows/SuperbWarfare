package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils.getXRotFromVector
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.FiringParametersItem
import com.atsuishio.superbwarfare.item.firingParameters
import com.atsuishio.superbwarfare.tools.FormatTool.format0D
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.FormatTool.format2D
import com.atsuishio.superbwarfare.tools.OBB

import com.atsuishio.superbwarfare.tools.RangeTool.getRange
import com.atsuishio.superbwarfare.tools.TraceTool
import com.atsuishio.superbwarfare.tools.TrajectoryCalculator.calculateLaunchVector
import com.atsuishio.superbwarfare.tools.worldToScreen
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import kotlin.math.max

@OnlyIn(Dist.CLIENT)
object Type63InfoOverlay : CommonOverlay("type_63_info") {
    private val AP = ItemStack(ModItems.MEDIUM_ROCKET_AP.get())
    private val HE = ItemStack(ModItems.MEDIUM_ROCKET_HE.get())
    private val CM = ItemStack(ModItems.MEDIUM_ROCKET_CM.get())

    override fun RenderContext.render() {
        val poseStack = guiGraphics.pose()

        val lookingEntity = TraceTool.findLookingEntity(player, player.getEntityReach())

        if (lookingEntity !is Type63Entity) return
        guiGraphics.drawString(
            Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.pitch")
                .append(
                    Component.literal(
                        format2D(
                            lookingEntity.getEntityData().get(Type63Entity.SHOOT_PITCH).toDouble(), "°"
                        )
                    )
                ),
            screenWidth / 2 - 130, screenHeight / 2 - 26, -1, false
        )
        guiGraphics.drawString(
            Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.yaw")
                .append(
                    Component.literal(
                        format2D(
                            lookingEntity.getEntityData().get(Type63Entity.SHOOT_YAW).toDouble(), "°"
                        )
                    )
                ),
            screenWidth / 2 - 130, screenHeight / 2 - 16, -1, false
        )
        guiGraphics.drawString(
            Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.range")
                .append(
                    Component.literal(
                        format1D(
                            max(
                                getRange(
                                    lookingEntity.getEntityData().get(Type63Entity.SHOOT_PITCH).toDouble(),
                                        lookingEntity.getProjectileVelocity("Main").toDouble(),
                                        lookingEntity.getProjectileGravity("Main").toDouble()
                                ).toInt(), 0
                            ).toDouble(), "m"
                        )
                    )
                ),
            screenWidth / 2 - 130, screenHeight / 2 - 6, -1, false
        )

        val items = lookingEntity.getEntityData().get(Type63Entity.LOADED_AMMO)
        for (i in lookingEntity.barrel.indices) {
            if (OBB.getLookingObb(player, player.getEntityReach()) === lookingEntity.barrel[i]) {
                val type: Int = items[i]!!

                val stack = when (type) {
                    0 -> AP
                    1 -> HE
                    2 -> CM
                    else -> ItemStack.EMPTY
                }

                val pos = OBB.vector3dToVec3(lookingEntity.barrel[i].center)
                val point = pos.worldToScreen()

                poseStack.pushPose()
                val x = point.x.toFloat()
                val y = point.y.toFloat()

                var component = stack.getHoverName()

                if (stack.isEmpty) {
                    component = Component.translatable("tips.superbwarfare.barrel_empty")
                    val width = Minecraft.getInstance().font.width(component)

                    poseStack.translate(x - width / 2f, y, 0f)
                    guiGraphics.drawString(Minecraft.getInstance().font, component, 0, 0, -1, false)
                } else {
                    val width = Minecraft.getInstance().font.width(component) + 20

                    poseStack.pushPose()
                    poseStack.translate(x - width / 2f, y, 0f)
                    guiGraphics.renderFakeItem(stack, 0, 0)

                    poseStack.translate(20f, 4f, 0f)
                    guiGraphics.drawString(Minecraft.getInstance().font, component, 0, 0, -1, false)
                }

                poseStack.popPose()
            }
        }

        var stack = player.offhandItem

        if (player.mainHandItem.item is FiringParametersItem) {
            stack = player.mainHandItem
        }

        if (stack.item is FiringParametersItem) {
            val parameters = stack.firingParameters
            val targetX = parameters.pos.x.toDouble()
            val targetY = (parameters.pos.y - 1).toDouble()
            val targetZ = parameters.pos.z.toDouble()
            val isDepressed = parameters.isDepressed

            val targetPos = Vec3(targetX, targetY, targetZ)
            val launchVector =
                calculateLaunchVector(lookingEntity.getShootPos(partialTick), targetPos,
                        lookingEntity.getProjectileVelocity("Main").toDouble(),
                        lookingEntity.getProjectileGravity("Main").toDouble(),
                        isDepressed)

            val vec3 = EntityAnchorArgument.Anchor.EYES.apply(lookingEntity)
            val d0 = (targetPos.x - vec3.x) * 0.2
            val d2 = (targetPos.z - vec3.z) * 0.2
            val targetYaw = Mth.wrapDegrees((Mth.atan2(d2, d0) * 57.2957763671875).toFloat() - 90f).toDouble()

            val angle: Float

            if (launchVector != null) {
                angle = getXRotFromVector(launchVector).toFloat()
            } else {
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable("tips.superbwarfare.mortar.out_of_range").withStyle(ChatFormatting.RED),
                    screenWidth / 2 + 90,
                    screenHeight / 2 - 26,
                    -1,
                    false
                )
                return
            }

            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.target.pitch")
                    .append(Component.literal(format2D(angle.toDouble(), "°"))),
                screenWidth / 2 + 90, screenHeight / 2 - 26, -1, false
            )
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.target.yaw")
                    .append(Component.literal(format2D(targetYaw, "°"))),
                screenWidth / 2 + 90, screenHeight / 2 - 16, -1, false
            )
            guiGraphics.drawString(
                Minecraft.getInstance().font, Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .append(Component.literal(format0D(targetX) + " " + format0D(targetY) + " " + format0D(targetZ))),
                screenWidth / 2 + 90, screenHeight / 2 - 6, -1, false
            )

            if (angle < -5 || angle > 60) {
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable("tips.superbwarfare.mortar.warn", lookingEntity.displayName)
                        .withStyle(ChatFormatting.RED),
                    screenWidth / 2 + 90,
                    screenHeight / 2 + 4,
                    -1,
                    false
                )
                if (angle > 60 && !isDepressed) {
                    guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        Component.translatable("tips.superbwarfare.ballistics.warn").withStyle(ChatFormatting.RED),
                        screenWidth / 2 + 90,
                        screenHeight / 2 + 14,
                        -1,
                        false
                    )
                }
            }
        }
    }
}
