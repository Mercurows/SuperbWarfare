package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.animation.AnimationCurves
import com.atsuishio.superbwarfare.client.animation.AnimationTimer
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.AmmoConsumer
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.Aircraft
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineInfo.Helicopter
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.tools.FormatTool
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Math
import top.theillusivec4.curios.api.CuriosApi

@OnlyIn(Dist.CLIENT)
object VehicleHudOverlay : CommonOverlay("vehicle_hud") {
    const val ANIMATION_TIME = 300

    private val ARMOR = loc("textures/overlay/vehicle/base/armor.png")
    private val ENERGY = loc("textures/overlay/vehicle/base/energy.png")
    private val VALUE_BAR = loc("textures/overlay/vehicle/base/value_bar.png")
    private val VALUE_FRAME = loc("textures/overlay/vehicle/base/value_frame.png")

    private val DRIVER = loc("textures/overlay/vehicle/base/driver.png")
    private val PASSENGER = loc("textures/overlay/vehicle/base/passenger.png")

    private val SELECTED = loc("textures/overlay/vehicle/weapon/frame/selected.png")
    private val NUMBER = loc("textures/overlay/vehicle/weapon/frame/number.png")
    private val FRACTION = loc("textures/overlay/vehicle/weapon/frame/fraction.png")
    private val PLUS = loc("textures/overlay/vehicle/weapon/frame/plus.png")

    private val FRAMES = arrayOf(
        loc("textures/overlay/vehicle/weapon/frame/frame_1.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_2.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_3.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_4.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_5.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_6.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_7.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_8.png"),
        loc("textures/overlay/vehicle/weapon/frame/frame_9.png")
    )

    private val HIT_MARKER = loc("textures/overlay/crosshair/hit_marker.png")
    private val HIT_MARKER_VEHICLE = loc("textures/overlay/crosshair/hit_marker_vehicle.png")
    private val HEADSHOT_MARKER = loc("textures/overlay/crosshair/headshot_marker.png")
    private val KILL_MARKER_1 = loc("textures/overlay/crosshair/kill_marker_1.png")
    private val KILL_MARKER_2 = loc("textures/overlay/crosshair/kill_marker_2.png")
    private val KILL_MARKER_3 = loc("textures/overlay/crosshair/kill_marker_3.png")
    private val KILL_MARKER_4 = loc("textures/overlay/crosshair/kill_marker_4.png")

    private val WEAPON_SLOTS_TIMER: Array<AnimationTimer> =
        AnimationTimer.createTimers(9, ANIMATION_TIME.toLong(), AnimationCurves.EASE_OUT_CIRC)
    private val WEAPON_INDEX_UPDATE_TIMER: AnimationTimer =
        AnimationTimer(ANIMATION_TIME.toLong()).animation(AnimationCurves.EASE_OUT_CIRC)

    private var wasRenderingWeapons = false
    private var oldWeaponIndex = 0
    private var oldRenderWeaponIndex = 0

    override fun RenderContext.render() {
        if (!shouldRenderHud(player)) {
            wasRenderingWeapons = false
            return
        }

        val entity = player.vehicle
        if (entity !is VehicleEntity) return

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()

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
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

        val compatHeight: Int = getArmorPlateCompatHeight(player)

        if (entity.hasEnergyStorage()) {
            val energy = entity.energy.toFloat()
            val maxEnergy = entity.maxEnergy.toFloat()

            RenderHelper.preciseBlit(
                guiGraphics,
                ENERGY,
                10f,
                (screenHeight - 22 - compatHeight).toFloat(),
                100f,
                0f,
                0f,
                8f,
                8f,
                8f,
                8f
            )
            RenderHelper.preciseBlit(
                guiGraphics,
                VALUE_FRAME,
                20f,
                (screenHeight - 21 - compatHeight).toFloat(),
                100f,
                0f,
                0f,
                60f,
                6f,
                60f,
                6f
            )
            RenderHelper.preciseBlit(
                guiGraphics,
                VALUE_BAR,
                20f,
                (screenHeight - 21 - compatHeight).toFloat(),
                100f,
                0f,
                0f,
                (60 * energy / maxEnergy).toInt().toFloat(),
                6f,
                60f,
                6f
            )
        }

        val health = entity.health
        val maxHealth = entity.getMaxHealth()

        RenderHelper.preciseBlit(
            guiGraphics,
            ARMOR,
            10f,
            (screenHeight - 13 - compatHeight).toFloat(),
            100f,
            0f,
            0f,
            8f,
            8f,
            8f,
            8f
        )
        RenderHelper.preciseBlit(
            guiGraphics,
            VALUE_FRAME,
            20f,
            (screenHeight - 12 - compatHeight).toFloat(),
            100f,
            0f,
            0f,
            60f,
            6f,
            60f,
            6f
        )
        RenderHelper.preciseBlit(
            guiGraphics,
            VALUE_BAR,
            20f,
            (screenHeight - 12 - compatHeight).toFloat(),
            100f,
            0f,
            0f,
            (60 * health / maxHealth).toInt().toFloat(),
            6f,
            60f,
            6f
        )

        renderWeaponInfo(guiGraphics, entity, screenWidth, screenHeight)
        renderPassengerInfo(guiGraphics, entity, screenWidth, screenHeight)
        renderGearInfo(guiGraphics, entity, screenWidth, screenHeight, partialTick, compatHeight)
        renderHoverInfo(guiGraphics, entity, screenWidth, screenHeight, partialTick, compatHeight)
        renderSpeedInfo(guiGraphics, entity, screenWidth, screenHeight, partialTick, compatHeight)

        poseStack.popPose()
    }


    private fun shouldRenderHud(player: Player?): Boolean {
        if (player == null) return false
        return !player.isSpectator && player.vehicle is VehicleEntity
    }

    private fun getArmorPlateCompatHeight(player: Player): Int {
        val stack = player.getItemBySlot(EquipmentSlot.CHEST)
        if (stack == ItemStack.EMPTY) return 0
        if (stack.tag == null || !stack.tag!!.contains("ArmorPlate")) return 0
        if (!DisplayConfig.ARMOR_PLATE_HUD.get()) return 0
        return 9
    }

    @JvmStatic
    fun renderKillIndicator(guiGraphics: GuiGraphics?, w: Float, h: Float) {
        val posX = w / 2f - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
        val posY = h / 2f - 7.5f + (2 * (Math.random() - 0.5f)).toFloat()
        val rate = (40 - CrossHairOverlay.killIndicator * 5) / 5.5f

        if (CrossHairOverlay.hitIndicator > 0) {
            RenderHelper.preciseBlit(guiGraphics, HIT_MARKER, posX, posY, 0f, 0f, 16f, 16f, 16f, 16f)
        }

        if (CrossHairOverlay.vehicleIndicator > 0) {
            RenderHelper.preciseBlit(guiGraphics, HIT_MARKER_VEHICLE, posX, posY, 0f, 0f, 16f, 16f, 16f, 16f)
        }

        if (CrossHairOverlay.headIndicator > 0) {
            RenderHelper.preciseBlit(guiGraphics, HEADSHOT_MARKER, posX, posY, 0f, 0f, 16f, 16f, 16f, 16f)
        }

        if (CrossHairOverlay.killIndicator > 0) {
            val posX1 = w / 2f - 7.5f - 2 + rate
            val posY1 = h / 2f - 7.5f - 2 + rate
            val posX2 = w / 2f - 7.5f + 2 - rate
            val posY2 = h / 2f - 7.5f + 2 - rate

            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_1, posX1, posY1, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_2, posX2, posY1, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_3, posX1, posY2, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_4, posX2, posY2, 0f, 0f, 16f, 16f, 16f, 16f)
        }
    }

    @JvmStatic
    fun renderKillIndicatorDynamic(guiGraphics: GuiGraphics?, posX: Float, posY: Float) {
        val rate = (40 - CrossHairOverlay.killIndicator * 5) / 5.5f

        if (CrossHairOverlay.hitIndicator > 0) {
            RenderHelper.preciseBlit(guiGraphics, HIT_MARKER, posX, posY, 0f, 0f, 16f, 16f, 16f, 16f)
        }

        if (CrossHairOverlay.vehicleIndicator > 0) {
            RenderHelper.preciseBlit(guiGraphics, HIT_MARKER_VEHICLE, posX, posY, 0f, 0f, 16f, 16f, 16f, 16f)
        }

        if (CrossHairOverlay.headIndicator > 0) {
            RenderHelper.preciseBlit(guiGraphics, HEADSHOT_MARKER, posX, posY, 0f, 0f, 16f, 16f, 16f, 16f)
        }

        if (CrossHairOverlay.killIndicator > 0) {
            val posX1 = posX - 2 + rate
            val posY1 = posY - 2 + rate
            val posX2 = posX + 2 - rate
            val posY2 = posY + 2 - rate

            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_1, posX1, posY1, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_2, posX2, posY1, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_3, posX1, posY2, 0f, 0f, 16f, 16f, 16f, 16f)
            RenderHelper.preciseBlit(guiGraphics, KILL_MARKER_4, posX2, posY2, 0f, 0f, 16f, 16f, 16f, 16f)
        }
    }

    private fun renderPassengerInfo(guiGraphics: GuiGraphics, vehicle: VehicleEntity, w: Int, h: Int) {
        val passengers = vehicle.getOrderedPassengers()

        for ((index, i) in passengers.indices.reversed().withIndex()) {
            val passenger = passengers[i]

            val y = h - 35 - index * 12
            var name = "---"

            if (passenger != null) {
                name = passenger.name.string
            }

            if (passenger is Player) {
                CuriosApi.getCuriosInventory(passenger).ifPresent { c ->
                    c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                        if (s.stack().hasCustomHoverName()) {
                            name = s.stack().getHoverName().string
                        }
                    }
                }
            }

            guiGraphics.drawString(mc.font, name, 42, y, 0x66ff00, true)

            val num = "[" + (i + 1) + "]"
            guiGraphics.drawString(
                mc.font,
                num,
                25 - mc.font.width(num),
                y,
                0x66ff00,
                true
            )

            RenderHelper.preciseBlit(
                guiGraphics,
                if (index == passengers.size - 1) DRIVER else PASSENGER,
                30f,
                y.toFloat(),
                100f,
                0f,
                0f,
                8f,
                8f,
                8f,
                8f
            )
        }
    }

    private fun renderGearInfo(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        w: Int,
        h: Int,
        partialTick: Float,
        compatHeight: Int
    ) {
        val engineType = vehicle.computed().engineType
        if (engineType != EngineType.AIRCRAFT) return
        val engineInfo = vehicle.engineInfo ?: return
        if (engineInfo !is Aircraft || !engineInfo.hasGear) return
        if (localPlayer != vehicle.firstPassenger) return

        var componentReady: MutableComponent

        if (vehicle.gearUp) {
            if (vehicle.synchedGearRot == 1f) {
                componentReady = Component.translatable("tips.superbwarfare.gear_retracted").append(
                    Component.literal(" [${ModKeyMappings.MOVE_SPACE.key.displayName.string}]")
                )
            } else {
                componentReady =
                    Component.translatable("tips.superbwarfare.gear_retracting").withStyle(ChatFormatting.RED)

            }
        } else {
            if (vehicle.synchedGearRot == 0f) {
                componentReady = Component.translatable("tips.superbwarfare.gear_extended").append(
                    Component.literal(" [${ModKeyMappings.MOVE_SPACE.key.displayName.string}]")
                )
            } else {
                componentReady =
                    Component.translatable("tips.superbwarfare.gear_extending").withStyle(ChatFormatting.RED)
            }
        }

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            componentReady,
            85,
            (h - 13 - compatHeight),
            -1,
            false
        )
    }

    private fun renderHoverInfo(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        w: Int,
        h: Int,
        partialTick: Float,
        compatHeight: Int
    ) {
        val engineType = vehicle.computed().engineType
        if (engineType != EngineType.HELICOPTER) return
        val engineInfo = vehicle.engineInfo ?: return
        if (engineInfo !is Helicopter) return
        if (localPlayer != vehicle.firstPassenger) return

        var componentReady = Component.translatable("tips.superbwarfare.hover_mode_off").append(
            Component.literal(" [${ModKeyMappings.MOVE_SPACE.key.displayName.string}]")
        )

        if (vehicle.hoverMode) {
            componentReady = Component.translatable("tips.superbwarfare.hover_mode_on").append(
                Component.literal(" [${ModKeyMappings.MOVE_SPACE.key.displayName.string}]")
            )
        }

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            componentReady,
            85,
            (h - 13 - compatHeight),
            -1,
            false
        )
    }

    private fun renderSpeedInfo(
        guiGraphics: GuiGraphics,
        vehicle: VehicleEntity,
        w: Int,
        h: Int,
        partialTick: Float,
        compatHeight: Int
    ) {
        if (localPlayer != vehicle.firstPassenger) return

        val componentReady = Component.literal(
            FormatTool.format0D(
                vehicle.absoluteSpeed * 72,
                " KM/H"
            )
        )

        guiGraphics.drawString(
            Minecraft.getInstance().font,
            componentReady,
            85,
            (h - 22 - compatHeight),
            -1,
            false
        )
    }

    private fun renderWeaponInfo(guiGraphics: GuiGraphics, vehicle: VehicleEntity, w: Int, h: Int) {
        val player = localPlayer

        if (!vehicle.banHand(player)) return
        if (!vehicle.hasWeapon()) return

        val temp: Boolean = wasRenderingWeapons
        wasRenderingWeapons = false

        checkNotNull(player)

        val index = vehicle.getSeatIndex(player)
        if (index == -1) return

        val weapons = vehicle.computed().seats()[index].weapons().map { vehicle.getGunData(it) }
        if (weapons.isEmpty()) return

        val weaponIndex = vehicle.getWeaponIndex(index)
        if (weaponIndex == -1) return

        wasRenderingWeapons = temp

        val currentTime = System.currentTimeMillis()

        // 若上一帧未在渲染武器信息，则初始化动画相关变量
        if (!wasRenderingWeapons) {
            WEAPON_SLOTS_TIMER[weaponIndex].beginForward(currentTime)

            if (oldWeaponIndex != weaponIndex) {
                WEAPON_SLOTS_TIMER[oldWeaponIndex].endBackward(currentTime)

                oldWeaponIndex = weaponIndex
                oldRenderWeaponIndex = weaponIndex
            }

            WEAPON_INDEX_UPDATE_TIMER.beginForward(currentTime)
        }

        // 切换武器时，更新上一个武器槽位和当前武器槽位的动画信息
        if (weaponIndex != oldWeaponIndex) {
            WEAPON_SLOTS_TIMER[weaponIndex].forward(currentTime)
            WEAPON_SLOTS_TIMER[oldWeaponIndex].backward(currentTime)

            oldRenderWeaponIndex = oldWeaponIndex
            oldWeaponIndex = weaponIndex

            WEAPON_INDEX_UPDATE_TIMER.beginForward(currentTime)
        }

        val pose = guiGraphics.pose()

        pose.pushPose()

        var frameIndex = 0

        var i = weapons.size - 1
        while (i in 0..<9) {
            val weapon = weapons[i]

            val frame: ResourceLocation = FRAMES[i]

            pose.pushPose()

            // 相对于最左边的偏移量
            val xOffset: Float
            // 向右偏移的最长长度
            val maxXOffset = 37

            val currentSlotTimer: AnimationTimer = WEAPON_SLOTS_TIMER[i]
            val progress = currentSlotTimer.getProgress(currentTime)

            RenderSystem.setShaderColor(1f, 1f, 1f, Mth.lerp(progress, 0.2f, 1f))
            xOffset = Mth.lerp(progress, maxXOffset.toFloat(), 0f)

            RenderHelper.preciseBlit(
                guiGraphics,
                frame,
                w - 85 + xOffset,
                (h - frameIndex * 18 - 20).toFloat(),
                100f,
                0f,
                0f,
                75f,
                16f,
                75f,
                16f
            )

            val data = vehicle.getGunData(vehicle.getSeatIndex(player), i)
            if (data == null) {
                pose.popPose()
                i--
                continue
            }

            val selected = i == weaponIndex
            // 当前选中武器
            if (selected) {
                val startY = Mth.lerp(
                    progress,
                    (h - (weapons.size - 1 - oldRenderWeaponIndex) * 18 - 16).toFloat(),
                    (h - (weapons.size - 1 - weaponIndex) * 18 - 16).toFloat()
                )

                RenderHelper.preciseBlit(
                    guiGraphics,
                    SELECTED,
                    (w - 95).toFloat(),
                    startY,
                    100f,
                    0f,
                    0f,
                    8f,
                    8f,
                    8f,
                    8f
                )

                var ammoCount = vehicle.getAmmoCount(player)
                val backUpAmmoCount = data.countBackupAmmo(vehicle)

                if (ammoCount == Int.MAX_VALUE) {
                    RenderHelper.preciseBlit(
                        guiGraphics,
                        NUMBER,
                        w - 28 + xOffset,
                        (h - frameIndex * 18 - 15).toFloat(),
                        100f,
                        58f,
                        0f,
                        10f,
                        7.5f,
                        75f,
                        7.5f
                    )
                } else {
                    val percent = data.selectedAmmoConsumer().type == AmmoConsumer.AmmoConsumeType.ENERGY
                    if (percent) {
                        ammoCount /= Math.max(1.0, vehicle.maxEnergy.toDouble() / 100).toInt()
                    }
                    if (!data.useBackpackAmmo()) {
                        var lengthB = 11

                        if (backUpAmmoCount in 10..99) {
                            lengthB = 14
                        }
                        if (backUpAmmoCount >= 100) {
                            lengthB = 16
                        }

                        if (backUpAmmoCount == Int.MAX_VALUE) {
                            RenderHelper.preciseBlit(
                                guiGraphics,
                                NUMBER,
                                w - 22 + xOffset,
                                (h - frameIndex * 18 - 15).toFloat(),
                                100f,
                                58f,
                                0f,
                                10f,
                                7.5f,
                                75f,
                                7.5f
                            )
                            lengthB = 14
                        } else {
                            if (backUpAmmoCount >= 100) {
                                renderNumber(
                                    guiGraphics,
                                    99,
                                    percent,
                                    w - 20 + xOffset,
                                    h - frameIndex * 18 - 12f,
                                    0.125f
                                )
                                RenderHelper.preciseBlit(
                                    guiGraphics,
                                    PLUS,
                                    w - 17.5f + xOffset,
                                    h - frameIndex * 18 - 12f,
                                    0f,
                                    0f,
                                    4f,
                                    4f,
                                    4f,
                                    4f
                                )
                            } else {
                                renderNumber(
                                    guiGraphics,
                                    backUpAmmoCount,
                                    percent,
                                    w - 18 + xOffset,
                                    h - frameIndex * 18 - 12f,
                                    0.125f
                                )
                            }
                        }

                        RenderHelper.preciseBlit(
                            guiGraphics,
                            FRACTION,
                            w - 14 + xOffset - lengthB,
                            h - frameIndex * 18 - 15.5f,
                            0f,
                            0f,
                            8f,
                            8f,
                            8f,
                            8f
                        )

                        renderNumber(
                            guiGraphics,
                            ammoCount,
                            percent,
                            w - 18 + xOffset - lengthB,
                            h - frameIndex * 18 - 15.5f,
                            0.25f
                        )

                    } else {
                        renderNumber(
                            guiGraphics,
                            ammoCount,
                            percent,
                            w - 20 + xOffset,
                            h - frameIndex * 18 - 15.5f,
                            0.25f
                        )
                    }
                }
            }

            RenderHelper.preciseBlit(
                guiGraphics,
                weapon!!.get(GunProp.ICON),
                w - 86 + xOffset,
                (h - frameIndex * 18 - 20).toFloat(),
                100f,
                0f,
                0f,
                75f,
                16f,
                75f,
                16f
            )

            // 这里不知道为什么不能合并，会导致上面那个渲染出错
            val size = data.get(GunProp.AMMO_CONSUMER).size
            if (selected && size > 1) {

                val component = Component.literal("[" + ModKeyMappings.FIRE_MODE.key.displayName.string + "] ")
                    .append(Component.translatable("tips.superbwarfare.switch_ammo"))

                pose.pushPose()
                pose.scale(0.6f, 0.6f, 1.0f)

                var height = -2f

                if (weaponIndex == weapons.size - 1) {
                    height = -27f
                }

                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    component.string,
                    (w - 85 + xOffset) / 0.6f,
                    (h - frameIndex * 18 + height) / 0.6f,
                    0xFFFFFF,
                    false
                )

                pose.popPose()
            }

            if (data.reloading()) {
                val totalReloadTime =
                    if (data.reload.empty()) data.get(GunProp.EMPTY_RELOAD_TIME) else data.get(GunProp.NORMAL_RELOAD_TIME)
                val currentReloadTime = data.reload.reloadTimer.get()

                val reloadProgress = (totalReloadTime - currentReloadTime).toFloat() / totalReloadTime
                val alpha = Mth.lerp(progress, 0.4f, 1f)

                if (currentReloadTime in 1..<totalReloadTime) {
                    RenderHelper.renderCircularRing(
                        guiGraphics,
                        w - 102 + xOffset, (h - frameIndex * 18 - 12).toFloat(),
                        0.014f, 0.010f,
                        floatArrayOf(0f, 0f, 0f, 0.4f * alpha),
                        floatArrayOf(1f, 1f, 1f, alpha),
                        reloadProgress,
                        true
                    )
                }
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
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

            pose.popPose()

            frameIndex++
            i--
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        pose.popPose()

        // 切换武器光标动画播放结束后，更新上次选择槽位
        if (oldWeaponIndex != oldRenderWeaponIndex && WEAPON_INDEX_UPDATE_TIMER.finished(currentTime)) {
            oldRenderWeaponIndex = oldWeaponIndex
        }
        wasRenderingWeapons = true
    }

    private fun renderNumber(
        guiGraphics: GuiGraphics,
        number: Int,
        percent: Boolean,
        x: Float,
        y: Float,
        scale: Float
    ) {
        var number = number
        var pX = x
        if (percent) {
            pX -= 32 * scale
            RenderHelper.preciseBlit(
                guiGraphics, NUMBER, pX + 20 * scale, y, 100f,
                200 * scale, 0f, 32 * scale, 30 * scale, 300 * scale, 30 * scale
            )
        }

        var index = 0
        if (number == 0) {
            RenderHelper.preciseBlit(
                guiGraphics, NUMBER, pX, y, 100f,
                0f, 0f, 20 * scale, 30 * scale, 300 * scale, 30 * scale
            )
        }

        while (number > 0) {
            val digit = number % 10
            RenderHelper.preciseBlit(
                guiGraphics, NUMBER, pX - index * 20 * scale, y, 100f,
                digit * 20 * scale, 0f, 20 * scale, 30 * scale, 300 * scale, 30 * scale
            )
            number /= 10
            index++
        }
    }
}
