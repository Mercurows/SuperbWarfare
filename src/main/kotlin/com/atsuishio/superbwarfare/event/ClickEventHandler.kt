package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.client.gun.GunChargeHandler
import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler
import com.atsuishio.superbwarfare.client.overlay.ammo.AmmoBarHud
import com.atsuishio.superbwarfare.client.screens.LoiterConfigScreen
import com.atsuishio.superbwarfare.client.screens.MissilePosInputScreen
import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen
import com.atsuishio.superbwarfare.client.screens.WeaponEditScreen
import com.atsuishio.superbwarfare.compat.CompatHolder
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper
import com.atsuishio.superbwarfare.config.client.ReloadConfig
import com.atsuishio.superbwarfare.config.server.MapConfig
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.*
import com.atsuishio.superbwarfare.item.ItemScreenProvider
import com.atsuishio.superbwarfare.item.curio.TacticalTerminalItem
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
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import org.lwjgl.glfw.GLFW
import top.theillusivec4.curios.api.CuriosApi

@Mod.EventBusSubscriber(
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = [Dist.CLIENT]
)
object ClickEventHandler {
    @JvmField
    var switchZoom: Boolean = false

    @SubscribeEvent
    fun onButtonReleased(event: InputEvent.MouseButton.Pre) {
        if (notInGame) return
        if (event.action != InputConstants.RELEASE) return

        val player = localPlayer ?: return
        if (player.hasEffect(ModMobEffects.SHOCK.get())) return

        val button = event.button
        if (button == ModKeyMappings.FIRE.key.value) {
            handleWeaponFireRelease()
        }

        if (button == ModKeyMappings.HOLD_ZOOM.key.value) {
            handleWeaponUseRelease()
        } else if (button == ModKeyMappings.SWITCH_ZOOM.key.value && !switchZoom) {
            handleWeaponUseRelease()
        }
    }

    fun cancelFireKey(player: Player, stack: ItemStack): Boolean {
        val vehicle = player.vehicle
        // 手持副武器时按普通物品处理
        return GunItem.isHeldWeapon(stack) || stack.`is`(ModItems.MONITOR.get())
                || stack.`is`(ModItems.ARTILLERY_INDICATOR.get()) || player.hasEffect(ModMobEffects.SHOCK.get())
                || (vehicle is VehicleEntity && vehicle.banHand(player))
    }

    fun cancelZoomKey(player: Player, stack: ItemStack): Boolean {
        val vehicle = player.vehicle
        // 手持副武器时按普通物品处理
        return GunItem.isHeldWeapon(stack) ||
                (vehicle is VehicleEntity && vehicle.banHand(player) && !stack.item.isEdible)
    }

    @SubscribeEvent
    fun onButtonPressed(event: InputEvent.MouseButton.Pre) {
        if (notInGame) return
        if (event.action != InputConstants.PRESS) return

        val player = localPlayer ?: return
        if (player.isSpectator) return

        if (player.hasEffect(ModMobEffects.SHOCK.get())) {
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
        }

        // 手持副武器时按普通物品处理
        if (GunItem.isHeldWeapon(stack)
            || player.vehicle is VehicleEntity
            || stack.`is`(ModItems.MONITOR.get())
            || (stack.`is`(Items.SPYGLASS) && player.isScoping && player.offhandItem.`is`(ModItems.FIRING_PARAMETERS.get()))
            || stack.`is`(ModItems.ARTILLERY_INDICATOR.get())
        ) {
            if (button == ModKeyMappings.FIRE.key.value) {
                handleWeaponFirePress(player, stack)
            }

            if (button == ModKeyMappings.HOLD_ZOOM.key.value) {
                handleWeaponUsePress(player, stack)
                switchZoom = false
            } else if (button == ModKeyMappings.SWITCH_ZOOM.key.value) {
                handleWeaponUsePress(player, stack)
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
        // 手持副武器时按普通物品处理
        if (GunItem.isHeldWeapon(player.getItemInHand(event.hand))) {
            event.setSwingHand(false)
        }
    }

    @SubscribeEvent
    fun onMouseScrolling(event: InputEvent.MouseScrollingEvent) {
        if (handleScroll(event.scrollDelta)) event.isCanceled = true
    }

    fun handleScroll(scroll: Double, shiftDown: Boolean = Screen.hasShiftDown()): Boolean {
        val player = localPlayer ?: return false
        if (notInGame || player.hasEffect(ModMobEffects.SHOCK.get())) return false

        var handled = false
        val stack = player.mainHandItem
        val vehicle = player.vehicle

        // 按下自由视角键时，为载具调整相机距离
        if (vehicle is VehicleEntity && ModKeyMappings.FREE_CAMERA.isDown) {
            ClientMouseHandler.custom3pDistance =
                (ClientMouseHandler.custom3pDistance - scroll).coerceIn(-3.0, 20.0)
            return true
        }

        // 未按下shift时，为有武器的载具切换武器
        if (!shiftDown && switchVehicleWeapon(player, scroll)) {
            handled = true
        }

        // ⚠ 瞄准镜切换**只对主武器生效**（四期）：副武器不具有配件
        // （`SubWeaponItem.canEditAttachments` = false，`GunData.availableAttachments` 对副武器返回空表），
        // 所以这里必须看**物理上的主手**，不能看 `ActiveGun` ——
        // 否则副武器被 G 切出来时，滚轮会去切一把根本没有瞄具槽的枪（服务端也会照着那把枪去找瞄具）。
        if (GunItem.isHeldWeapon(stack) && ClientEventHandler.zoom) {
            val data = GunData.from(stack)
            if (data.canSwitchScope()) {
                sendPacketToServer(SwitchScopeMessage(scroll))
            } else if (data.canAdjustZoom()) {
                sendPacketToServer(AdjustZoomFovMessage(scroll))
            }
            handled = true
        }

        if (player.isUsingItem && player.useItem.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
            ClientEventHandler.artilleryIndicatorCustomZoom =
                (ClientEventHandler.artilleryIndicatorCustomZoom + 0.4 * scroll).coerceIn(-2.0, 6.0)
            handled = true
        }

        val looking = OverlayTraceHandler.playerReachEntity
        if (looking is MortarEntity && player.isShiftKeyDown) {
            sendPacketToServer(AdjustMortarAngleMessage(scroll))
            handled = true
        }
        return handled
    }

    fun switchVehicleWeapon(player: Player, scroll: Double): Boolean {
        val vehicle = player.vehicle
        if (vehicle !is VehicleEntity) return false
        val index = vehicle.getSeatIndex(player)
        if (!vehicle.hasWeapon(index) || !vehicle.banHand(player)) return false

        if (ClientEventHandler.switchVehicleWeaponCooldown <= 0) {
            sendPacketToServer(SwitchVehicleWeaponMessage(index, -scroll, true))
            ClientEventHandler.switchVehicleWeaponCooldown = 3
        }
        return true
    }

    @SubscribeEvent
    fun onKeyPressed(event: InputEvent.Key) {
        if (event.key < 0) return
        handleKeyInput(event.action) { mapping ->
            event.key == mapping.key.value &&
                    (mapping != ModKeyMappings.CONFIG || mapping.keyModifier.isActive(KeyConflictContext.IN_GAME))
        }
    }

    fun handleKeyInput(mapping: KeyMapping, action: Int) {
        handleKeyInput(action) { it == mapping }
    }

    private fun handleKeyInput(action: Int, matchesKey: (KeyMapping) -> Boolean) {
        if (notInGame) return
        val player = localPlayer ?: return
        if (player.isSpectator || player.hasEffect(ModMobEffects.SHOCK.get())) return

        val stack = player.mainHandItem
        val vehicle = player.vehicle

        if (action == GLFW.GLFW_PRESS) {
            if (matchesKey(ModKeyMappings.ACTIVE_THERMAL_IMAGING)) {
                if (vehicle is VehicleEntity) {
                    val index = vehicle.getSeatIndex(player)
                    val seat = vehicle.computed().seats().getOrNull(index)
                    if (seat != null && seat.hasThermalImaging) {
                        ClientEventHandler.activeThermalImaging = !ClientEventHandler.activeThermalImaging
                        if (ClientEventHandler.activeThermalImaging) {
                            player.playSound(ModSounds.CANNON_ZOOM_IN.get())
                        } else {
                            player.playSound(ModSounds.CANNON_ZOOM_OUT.get())
                        }
                        return
                    }
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

            if (matchesKey(ModKeyMappings.DISMOUNT)) {
                handleDismountPress(player)
            }

            if (matchesKey(ModKeyMappings.TOGGLE_TACTICAL_MAP) && MapConfig.ENABLE_TACTICAL_MAP.get()) {
                if (TacticalTerminalItem.isTerminalEquipped(player) && mc.screen == null) {
                    mc.setScreen(TacticalMapScreen())
                }
            }

            if (matchesKey(Minecraft.getInstance().options.keyJump)) {
                handleDoubleJump(player)
                handleParachute()
            }

            if (matchesKey(ModKeyMappings.CONFIG)) {
                handleConfigScreen(player)
            }
            if (matchesKey(ModKeyMappings.RELOAD)) {
                ClientEventHandler.burstFireAmount = 0
                ClientEventHandler.isEditing = false
                ClientEventHandler.seekingTime = 0
                ClientEventHandler.lockOn = false
                ClientEventHandler.lockingEntity = null
                ClientEventHandler.seekingEntity = null
                ClientEventHandler.lockingPos = null
                sendPacketToServer(ReloadMessage)
            }
            if (matchesKey(ModKeyMappings.FIRE_MODE) || matchesKey(ModKeyMappings.CHANGE_FIRE_MODE_BACKWARD)) {
                sendPacketToServer(FireModeMessage(false))
                ClientEventHandler.burstFireAmount = 0
            }
            if (matchesKey(ModKeyMappings.CHANGE_FIRE_MODE_FORWARD)) {
                sendPacketToServer(FireModeMessage(true))
                ClientEventHandler.burstFireAmount = 0
            }
            // 部署状态下按跳跃/潜行键直接解除部署，效果等同再按一次交互键
            if ((matchesKey(mc.options.keyJump) || matchesKey(mc.options.keyShift))
                && DeployedWeaponHandler.isDeployed(player)
            ) {
                sendPacketToServer(DeployWeaponMessage)
            }
            if (matchesKey(ModKeyMappings.INTERACT)) {
                // 手持副武器时按普通物品处理
                if (GunItem.isHeldWeapon(stack)) {
                    if (DeployedWeaponHandler.isDeployed(player) ||
                        DeployedWeaponHandler.deployTarget(player, OverlayTraceHandler.playerViewBlockResult) != null
                    ) {
                        sendPacketToServer(DeployWeaponMessage)
                    } else {
                        KeyMapping.click(mc.options.keyUse.key)
                    }
                }
            }

            // 玩家手持枪械时，处理卸弹/切换弹种
            if (GunItem.isHeldWeapon(stack)) {
                val data = GunData.from(stack)
                if (matchesKey(ModKeyMappings.UNLOAD)) {
                    if (data.useBackpackAmmo() || data.ammo.get() + data.virtualAmmo.get() <= 0) return
                    sendPacketToServer(UnloadMessage)
                    ClientEventHandler.burstFireAmount = 0
                }
                if (data.get(GunProp.AMMO_CONSUMER).size > 1) {
                    if (matchesKey(ModKeyMappings.CHANGE_AMMO_FORWARD)) {
                        AmmoBarHud.noteAmmoSwitch(data, add = false)
                        sendPacketToServer(EditMessage(5, add = false, isVehicle = false))
                        ClientEventHandler.burstFireAmount = 0
                    }
                    if (matchesKey(ModKeyMappings.CHANGE_AMMO_BACKWARD)) {
                        AmmoBarHud.noteAmmoSwitch(data, add = true)
                        sendPacketToServer(EditMessage(5, add = true, isVehicle = false))
                        ClientEventHandler.burstFireAmount = 0
                    }
                }
            }

            // 玩家位于载具上时，处理切换弹种
            if (vehicle is VehicleEntity) {
                val data = vehicle.getGunData(player)
                if (data != null && data.get(GunProp.AMMO_CONSUMER).size > 1) {
                    if (matchesKey(ModKeyMappings.CHANGE_AMMO_FORWARD)) {
                        sendPacketToServer(EditMessage(5, add = false, isVehicle = true))
                        ClientEventHandler.burstFireAmount = 0
                    }
                    if (matchesKey(ModKeyMappings.CHANGE_AMMO_BACKWARD) || matchesKey(ModKeyMappings.FIRE_MODE)) {
                        sendPacketToServer(EditMessage(5, add = true, isVehicle = true))
                        ClientEventHandler.burstFireAmount = 0
                    }
                }

                if (matchesKey(ModKeyMappings.LOITER_CONFIG)) {
                    if (vehicle.computed().engineType == EngineType.AIRCRAFT && mc.screen == null) {
                        mc.setScreen(LoiterConfigScreen(vehicle))
                    }
                }
            }

            if (matchesKey(ModKeyMappings.EDIT_MODE)) {
                if (DeployedWeaponHandler.isDeployed(player)) return

                if (vehicle is VehicleEntity) {
                    val data = vehicle.getGunData(player)
                    if (data != null) {
                        val input = data.get(GunProp.SEEK_WEAPON_INFO)?.inputBlockPos
                        if (input == true) {
                            Minecraft.getInstance().setScreen(MissilePosInputScreen())
                            return
                        }
                    }
                }

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

            if (matchesKey(ModKeyMappings.BREATH) && !ClientEventHandler.exhaustion && ClientEventHandler.zoom) {
                ClientEventHandler.breath = true
            }
            if (matchesKey(ModKeyMappings.SENSITIVITY_INCREASE)) {
                sendPacketToServer(SensitivityMessage(true))
            }
            if (matchesKey(ModKeyMappings.SENSITIVITY_REDUCE)) {
                sendPacketToServer(SensitivityMessage(false))
            }

            // 手持副武器时按普通物品处理
            if (GunItem.isHeldWeapon(stack)
                || (vehicle is VehicleEntity && vehicle.firstPassenger == player)
                || stack.`is`(ModItems.MONITOR.get())
                || (stack.`is`(Items.SPYGLASS) && player.isScoping && player.offhandItem.`is`(ModItems.FIRING_PARAMETERS.get()))
                || (stack.`is`(ModItems.ARTILLERY_INDICATOR.get()))
            ) {
                if (matchesKey(ModKeyMappings.FIRE)) {
                    handleWeaponFirePress(player, stack)
                }

                if (matchesKey(ModKeyMappings.HOLD_ZOOM)) {
                    handleWeaponUsePress(player, stack)
                    switchZoom = false
                    return
                }

                if (matchesKey(ModKeyMappings.SWITCH_ZOOM)) {
                    handleWeaponUsePress(player, stack)
                    switchZoom = !switchZoom
                }
            }

            if (matchesKey(ModKeyMappings.MARK)) {
                if (stack.`is`(ModItems.ARTILLERY_INDICATOR.get())) {
                    sendPacketToServer(SetFiringParametersMessage)
                }
            }
        } else {
            if (matchesKey(ModKeyMappings.FIRE)) {
                handleWeaponFireRelease()
            }

            if (matchesKey(ModKeyMappings.HOLD_ZOOM)) {
                handleWeaponUseRelease()
            } else if (matchesKey(ModKeyMappings.SWITCH_ZOOM) && !switchZoom) {
                handleWeaponUseRelease()
            }

            if (action == GLFW.GLFW_RELEASE) {
                if (matchesKey(ModKeyMappings.BREATH)) {
                    ClientEventHandler.breath = false
                }
            }
        }
    }

    fun handleWeaponFirePress(player: Player, stack: ItemStack) {
        ClientEventHandler.isEditing = false

        if (player.hasEffect(ModMobEffects.SHOCK.get())) return

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

        // 当前操控的枪
        val gunStack = ActiveGun.stackOf(player)
        val item = gunStack.item as? GunItem ?: return
        if (!GunItem.isOperable(gunStack)) return

        // 冲锋窗口里不许开火（近战专属枪械的左键是挥击，不算开火）
        if (GunChargeHandler.isCharging() && !GunData.from(gunStack).meleeOnly()) return

        if (ClientEventHandler.clientTimer.progress == 0L
            && !notInGame
        ) {
            val data = GunData.from(gunStack)
            val resource = GunResource.compute(gunStack)
            val fireModeInfo = data.selectedFireModeInfo()
            val chargeConfig = fireModeInfo.chargeConfig()

            if (!data.meleeOnly()) {
                if (chargeConfig != null) {
                    // 空弹或装箭期间也记住按住左键；就绪后自动开始蓄力，松手仍由 release 清除。
                    ClientEventHandler.chargeActive = item.canShoot(data, player)
                    ClientEventHandler.chargeProgress = 0.0
                    ClientEventHandler.chargePower = 0.0
                    ClientEventHandler.holdingFireKeyTicks = 0
                    ClientEventHandler.holdingFireKeyTicks0 = 0f
                    ClientEventHandler.shouldPlayDischargeSound = true
                    if (ClientEventHandler.drawTime < 0.01) {
                        ClientEventHandler.holdingFireKey = true
                        player.setSprinting(false)
                    }

                    val chargeSound = resource.chargeSound
                    if (chargeSound != null && ClientEventHandler.chargeActive) {
                        player.playSound(chargeSound, 1f, 1f)
                    }
                }

                val triggerSound = resource.triggerSound
                if (triggerSound != null) {
                    player.playSound(triggerSound, 1f, 1f)
                }
            }

            // 近战专属枪械（`"Projectile": "@melee"`）：左键**直通近战输入**，
            // 不走进开火/换弹分支。按住左键的连续挥击由 `handleGunMelee` 的
            // `meleeOnly() && holdingFireKey` 分支负责。
            if (data.meleeOnly()) {
                ClientEventHandler.holdingFireKey = true
                player.setSprinting(false)
                return
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
                sendPacketToServer(FireKeyMessage(0, ClientEventHandler.chargePower, ClientEventHandler.zoom))
                if (ClientEventHandler.drawTime < 0.01) {
                    val fireMode = data.selectedFireModeInfo().mode
                    if (fireMode == FireMode.BURST) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            ClientEventHandler.noSprintTicks = 8f
                            player.setSprinting(false)
                            ClientEventHandler.burstFireAmount = data.get(GunProp.BURST_AMOUNT)
                        }
                    } else if (fireMode == FireMode.SEMI) {
                        if (ClientEventHandler.burstFireAmount == 0) {
                            ClientEventHandler.noSprintTicks = 3f
                            player.setSprinting(false)
                            ClientEventHandler.burstFireAmount = 1
                        }
                    }

                    ClientEventHandler.holdingFireKey = true
                    player.setSprinting(false)
                }
            }
        }
    }

    fun cancelWeaponFire() {
        ClientEventHandler.holdingFireKey = false
        ClientEventHandler.holdFireVehicle = false
        ClientEventHandler.holdingFireKeyTicks = 0
        ClientEventHandler.holdingFireKeyTicks0 = 0f
        ClientEventHandler.chargeActive = false
        ClientEventHandler.chargeProgress = 0.0
        ClientEventHandler.chargePower = 0.0
        ClientEventHandler.burstFireAmount = 0
        handleWeaponFireRelease()
    }

    fun handleWeaponFireRelease() {
        val player = localPlayer ?: return
        if (player.isSpectator) return

        val stack = player.mainHandItem
        val data = if (GunItem.isHeldWeapon(stack)) GunData.from(stack) else null
        val fireModeInfo = data?.selectedFireModeInfo()
        val chargeConfig = fireModeInfo?.chargeConfig()
        val tickProgress = if (chargeConfig != null) {
            (ClientEventHandler.holdingFireKeyTicks.toDouble() / chargeConfig.effectiveDuration).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val releaseProgress = if (chargeConfig != null) {
            maxOf(ClientEventHandler.chargeProgress, tickProgress)
        } else {
            0.0
        }
        val chargePower = chargeConfig?.powerForProgress(releaseProgress) ?: ClientEventHandler.chargePower

        if (mc.connection != null) {
            sendPacketToServer(FireKeyMessage(1, chargePower, ClientEventHandler.zoom))
        }
        ClientEventHandler.holdingFireKey = false
        ClientEventHandler.chargeActive = false
        ClientEventHandler.chargeProgress = 0.0
        ClientEventHandler.chargePower = 0.0

        if (!ClientEventHandler.hasBarrelSpin(stack)) {
            ClientEventHandler.holdingFireKeyTicks = 0
            ClientEventHandler.holdingFireKeyTicks0 = 0f
        }
        ClientEventHandler.holdFireVehicle = false
        ClientEventHandler.isEditing = false
        ClientEventHandler.customRpm = 0

        if (data != null && fireModeInfo != null) {
            val fireMode = data.selectedFireModeInfo().mode

            if (fireMode != FireMode.BURST) {
                ClientEventHandler.burstFireAmount = 0
            }

            if (chargeConfig != null) {
                val releaseAllowed = when (chargeConfig.trigger) {
                    ChargeTrigger.ON_RELEASE -> releaseProgress > 0.0
                    ChargeTrigger.ON_FULL_RELEASE -> releaseProgress >= 1.0
                    ChargeTrigger.AUTO_AT_FULL -> false
                }
                if (releaseAllowed && chargePower >= chargeConfig.effectiveMinPower) {
                    ClientEventHandler.shootClient(player, chargePower)
                }
            }

            if (data.get(GunProp.SEEK_TYPE) == SeekType.HOLD_FIRE) {
                ClientEventHandler.stopWeaponSeekSound(Minecraft.getInstance().player)
            }
        }
    }

    fun handleWeaponUsePress(player: Player, stack: ItemStack) {
        ClientEventHandler.isEditing = false

        val vehicle = player.vehicle
        if (vehicle is VehicleEntity && vehicle.hasWeapon(vehicle.getSeatIndex(player)) && vehicle.banHand(player)) {
            val data = vehicle.getGunData(player)
            if (data != null) {
                val input = data.get(GunProp.SEEK_WEAPON_INFO)?.inputBlockPos
                if (input == true) {
                    Minecraft.getInstance().setScreen(MissilePosInputScreen())
                    return
                }
            }
            sendPacketToServer(ZoomMessage(0))
            ClientEventHandler.zoomVehicle = true
            return
        }

        // 当前操控的枪：`CanZoom` / `ZOOM_TIME` 全部读正在操作那把的数据
        val gunStack = ActiveGun.stackOf(player)
        if (!GunItem.isOperable(gunStack)) return

        val item = gunStack.item as? GunItem
        val data = GunData.from(gunStack)
        if (item != null && item.onUseActionPress(player, gunStack, data)) return

        when (data.get(GunProp.RIGHT_CLICK_ACTION)) {
            RightClickAction.HEAVY_STRIKE -> ClientEventHandler.requestHeavyStrike()
            RightClickAction.BAYONET_CHARGE -> GunChargeHandler.start(gunStack, data)
            else -> handleWeaponAimPress(player, gunStack, data)
        }
    }

    private fun handleWeaponAimPress(player: Player, gunStack: ItemStack, data: GunData) {
        // 冲锋窗口里不许开镜
        if (GunChargeHandler.isCharging()) return
        if (!GunResource.compute(gunStack).canZoom) return

        sendPacketToServer(ZoomMessage(0))
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

    fun handleWeaponUseRelease() {
        val player = Minecraft.getInstance().player
        if (!ClientEventHandler.zoom && !ClientEventHandler.zoomVehicle) return

        if (player != null) {
            val gunStack = ActiveGun.stackOf(player)
            val item = gunStack.item as? GunItem
            if (item != null && item.onUseActionRelease(player, gunStack, GunData.from(gunStack))) return
        }

        if (mc.connection != null) {
            sendPacketToServer(ZoomMessage(1))
        }
        ClientEventHandler.zoom = false
        ClientEventHandler.zoomVehicle = false
        ClientEventHandler.lockedEntity = null
        ClientEventHandler.stopWeaponSeekSound(player)
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
}
