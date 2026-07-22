package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler.ClientSyncedPlayer
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.tools.*
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Camera
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import top.theillusivec4.curios.api.CuriosApi

@OnlyIn(Dist.CLIENT)
object IFFOverlay : CommonOverlay("iff") {
    val FRIENDLY_INDICATOR = loc("textures/overlay/teammate/friendly_indicator.png")
    val FRIENDLY_AIRCRAFT = loc("textures/overlay/teammate/friendly_aircraft.png")
    val FRIENDLY_TANK = loc("textures/overlay/teammate/friendly_tank.png")
    val FRIENDLY_APC = loc("textures/overlay/teammate/friendly_apc.png")
    val FRIENDLY_AA = loc("textures/overlay/teammate/friendly_aa.png")
    val FRIENDLY_CAR = loc("textures/overlay/teammate/friendly_car.png")
    val FRIENDLY_ARTILLERY = loc("textures/overlay/teammate/friendly_artillery.png")
    val FRIENDLY_BOAT = loc("textures/overlay/teammate/friendly_boat.png")
    val FRIENDLY_DEFENSE = loc("textures/overlay/teammate/friendly_defense.png")
    val FRIENDLY_DRONE = loc("textures/overlay/teammate/friendly_drone.png")
    val FRIENDLY_HELICOPTER = loc("textures/overlay/teammate/friendly_helicopter.png")
    val FRIENDLY_MINE = loc("textures/overlay/teammate/friendly_mine.png")
    val FRIENDLY_MISSILE = loc("textures/overlay/teammate/friendly_missile.png")
    val FRIENDLY_MAID = loc("textures/overlay/teammate/friendly_maid.png")
    val FRIENDLY_AIRSHIP = loc("textures/overlay/teammate/friendly_airship.png")

    override fun shouldRender() = super.shouldRender() && DisplayConfig.IFF_HUD.get()

    override fun RenderContext.render() {
        val level = player.level()

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        CuriosApi.getCuriosInventory(player).ifPresent { c ->
            c.findFirstCurio(ModItems.IFF.get()).ifPresent { _ ->
                // ── 友方实体（绿色）──
                var friendlyEntities = ClientSyncedEntityHandler.getSyncedFriendlyEntities(level)
                val clientEntities = SeekTool.Builder(player)
                    .friendly()
                    .notPlayer()
                    .build().toList()

                friendlyEntities = (friendlyEntities + clientEntities).distinctBy { it.id }

                for (entity in friendlyEntities) {
                    val teammate = level.getEntity(entity.id) ?: entity
                    if (teammate !== player && teammate.position().canBeSeen() && teammate !== player.vehicle && teammate.vehicle == null) {

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
                        RenderSystem.setShaderColor(1f, 1f, 1f, if (checkNoClip(player, teammate, cameraPos)) 1f else 0.4f)

                        val pos = if (level.getEntity(teammate.id) != null)
                            VectorTool.lerpGetEntityBoundingBoxCenter(teammate, partialTick)
                        else
                            ClientSyncedEntityHandler.getExtrapolatedPos(level, teammate)
                                .add(0.0, teammate.bbHeight / 2.0, 0.0)

                        val point = pos.worldToScreen()
                        val xf = point.x.toFloat()
                        val yf = point.y.toFloat()
                        val icon = getResourceLocation(teammate)

                        RenderHelper.blit(
                            guiGraphics.pose(), icon,
                            (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                            (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                            0f, 0f, 12f, 12f, 12f, 12f,
                            0x7FFFAD
                        )

                        if (Vec2(xf, yf).distanceToSqr(
                                Vec2(screenWidth.toFloat() / 2.0f, screenHeight.toFloat() / 2.0f)
                            ) < 12
                        ) {
                            poseStack.pushPose()
                            poseStack.translate(xf, yf, 0f)
                            poseStack.scale(0.75f, 0.75f, 1f)
                            val str = "${teammate.displayName.string} [${FormatTool.format1DZ(pos.distanceTo(cameraPos))}m]"
                            guiGraphics.drawString(mc.font, str, -mc.font.width(str) / 2, 10, 0x7FFFAD, false)
                            poseStack.popPose()
                        }
                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                    }
                }

                // ── 队友玩家（来自 SYNCED_PLAYERS）──
                val syncedPlayers = ClientSyncedEntityHandler.getSyncedPlayerInfo(level)
                for (otherPlayer in syncedPlayers) {
                    if (otherPlayer.uuid == player.uuid) continue
                    val color = when (otherPlayer.relation) {
                        "hostile" -> 0xFFBD7F
                        "neutral" -> -0x1
                        else -> 0x7FFFAD
                    }
                    renderSyncedPlayer(
                        otherPlayer, color, player, level, cameraPos,
                        poseStack, guiGraphics, partialTick
                    )
                }

                val hostileEntities = ClientSyncedEntityHandler.getSyncedHostileEntities(player.level())
                for (entity in hostileEntities) {
                    val e = level.getEntity(entity.id) ?: entity

                    if (e !== player && e.position().canBeSeen() && e !== player.vehicle && e.vehicle == null) {
                        val enemy = e.vehicle ?: e

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

                        if (checkNoClip(player, enemy, cameraPos)) {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        } else {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f)
                        }

                        val pos = if (level.getEntity(e.id) != null)
                            VectorTool.lerpGetEntityBoundingBoxCenter(enemy, partialTick)
                        else
                            ClientSyncedEntityHandler.getExtrapolatedPos(level, enemy)
                                .add(0.0, enemy.bbHeight / 2.0, 0.0)
                        val point = pos.worldToScreen()
                        val xf = point.x.toFloat()
                        val yf = point.y.toFloat()
                        val icon = getResourceLocation(enemy)

                        RenderHelper.blit(
                            guiGraphics.pose(),
                            icon,
                            (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                            (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                            0f, 0f, 12f, 12f, 12f, 12f,
                            0xFFBD7F
                        )

                        if (Vec2(xf, yf).distanceToSqr(
                                Vec2(screenWidth.toFloat() / 2.0f, screenHeight.toFloat() / 2.0f)
                            ) < 12
                        ) {
                            poseStack.pushPose()
                            poseStack.translate(xf, yf, 0f)
                            poseStack.scale(0.75f, 0.75f, 1f)
                            val str = "${e.displayName.string} [${FormatTool.format1DZ(pos.distanceTo(cameraPos))}m]"
                            guiGraphics.drawString(mc.font, str, -mc.font.width(str) / 2, 10, 0xFFBD7F, false)
                            poseStack.popPose()
                        }

                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                    }
                }

                // 中立实体（无人驾驶、无主人的载具）
                val neutralEntities = ClientSyncedEntityHandler.getSyncedNeutralEntities(player.level())
                for (entity in neutralEntities) {
                    val e = level.getEntity(entity.id) ?: entity
                    if (e === player || !e.position().canBeSeen() || e === player.vehicle || e.vehicle != null) continue
                    val neutral = e.vehicle ?: e

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
                    RenderSystem.setShaderColor(1f, 1f, 1f, if (checkNoClip(player, neutral, cameraPos)) 1f else 0.4f)

                    val pos = if (level.getEntity(e.id) != null)
                        VectorTool.lerpGetEntityBoundingBoxCenter(neutral, partialTick)
                    else
                        ClientSyncedEntityHandler.getExtrapolatedPos(level, neutral)
                            .add(0.0, neutral.bbHeight / 2.0, 0.0)
                    val point = pos.worldToScreen()
                    val xf = point.x.toFloat()
                    val yf = point.y.toFloat()
                    val icon = getResourceLocation(neutral)

                    RenderHelper.blit(
                        guiGraphics.pose(), icon,
                        (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                        (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                        0f, 0f, 12f, 12f, 12f, 12f,
                        -1 // 白色 = 中立
                    )

                    if (Vec2(xf, yf).distanceToSqr(
                            Vec2(screenWidth.toFloat() / 2.0f, screenHeight.toFloat() / 2.0f)
                        ) < 12
                    ) {
                        poseStack.pushPose()
                        poseStack.translate(xf, yf, 0f)
                        poseStack.scale(0.75f, 0.75f, 1f)
                        val str = "${e.displayName.string} [${FormatTool.format1DZ(pos.distanceTo(cameraPos))}m]"
                        guiGraphics.drawString(mc.font, str, -mc.font.width(str) / 2, 10, -1, false)
                        poseStack.popPose()
                    }

                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                }
            }
        }

        poseStack.popPose()
    }

    private fun getResourceLocation(entity: Entity): ResourceLocation {
        return if (entity is Boat) {
            FRIENDLY_BOAT
        } else if (entity is VehicleEntity) {
            when (entity.vehicleType) {
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
                VehicleType.AIRSHIP -> FRIENDLY_AIRSHIP
                else -> FRIENDLY_INDICATOR
            }
        } else if (entity.type.`is`(ModTags.EntityTypes.MINE)) {
            FRIENDLY_MINE
        } else if (entity is MissileProjectile) {
            FRIENDLY_MISSILE
        } else if (entity.type.descriptionId == "entity.touhou_little_maid.maid") {
            FRIENDLY_MAID
        } else {
            FRIENDLY_INDICATOR
        }
    }

    fun checkNoClip(player: Player, teammate: Entity, pos: Vec3): Boolean {
        val vec = pos.vectorTo(teammate.position())
        val toPos = if (vec.lengthSqr() > 512 * 512)
            pos.add(pos.vectorTo(teammate.position()).normalize().scale(512.0))
        else teammate.position()
        return player.level().clip(
            ClipContext(
                pos, toPos,
                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, null
            )
        ).type != HitResult.Type.BLOCK
    }

    fun checkNoClip(player: Player, targetPos: Vec3, pos: Vec3): Boolean {
        val vec = pos.vectorTo(targetPos)
        val toPos = if (vec.lengthSqr() > 512 * 512)
            pos.add(pos.vectorTo(targetPos).normalize().scale(512.0))
        else targetPos
        return player.level().clip(
            ClipContext(
                pos, toPos,
                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, null
            )
        ).type != HitResult.Type.BLOCK
    }

    fun calculateAngle(entityA: Entity, camera: Camera): Double {
        val v1 = camera.position.vectorTo(entityA.position())
        val v2 = Vec3(camera.lookVector)
        return v1.angleTo(v2)
    }

    /**
     * 渲染从 [ClientSyncedEntityHandler.SYNCED_PLAYERS] 同步过来的单个玩家标记。
     * 根据 relation 使用不同颜色：friendly=绿, hostile=橙, neutral=白。
     */
    private fun RenderContext.renderSyncedPlayer(
        info: ClientSyncedPlayer,
        color: Int,
        localPlayer: Player,
        level: net.minecraft.world.level.Level,
        cameraPos: Vec3,
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        guiGraphics: net.minecraft.client.gui.GuiGraphics,
        partialTick: Float,
    ) {
        val foundPlayer = EntityFindUtil.findPlayer(level, info.uuid.toString())
        var pos = info.pos

        if (foundPlayer != null) {
            pos = VectorTool.lerpGetEntityBoundingBoxCenter(foundPlayer, partialTick)
        }

        if (!pos.canBeSeen()) return

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

        if (checkNoClip(localPlayer, pos, cameraPos)) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f)
        }

        if (foundPlayer != null && foundPlayer.vehicle != null) {
            pos = VectorTool.lerpGetEntityBoundingBoxCenter(foundPlayer.vehicle!!, partialTick)
        }

        val point = pos.worldToScreen()
        val xf = point.x.toFloat()
        val yf = point.y.toFloat()

        var height = 10

        if (!info.onVehicle) {
            RenderHelper.blit(
                guiGraphics.pose(), FRIENDLY_INDICATOR,
                (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                0f, 0f, 12f, 12f, 12f, 12f,
                color
            )
        } else {
            height = 20
        }

        if (Vec2(xf, yf).distanceToSqr(
                Vec2(screenWidth.toFloat() / 2.0f, screenHeight.toFloat() / 2.0f)
            ) < 12
        ) {
            poseStack.pushPose()
            poseStack.translate(xf, yf, 0f)
            poseStack.scale(0.75f, 0.75f, 1f)

            val str: String = if (info.isDriver) {
                info.name
            } else if (info.onVehicle) {
                ""
            } else {
                "${info.name} [${FormatTool.format1DZ(pos.distanceTo(cameraPos))}m]"
            }

            guiGraphics.drawString(mc.font, str, -mc.font.width(str) / 2, height, color, false)
            poseStack.popPose()
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    }
}
