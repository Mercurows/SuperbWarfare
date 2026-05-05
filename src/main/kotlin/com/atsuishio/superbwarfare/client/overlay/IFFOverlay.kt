package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModTags
import com.atsuishio.superbwarfare.network.message.receive.EntitySyncMessage
import com.atsuishio.superbwarfare.tools.*
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Camera
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import top.theillusivec4.curios.api.CuriosApi

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
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

    @SubscribeEvent
    fun onIFFClientTick(event: ClientTickEvent.Post) {
        val player = localPlayer ?: return
        val level = clientLevel ?: return
        CuriosApi.getCuriosInventory(player)
            .flatMap { c -> c.findFirstCurio(ModItems.IFF.get()) }
            .ifPresent { _ ->
                val clientEntities = SeekTool.Builder(player)
                    .friendly()
                    .notPlayer()
                    .build()
                    .asSequence()
                    .map {
                        EntitySyncMessage.SyncedEntity(
                            it.id,
                            BuiltInRegistries.ENTITY_TYPE.getKey(it.type),
                            it.position(),
                            it.deltaMovement,
                            CompoundTag().also { tag -> it.saveWithoutId(tag) }
                        )
                    }.toList()
                ClientSyncedEntityHandler.sync(level.dimension().location(), clientEntities, true)
            }
    }

    override fun shouldRender() = super.shouldRender() && DisplayConfig.IFF_HUD.get()

    override fun RenderContext.render() {
        val level = player.level()

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

        CuriosApi.getCuriosInventory(player)
            .flatMap { c -> c.findFirstCurio(ModItems.IFF.get()) }
            .ifPresent { _ ->
                val entities = ClientSyncedEntityHandler.getSyncedEntities(level)
                for (entity in entities) {
                    val e = level.getEntity(entity.id) ?: entity

                    if (e !== player && e.position().canBeSeen() && e !== player.vehicle) {
                        val teammate = e.vehicle ?: e

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

                        if (checkNoClip(player, teammate, cameraPos)) {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        } else {
                            RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f)
                        }

                        val pos = VectorTool.lerpGetEntityBoundingBoxCenter(teammate, partialTick)
                        val point = pos.worldToScreen()
                        val xf = point.x.toFloat()
                        val yf = point.y.toFloat()
                        val icon = getResourceLocation(teammate)

                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            icon,
                            (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                            (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                            0f,
                            0f,
                            12f,
                            12f,
                            12f,
                            12f,
                            0x7FFFAD
                        )

                        if (Vec2(xf, yf)
                                .distanceToSqr(Vec2(screenWidth.toFloat() / 2.0f, screenHeight.toFloat() / 2.0f)) < 12
                        ) {
                            poseStack.pushPose()
                            poseStack.translate(xf, yf, 0f)
                            poseStack.scale(0.75f, 0.75f, 1f)
                            val str =
                                "${e.displayName?.string ?: "---"} [${FormatTool.format1D(pos.distanceTo(cameraPos))}m]"
                            guiGraphics.drawString(
                                mc.font,
                                str,
                                -mc.font.width(str) / 2,
                                10,
                                0x7FFFAD,
                                false
                            )
                            poseStack.popPose()
                        }

                        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                    }
                }

                val players = ClientSyncedEntityHandler.getSyncedPlayerInfo(level)

                for (otherPlayers in players) {
                    if (otherPlayers.uuid != player.uuid) {
                        val localPlayer = EntityFindUtil.findPlayer(level, otherPlayers.uuid.toString())
                        var pos = otherPlayers.pos

                        if (localPlayer != null) {
                            pos = VectorTool.lerpGetEntityBoundingBoxCenter(localPlayer, partialTick)
                        }

                        if (pos.canBeSeen()) {
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

                            if (checkNoClip(player, pos, cameraPos)) {
                                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                            } else {
                                RenderSystem.setShaderColor(1f, 1f, 1f, 0.4f)
                            }

                            if (localPlayer != null && localPlayer.vehicle != null) {
                                pos = VectorTool.lerpGetEntityBoundingBoxCenter(localPlayer.vehicle!!, partialTick)
                            }

                            val point = pos.worldToScreen()
                            val xf = point.x.toFloat()
                            val yf = point.y.toFloat()

                            var height = 10

                            if (!otherPlayers.onVehicle) {
                                RenderHelper.preciseBlitWithColor(
                                    guiGraphics,
                                    FRIENDLY_INDICATOR,
                                    (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                                    (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                                    0f,
                                    0f,
                                    12f,
                                    12f,
                                    12f,
                                    12f,
                                    0x7FFFAD
                                )
                            } else {
                                height = 20
                            }

                            if (Vec2(xf, yf).distanceToSqr(
                                    Vec2(
                                        screenWidth.toFloat() / 2.0f,
                                        screenHeight.toFloat() / 2.0f
                                    )
                                ) < 12
                            ) {
                                poseStack.pushPose()
                                poseStack.translate(xf, yf, 0f)
                                poseStack.scale(0.75f, 0.75f, 1f)

                                val str: String = if (otherPlayers.isDriver) {
                                    otherPlayers.name
                                } else if (otherPlayers.onVehicle) {
                                    ""
                                } else {
                                    "${otherPlayers.name} [${FormatTool.format1D(pos.distanceTo(cameraPos))}m]"
                                }

                                guiGraphics.drawString(
                                    mc.font,
                                    str,
                                    -mc.font.width(str) / 2,
                                    height,
                                    0x7FFFAD,
                                    false
                                )
                                poseStack.popPose()
                            }

                            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                        }
                    }
                }

                val hostileEntities = ClientSyncedEntityHandler.getSyncedHostileEntities(player.level())
                for (entity in hostileEntities) {
                    val e = level.getEntity(entity.id) ?: entity

                    if (e !== player && e.position().canBeSeen() && e !== player.vehicle) {
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

                        val pos = VectorTool.lerpGetEntityBoundingBoxCenter(enemy, partialTick)
                        val point = pos.worldToScreen()
                        val xf = point.x.toFloat()
                        val yf = point.y.toFloat()
                        val icon = getResourceLocation(enemy)

                        var color = 0xFFBD7F

                        if (e is VehicleEntity && ((e.firstPassenger == null && e.team == null) || (e is OwnableEntity && e.owner == null))) {
                            color = -1
                        }

                        RenderHelper.preciseBlitWithColor(
                            guiGraphics,
                            icon,
                            (xf - 6).coerceIn(0f, (screenWidth - 12).toFloat()),
                            (yf - 6).coerceIn(0f, (screenHeight - 12).toFloat()),
                            0f,
                            0f,
                            12f,
                            12f,
                            12f,
                            12f,
                            color
                        )

                        if (Vec2(xf, yf).distanceToSqr(
                                Vec2(
                                    screenWidth.toFloat() / 2.0f,
                                    screenHeight.toFloat() / 2.0f
                                )
                            ) < 12
                        ) {
                            poseStack.pushPose()
                            poseStack.translate(xf, yf, 0f)
                            poseStack.scale(0.75f, 0.75f, 1f)
                            val str =
                                "${e.displayName?.string ?: "---"} [${FormatTool.format1D(pos.distanceTo(cameraPos))}m]"
                            guiGraphics.drawString(
                                mc.font,
                                str,
                                -mc.font.width(str) / 2,
                                10,
                                color,
                                false
                            )
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
            ClipContext(pos, toPos, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, CollisionContext.empty())
        ).type != HitResult.Type.BLOCK
    }

    fun checkNoClip(player: Player, targetPos: Vec3, pos: Vec3): Boolean {
        val vec = pos.vectorTo(targetPos)
        val toPos = if (vec.lengthSqr() > 512 * 512)
            pos.add(pos.vectorTo(targetPos).normalize().scale(512.0))
        else targetPos
        return player.level().clip(
            ClipContext(pos, toPos, ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, CollisionContext.empty())
        ).type != HitResult.Type.BLOCK
    }

    fun calculateAngle(entityA: Entity, camera: Camera): Double {
        val v1 = camera.position.vectorTo(entityA.position())
        val v2 = Vec3(camera.lookVector)
        return v1.angleTo(v2)
    }
}
