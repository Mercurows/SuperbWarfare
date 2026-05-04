package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper
import com.atsuishio.superbwarfare.config.client.ReloadConfig
import com.atsuishio.superbwarfare.data.gun.FireMode
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.SeekType
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.*
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.atsuishio.superbwarfare.tools.*
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.settings.KeyConflictContext
import org.lwjgl.glfw.GLFW
import top.theillusivec4.curios.api.CuriosApi

@EventBusSubscriber(Dist.CLIENT)
object ClickEventHandler {
    @JvmField
    var switchZoom: Boolean = false

    @SubscribeEvent
    fun onButtonReleased(event: InputEvent.MouseButton.Pre) {
        if (notInGame) return
        if (event.action != InputConstants.RELEASE) return

        val player = localPlayer ?: return
        if (player.hasEffect(ModMobEffects.SHOCK)) return

        val button = event.button
        if (button == ModKeyMappings.FIRE.key.value) {
            handleWeaponFireRelease()
        }

        if (button == ModKeyMappings.HOLD_ZOOM.key.value) {
            handleWeaponZoomRelease()
        } else if (button == ModKeyMappings.SWITCH_ZOOM.key.value && !switchZoom) {
            handleWeaponZoomRelease()
        }
    }

    private fun cancelFireKey(player: Player, stack: ItemStack): Boolean {
        val vehicle = player.vehicle
        return stack.item is GunItem || stack.`is`(ModItems.MONITOR.get()) || stack.`is`(ModItems.LUNGE_MINE.get())
                || stack.`is`(ModItems.ARTILLERY_INDICATOR.get()) || player.hasEffect(ModMobEffects.SHOCK)
                || (vehicle is VehicleEntity && vehicle.banHand(player))
    }

    private fun cancelZoomKey(player: Player, stack: ItemStack): Boolean {
        val vehicle = player.vehicle
        return stack.item is GunItem || (vehicle is VehicleEntity && vehicle.banHand(player) && !stack.isEdible)
    }

    @SubscribeEvent
    fun onButtonPressed(event: InputEvent.MouseButton.Pre) {
        if (notInGame) return
        if (event.action != InputConstants.PRESS) return

        val player = localPlayer ?: return
        if (player.isSpectator) return

        if (player.hasEffect(ModMobEffects.SHOCK)) {
            event.isCanceled = true
            return
        }

        val stack = player.mainHandItem
        val button = event.button

        val fireKey = ModKeyMappings.FIRE.key
        if (fireKey.type == InputConstants.Type.MOUSE
            && fireKey.value == button
            && cancelFireKey(player, stack)
        ) {
            event.isCanceled = true
        }

        val zoomKey = ModKeyMappings.HOLD_ZOOM.key
        if (zoomKey.type == InputConstants.Type.MOUSE
            && zoomKey.value == button
            && cancelZoomKey(player, stack)
        ) {
            event.isCanceled = true
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            if (stack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                event.isCanceled = true
            }
            if (stack.`is`(ModItems.MONITOR.get()) && player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                event.isCanceled = true
            }
        }

        if (button == ModKeyMappings.MARK.key.value) {
            if (stack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                sendPacketToServer(SetFiringParametersMessage)
            }
            if (stack.`is`(ModItems.MONITOR.get()) && player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                droneLeftClick(stack, player)
            }
        }

        if (stack.item is GunItem
            || player.vehicle is VehicleEntity
            || stack.`is`(ModItems.MONITOR.get())
            || stack.`is`(ModItems.LUNGE_MINE.get())
            || (stack.`is`(Items.SPYGLASS) && player.isScoping && player.offhandItem.`is`(ModItems.FIRING_PARAMETERS.get()))
            || stack.`is`(ModItems.ARTILLERY_INDICATOR.get())
        ) {
            if (button == ModKeyMappings.FIRE.key.value) {
                handleWeaponFirePress(player, stack)
            }

            if (button == ModKeyMappings.HOLD_ZOOM.key.value) {
                handleWeaponZoomPress(player, stack)
                switchZoom = false
            } else if (button == ModKeyMappings.SWITCH_ZOOM.key.value) {
                handleWeaponZoomPress(player, stack)
                switchZoom = !switchZoom
            }
        }

        val fireModeKey = ModKeyMappings.FIRE_MODE.key
        if (fireModeKey.type == InputConstants.Type.MOUSE && button == fireModeKey.value) {
            val vehicle = player.vehicle
            if (vehicle is VehicleEntity) {
                val data = vehicle.getGunData(player)
                if (data != null && data.get(GunProp.AMMO_CONSUMER).size > 1) {
                    sendPacketToServer(EditMessage(5, add = true, isVehicle = true))
                }
            } else {
                sendPacketToServer(FireModeMessage(false))
            }
            ClientEventHandler.burstFireAmount = 0
        }
    }

    /**
     * 枪械交互时禁止挥舞手臂
     */
    @SubscribeEvent
    fun stopSwing(event: InputEvent.InteractionKeyMappingTriggered) {
        val player = localPlayer ?: return
        if (player.getItemInHand(event.hand).item is GunItem) {
            event.setSwingHand(false)
        }
    }

    @SubscribeEvent
    fun onMouseScrolling(event: InputEvent.MouseScrollingEvent) {
        val player = localPlayer ?: return
        if (notInGame) return
        if (player.hasEffect(ModMobEffects.SHOCK)) {
            return
        }

        val stack = player.mainHandItem
        val scroll = event.scrollDeltaY
        val vehicle = player.vehicle

        // 按下自由视角键时，为载具调整相机距离
        if (vehicle is VehicleEntity && player == vehicle.firstPassenger && ModKeyMappings.FREE_CAMERA.isDown()) {
            ClientMouseHandler.custom3pDistance =
                (ClientMouseHandler.custom3pDistance - event.scrollDeltaY).coerceIn(-3.0, 8.0)
            event.isCanceled = true
            return
        }

        // 未按下shift时，为有武器的载具切换武器
        if (!Screen.hasShiftDown()
            && vehicle is VehicleEntity
            && vehicle.hasWeapon(vehicle.getSeatIndex(player))
            && vehicle.banHand(player)
        ) {
            if (ClientEventHandler.switchVehicleWeaponCooldown <= 0) {
                val index = vehicle.getSeatIndex(player)
                sendPacketToServer(SwitchVehicleWeaponMessage(index, -scroll, true))
                ClientEventHandler.switchVehicleWeaponCooldown = 3
            }
            event.isCanceled = true
        }

        if (stack.item is GunItem && ClientEventHandler.zoom) {
            val data = GunData.from(stack)
            if (data.canSwitchScope()) {
                sendPacketToServer(SwitchScopeMessage(scroll))
            } else if (data.canAdjustZoom() || stack.`is`(ModItems.MINIGUN.get())) {
                sendPacketToServer(AdjustZoomFovMessage(scroll))
            }
            event.isCanceled = true
        }

        val tag = NBTTool.getTag(stack)

        if (stack.`is`(ModItems.MONITOR.get()) && tag.getBoolean("Using")
            && tag.getBoolean("Linked")
        ) {
            ClientEventHandler.droneFov = (ClientEventHandler.droneFov + 0.4 * scroll).coerceIn(1.0, 6.0)
            event.isCanceled = true
        }

        if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
            ClientEventHandler.artilleryIndicatorCustomZoom =
                (ClientEventHandler.artilleryIndicatorCustomZoom + 0.4 * scroll).coerceIn(-2.0, 6.0)
            event.isCanceled = true
        }

        val looking = TraceTool.findLookingEntity(player, 6.0)
        if (looking is MortarEntity && player.isShiftKeyDown) {
            sendPacketToServer(AdjustMortarAngleMessage(scroll))
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onKeyPressed(event: InputEvent.Key) {
        if (notInGame) return

        val player = localPlayer ?: return

        val key = event.key
        if (key < 0) return

        if (player.isSpectator) return
        if (player.hasEffect(ModMobEffects.SHOCK)) return

        val stack = player.mainHandItem
        val vehicle = player.vehicle

        if (event.action == GLFW.GLFW_PRESS) {
            if (key == ModKeyMappings.ACTIVE_THERMAL_IMAGING.key.value) {
                if (vehicle is VehicleEntity) {
                    val index = vehicle.getSeatIndex(player)
                    val seat = vehicle.computed().seats().getOrNull(index) ?: return
                    if (seat.hasThermalImaging) {
                        ClientEventHandler.activeThermalImaging = !ClientEventHandler.activeThermalImaging
                        if (ClientEventHandler.activeThermalImaging) {
                            player.playSound(ModSounds.CANNON_ZOOM_IN.get())
                        } else {
                            player.playSound(ModSounds.CANNON_ZOOM_OUT.get())
                        }
                    }
                    return
                }

                CuriosApi.getCuriosInventory(player).ifPresent {
                    it.findFirstCurio(ModItems.THERMAL_IMAGING_GOGGLES.get()).ifPresent {
                        ClientEventHandler.activeThermalImaging = !ClientEventHandler.activeThermalImaging
                        if (ClientEventHandler.activeThermalImaging) {
                            player.playSound(ModSounds.NIGHT_VISION_ACTIVATE.get())
                        } else {
                            player.playSound(ModSounds.CANNON_ZOOM_OUT.get())
                        }
                    }
                }
            }

            if (key == ModKeyMappings.DISMOUNT.key.value) {
                handleDismountPress(player)
            }

            if (key == Minecraft.getInstance().options.keyJump.key.value) {
                handleDoubleJump(player)
                handleParachute()
            }

            if (key == ModKeyMappings.CONFIG.key.value && ModKeyMappings.CONFIG.keyModifier.isActive(KeyConflictContext.IN_GAME)) {
                handleConfigScreen(player)
            }
            if (key == ModKeyMappings.RELOAD.key.value) {
                ClientEventHandler.burstFireAmount = 0
                ClientEventHandler.isEditing = false
                ClientEventHandler.seekingTime = 0
                ClientEventHandler.lockOn = false
                ClientEventHandler.lockingEntity = null
                ClientEventHandler.seekingEntity = null
                ClientEventHandler.lockingPos = null
                sendPacketToServer(ReloadMessage)
            }
            if (key == ModKeyMappings.FIRE_MODE.key.value || key == ModKeyMappings.CHANGE_FIRE_MODE_BACKWARD.key.value) {
                sendPacketToServer(FireModeMessage(false))
                ClientEventHandler.burstFireAmount = 0
            }
            if (key == ModKeyMappings.CHANGE_FIRE_MODE_FORWARD.key.value) {
                sendPacketToServer(FireModeMessage(true))
                ClientEventHandler.burstFireAmount = 0
            }
            if (key == ModKeyMappings.INTERACT.key.value) {
                if (stack.item is GunItem) {
                    KeyMapping.click(mc.options.keyUse.key)
                } else if (stack.`is`(ModItems.MONITOR.get())) {
                    sendPacketToServer(InteractMessage)
                }
            }

            // 玩家手持枪械时，处理卸弹/切换弹种
            if (stack.item is GunItem) {
                val data = GunData.from(stack)
                if (key == ModKeyMappings.UNLOAD.key.value) {
                    if (data.useBackpackAmmo() || data.ammo.get() + data.virtualAmmo.get() <= 0) return
                    sendPacketToServer(UnloadMessage)
                    ClientEventHandler.burstFireAmount = 0
                }
                if (data.get(GunProp.AMMO_CONSUMER).size > 1) {
                    if (key == ModKeyMappings.CHANGE_AMMO_FORWARD.key.value) {
                        sendPacketToServer(EditMessage(5, add = false, isVehicle = false))
                        ClientEventHandler.burstFireAmount = 0
                    }
                    if (key == ModKeyMappings.CHANGE_AMMO_BACKWARD.key.value) {
                        sendPacketToServer(EditMessage(5, add = true, isVehicle = false))
                        ClientEventHandler.burstFireAmount = 0
                    }
                }
            }

            // 玩家位于载具上时，处理切换弹种
            if (vehicle is VehicleEntity) {
                val data = vehicle.getGunData(player)
                if (data != null && data.get(GunProp.AMMO_CONSUMER).size > 1) {
                    if (key == ModKeyMappings.CHANGE_AMMO_FORWARD.key.value) {
                        sendPacketToServer(EditMessage(5, add = false, isVehicle = true))
                        ClientEventHandler.burstFireAmount = 0
                    }
                    if (key == ModKeyMappings.CHANGE_AMMO_BACKWARD.key.value || key == ModKeyMappings.FIRE_MODE.key.value) {
                        sendPacketToServer(EditMessage(5, add = true, isVehicle = true))
                        ClientEventHandler.burstFireAmount = 0
                    }
                }
            }

            if (key == ModKeyMappings.EDIT_MODE.key.value) {
                val item = stack.item
                if (item is ItemScreenProvider) {
                    val screen = item.getItemScreen(stack, player, InteractionHand.MAIN_HAND)
                    if (screen != null) {
                        Minecraft.getInstance().setScreen(screen)
                        if (screen is WeaponEditScreen) {
                            ClientEventHandler.onOpenEditScreen()
                        }
                        return
                    }
                }

                val offHand = player.offhandItem
                val offHandItem = offHand.item
                if (offHandItem is ItemScreenProvider) {
                    val screen = offHandItem.getItemScreen(offHand, player, InteractionHand.OFF_HAND)
                    if (screen != null) {
                        Minecraft.getInstance().setScreen(screen)
                        return
                    }
                }
            }

            if (key == ModKeyMappings.BREATH.key.value && !ClientEventHandler.exhaustion && ClientEventHandler.zoom) {
                ClientEventHandler.breath = true
            }
            if (key == ModKeyMappings.SENSITIVITY_INCREASE.key.value) {
                sendPacketToServer(SensitivityMessage(true))
            }
            if (key == ModKeyMappings.SENSITIVITY_REDUCE.key.value) {
                sendPacketToServer(SensitivityMessage(false))
            }

            if (stack.item is GunItem
                || (vehicle is VehicleEntity && vehicle.firstPassenger == player)
                || stack.`is`(ModItems.MONITOR.get())
                || (stack.`is`(Items.SPYGLASS) && player.isScoping && player.offhandItem.`is`(ModItems.FIRING_PARAMETERS.get()))
                || (stack.`is`(ModItems.ARTILLERY_INDICATOR.get()))
            ) {
                if (key == ModKeyMappings.FIRE.key.value) {
                    handleWeaponFirePress(player, stack)
                }

                if (key == ModKeyMappings.HOLD_ZOOM.key.value) {
                    handleWeaponZoomPress(player, stack)
                    switchZoom = false
                    return
                }

                if (key == ModKeyMappings.SWITCH_ZOOM.key.value) {
                    handleWeaponZoomPress(player, stack)
                    switchZoom = !switchZoom
                }
            }

            if (key == ModKeyMappings.MARK.key.value) {
                if (stack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                    sendPacketToServer(SetFiringParametersMessage)
                }
                if (stack.`is`(ModItems.MONITOR.get()) && player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                    droneLeftClick(stack, player)
                }
            }
        } else {
            if (key == ModKeyMappings.FIRE.key.value) {
                handleWeaponFireRelease()
            }

            if (key == ModKeyMappings.HOLD_ZOOM.key.value) {
                handleWeaponZoomRelease()
            } else if (key == ModKeyMappings.SWITCH_ZOOM.key.value && !switchZoom) {
                handleWeaponZoomRelease()
            }

            if (event.action == GLFW.GLFW_RELEASE) {
                if (key == ModKeyMappings.BREATH.key.value) {
                    ClientEventHandler.breath = false
                }
            }
        }
    }

    fun handleWeaponFirePress(player: Player, stack: ItemStack) {
        ClientEventHandler.isEditing = false

        if (player.hasEffect(ModMobEffects.SHOCK)) return

        val vehicle = player.vehicle

        if (vehicle is VehicleEntity && vehicle.banHand(player)) {
            if (vehicle.hasWeapon(vehicle.getSeatIndex(player))) {
                ClientEventHandler.holdFireVehicle = true
            }
            return
        }

        if (stack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
            ClientEventHandler.holdingFireKey = true
        }

        if (stack.`is`(Items.SPYGLASS) && player.isScoping && player.offhandItem.`is`(ModItems.FIRING_PARAMETERS.get())) {
            sendPacketToServer(SetFiringParametersMessage)
        }

        if (stack.`is`(ModItems.MONITOR.get())) {
            if (player.offhandItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                ClientEventHandler.holdingFireKey = true
            } else {
                droneLeftClick(stack, player)
            }
        }

        if (stack.`is`(ModItems.LUNGE_MINE.get())) {
            ClientEventHandler.usingLunge = true
        }

        val item = stack.item
        if (item is GunItem
            && ClientEventHandler.clientTimer.progress == 0L
            && !notInGame
        ) {
            val data = GunData.from(stack)
            val resource = GunResource.compute(stack)

            // TODO 整合特殊处理
            if (!(stack.`is`(ModItems.BOCEK.get()))) {
                if (!data.meleeOnly()) {
                    // 普通枪（？）
                    if (stack.`is`(ModItems.QL_1031.get()) && data.selectedFireModeInfo().name == "Hold"
                        && item.canShoot(data, player)
                    ) {
                        player.playSound(ModSounds.QL_1031_CHARGE.get(), 1f, 1f)
                        ClientEventHandler.shouldPlayDischargeSound = true
                    }

                    val triggerSound = resource.triggerSound
                    if (triggerSound != null && !data.meleeOnly()) {
                        player.playSound(triggerSound, 1f, 1f)
                    }
                }
            } else {
                // 波塞克特殊处理
                ClientEventHandler.bowPower = 0.0
                ClientEventHandler.holdingFireKey = true
                player.isSprinting = false
                if (data.hasEnoughAmmoToShoot(player)) {
                    return
                }
            }

            if (!data.useBackpackAmmo() && !data.meleeOnly() && !data.hasEnoughAmmoToShoot(player) && data.reload.time() == 0) {
                if (ReloadConfig.LEFT_CLICK_RELOAD.get()) {
                    sendPacketToServer(ReloadMessage)
                    ClientEventHandler.burstFireAmount = 0
                    ClientEventHandler.seekingTime = 0
                    ClientEventHandler.lockOn = false
                    ClientEventHandler.lockingEntity = null
                    ClientEventHandler.seekingEntity = null
                    ClientEventHandler.lockingPos = null
                }
            } else {
                sendPacketToServer(FireKeyMessage(0, ClientEventHandler.bowPower, ClientEventHandler.zoom))
                if (ClientEventHandler.drawTime < 0.01) {
                    val fireMode = data.selectedFireModeInfo().mode
                    if (fireMode == FireMode.BURST) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            ClientEventHandler.noSprintTicks = 8f
                            player.isSprinting = false
                            ClientEventHandler.burstFireAmount = data.get(GunProp.BURST_AMOUNT)
                        }
                    } else if (fireMode == FireMode.SEMI) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            ClientEventHandler.noSprintTicks = 3f
                            player.isSprinting = false
                            ClientEventHandler.burstFireAmount = 1
                        }
                    }

                    ClientEventHandler.holdingFireKey = true
                    player.isSprinting = false
                }
            }
        }
    }

    fun handleWeaponFireRelease() {
        sendPacketToServer(FireKeyMessage(1, ClientEventHandler.bowPower, ClientEventHandler.zoom))
        ClientEventHandler.bowPull = false
        ClientEventHandler.holdingFireKey = false
        ClientEventHandler.holdFireVehicle = false
        ClientEventHandler.isEditing = false
        ClientEventHandler.customRpm = 0

        val player = localPlayer ?: return
        if (player.isSpectator) return

        val stack = player.mainHandItem

        if (stack.`is`(ModItems.BOCEK.get())) {
            sendPacketToServer(ReloadMessage)
        }

        if (stack.item is GunItem) {
            val data = GunData.from(stack)
            val fireMode = data.selectedFireModeInfo().mode

            if (fireMode != FireMode.BURST) {
                ClientEventHandler.burstFireAmount = 0
            }

            if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_FIRE) {
                ClientEventHandler.stopWeaponSeekSound(Minecraft.getInstance().player)
            }
        }
    }

    fun handleWeaponZoomPress(player: Player, stack: ItemStack) {
        sendPacketToServer(ZoomMessage(0))

        ClientEventHandler.isEditing = false

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player)) && vehicle.banHand(player)) {
            ClientEventHandler.zoomVehicle = true
            return
        }

        if (stack.item !is GunItem) return
        if (!GunResource.compute(stack).canZoom) return

        val data = GunData.from(stack)
        ClientEventHandler.zoom = true

        val level = data.perk.getLevel(ModPerks.INTELLIGENT_CHIP)
        if (level > 0) {
            if (ClientEventHandler.lockedEntity == null) {
                ClientEventHandler.lockedEntity =
                    if (data.perk.has(ModPerks.PHASE_PENETRATING_BULLET.get()) || data.perk.has(ModPerks.BEAST_BULLET.get())) {
                        SeekTool.seekEntityThroughWall(player, 32 + 8 * (level - 1).toDouble(), 20.0)
                    } else {
                        SeekTool.seekLivingEntity(player, 32 + 8 * (level - 1).toDouble(), 20.0)
                    }
            }
        }
    }

    fun handleWeaponZoomRelease() {
        sendPacketToServer(ZoomMessage(1))
        ClientEventHandler.zoom = false
        ClientEventHandler.zoomVehicle = false
        ClientEventHandler.lockedEntity = null
        ClientEventHandler.stopWeaponSeekSound(Minecraft.getInstance().player)
        ClientEventHandler.breath = false
    }

    fun handleDoubleJump(player: Player) {
        val level = player.level()
        val x = player.x
        val y = player.y
        val z = player.z

        if (!level.isLoaded(player.blockPosition())) {
            return
        }

        if (ClientEventHandler.canDoubleJump) {
            player.deltaMovement = Vec3(player.lookAngle.x, 0.8, player.lookAngle.z)
            level.playLocalSound(x, y, z, ModSounds.DOUBLE_JUMP.get(), SoundSource.BLOCKS, 1f, 1f, false)
            sendPacketToServer(DoubleJumpMessage)
            ClientEventHandler.canDoubleJump = false
        }
    }

    fun handleParachute() {
        sendPacketToServer(ParachuteMessage)
    }

    fun handleConfigScreen(player: Player) {
        if (ModList.get().isLoaded(CompatHolder.CLOTH_CONFIG)) {
            CompatHolder.hasMod(
                CompatHolder.CLOTH_CONFIG
            ) { mc.setScreen(ClothConfigHelper.getConfigScreen(null)) }
        } else {
            player.displayClientMessage(
                Component.translatable("tips.superbwarfare.no_cloth_config").withStyle(ChatFormatting.RED), true
            )
        }
    }

    private fun handleDismountPress(player: Player) {
        val vehicle = player.vehicle as? VehicleEntity ?: return

        if ((!vehicle.onGround() || vehicle.deltaMovement.length() >= 0.1) && ClientEventHandler.dismountCountdown <= 0) {
            if (vehicle.allowEjection(vehicle.getSeatIndex(player))) {
                player.displayClientMessage(
                    Component.translatable(
                        "tips.superbwarfare.mount.onboard",
                        ModKeyMappings.DISMOUNT.translatedKeyMessage
                    ), true
                )
            } else {
                player.displayClientMessage(
                    Component.translatable(
                        "mount.onboard",
                        ModKeyMappings.DISMOUNT.translatedKeyMessage
                    ), true
                )
            }

            ClientEventHandler.dismountCountdown = 20
            return
        }
        sendPacketToServer(PlayerStopRidingMessage(false))
        ClientEventHandler.stopVehicleReloadSound(player)
    }

    fun droneLeftClick(stack: ItemStack, player: Player) {
        if (stack.`is`(ModItems.MONITOR.get()) && stack.getOrCreateTag().getBoolean("Using")
            && stack.getOrCreateTag().getBoolean("Linked")
        ) {
            val drone =
                EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString("LinkedDrone")) ?: return
            val lookingEntity = SeekTool.seekLivingEntity(drone, 512.0, 2 / ClientEventHandler.droneFovLerp)

            val result = player.level().clip(
                ClipContext(
                    drone.eyePosition,
                    drone.eyePosition.add(drone.lookAngle.scale(512.0)),
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    drone
                )
            )

            var pos = result.location
            if (lookingEntity != null && !player.isShiftKeyDown) {
                pos = lookingEntity.position()
            }

            sendPacketToServer(DroneFireMessage(pos.toVector3f()))
        }
    }
}