package com.atsuishio.superbwarfare.compat.controlify

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.EngineType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClickEventHandler
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModKeyMappings
import com.atsuishio.superbwarfare.init.ModMobEffects
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.network.message.send.ChangeVehicleSeatMessage
import com.atsuishio.superbwarfare.network.message.send.VehicleDisconnectTowingMessage
import com.atsuishio.superbwarfare.network.message.send.VehicleUnloadPassengersMessage
import com.atsuishio.superbwarfare.tools.*
import dev.isxander.controlify.api.ControlifyApi
import dev.isxander.controlify.api.bind.RadialIcon
import dev.isxander.controlify.api.bind.ControlifyBindApi
import dev.isxander.controlify.api.bind.InputBindingSupplier
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint
import dev.isxander.controlify.api.entrypoint.InitContext
import dev.isxander.controlify.api.entrypoint.PreInitContext
import dev.isxander.controlify.api.event.ControlifyEvents
import dev.isxander.controlify.api.ingameinput.LookInputModifier
import dev.isxander.controlify.bindings.BindContext
import dev.isxander.controlify.bindings.ControlifyBindings
import dev.isxander.controlify.bindings.RadialIcons
import dev.isxander.controlify.controller.ControllerEntity
import dev.isxander.controlify.gui.screen.RadialMenuScreen
import net.minecraft.client.CameraType
import net.minecraft.client.KeyMapping
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec2
import net.minecraftforge.client.event.InputEvent
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import org.lwjgl.glfw.GLFW
import kotlin.math.abs

class ControlifyCompat : ControlifyEntrypoint {
    private val bindings = linkedMapOf<KeyMapping, InputBindingSupplier>()
    private val scrollBindings = linkedMapOf<Double, InputBindingSupplier>()
    private lateinit var vehicleReload: InputBindingSupplier
    private lateinit var seekOrFreeCamera: InputBindingSupplier
    private var controller: ControllerEntity? = null
    private var toggledZoom = false

    // Actions selected from the radial menu:
    // Executed on the next tick after the menu closes;
    // held down for a specified number of ticks
    private var pendingAction: (() -> Unit)? = null
    private val pulses = mutableMapOf<KeyMapping, Int>()

    override fun onControlifyPreInit(context: PreInitContext) {
        val api = ControlifyBindApi.get()
        val vehicleContext = BindContext(ResourceLocation(Mod.MODID, "vehicle")) {
            !notInGame && localPlayer?.vehicle is VehicleEntity
        }

        val gunContext = BindContext(ResourceLocation(Mod.MODID, "gun")) {
            !notInGame && localPlayer?.vehicle !is VehicleEntity &&
                    localPlayer?.let { GunItem.isHeldWeapon(it.mainHandItem) } == true
        }

        api.registerBindContext(vehicleContext)
        api.registerBindContext(gunContext)

        val vehicleKeys = setOf(
            ModKeyMappings.MOVE_FORWARD, ModKeyMappings.MOVE_BACKWARD,
            ModKeyMappings.MOVE_LEFT, ModKeyMappings.MOVE_RIGHT,
            ModKeyMappings.MOVE_SPACE, ModKeyMappings.MOVE_SHIFT, ModKeyMappings.MOVE_CTRL,
            ModKeyMappings.DISMOUNT, ModKeyMappings.CHANGE_SEAT, ModKeyMappings.FREE_CAMERA,
            ModKeyMappings.RELEASE_DECOY, ModKeyMappings.VEHICLE_SEEK,
            ModKeyMappings.UNLOAD_PASSENGERS, ModKeyMappings.DISCONNECT_TOWING, ModKeyMappings.LOITER_CONFIG
        )

        for (key in ModKeyMappings.keyMappings) {
            val bindContext = when (key) {
                in vehicleKeys -> vehicleContext
                ModKeyMappings.RELOAD -> gunContext
                else -> BindContext.IN_GAME
            }
            bindings[key] = api.registerBinding { builder ->
                builder.id("superbwarfare", key.name.removePrefix("key.superbwarfare."))
                    .name(Component.translatable(key.name))
                    .category(Component.translatable(ModKeyMappings.CATEGORY))
                    .allowedContexts(bindContext)
                    .addKeyCorrelation(key)
                    .radialCandidate(RadialIcons.EMPTY)
            }
        }

        for ((name, scroll) in listOf("next_weapon" to -1.0, "previous_weapon" to 1.0)) {
            scrollBindings[scroll] = api.registerBinding { builder ->
                builder.id(Mod.MODID, name)
                    .category(Component.translatable(ModKeyMappings.CATEGORY))
                    .allowedContexts(BindContext.IN_GAME)
                    .radialCandidate(RadialIcons.EMPTY)
            }
        }

        vehicleReload = api.registerBinding { builder ->
            builder.id(Mod.MODID, "vehicle_reload")
                .category(Component.translatable(ModKeyMappings.CATEGORY))
                .allowedContexts(vehicleContext)
                .radialCandidate(RadialIcons.EMPTY)
        }

        seekOrFreeCamera = api.registerBinding { builder ->
            builder.id(Mod.MODID, "seek_or_free_camera")
                .category(Component.translatable(ModKeyMappings.CATEGORY))
                .allowedContexts(vehicleContext)
                .radialCandidate(RadialIcons.EMPTY)
        }

        ControlifyEvents.ACTIVE_CONTROLLER_TICKED.register { updateInput(it.controller()) }
        ControlifyEvents.LOOK_INPUT_MODIFIER.register(::modifyLook)
        ControlifyEvents.CONTROLLER_DISCONNECTED.register {
            if (it.controller() === controller) resetInput()
        }

        ControlifyEvents.INPUT_MODE_CHANGED.register {
            if (!it.mode().isController) resetInput()
        }

        // When opening the Controlify radial menu while in a vehicle, replace it with the vehicle action menu
        MinecraftForge.EVENT_BUS.addListener<ScreenEvent.Opening> {
            val player = localPlayer ?: return@addListener
            val vehicle = player.vehicle as? VehicleEntity ?: return@addListener
            val current = ControlifyApi.get().currentController.orElse(null) ?: return@addListener
            val radial = ControlifyBindings.RADIAL_MENU.on(current)
            if (it.newScreen is RadialMenuScreen && radial.digitalNow()) {
                it.newScreen = RadialMenuScreen(
                    current, radial, vehicleRadialItems(player, vehicle),
                    Component.translatable("controlify.radial.superbwarfare.vehicle"), null, null
                )
            }
        }

        MinecraftForge.EVENT_BUS.addListener<TickEvent.ClientTickEvent> {
            if (it.phase == TickEvent.Phase.START && !canInput()) resetInput()
        }

        MinecraftForge.EVENT_BUS.addListener<InputEvent.InteractionKeyMappingTriggered> {
            val player = localPlayer
            val current = ControlifyApi.get().currentController.orElse(null)
            if (!notInGame && player != null && !player.isSpectator &&
                ControlifyApi.get().currentInputMode().isController && current != null &&
                ((it.isAttack && ControlifyBindings.ATTACK.on(current).digitalNow() &&
                        ClickEventHandler.cancelFireKey(player, player.mainHandItem)) ||
                        (it.isUseItem && ControlifyBindings.USE.on(current).digitalNow() &&
                                ClickEventHandler.cancelZoomKey(player, player.mainHandItem)))
            ) {
                it.isCanceled = true
                it.setSwingHand(false)
            }
        }
    }

    override fun onControlifyInit(context: InitContext) {}

    override fun onControllersDiscovered(controlify: ControlifyApi) {}

    private fun canInput(): Boolean {
        val player = localPlayer ?: return false
        return !notInGame && !player.isSpectator && !player.hasEffect(ModMobEffects.SHOCK.get()) &&
                ControlifyApi.get().currentInputMode().isController &&
                ControlifyApi.get().currentController.isPresent
    }

    private fun updateInput(current: ControllerEntity) {
        if (!canInput()) {
            resetInput()
            return
        }

        if (controller !== current) resetInput()
        controller = current

        val player = localPlayer ?: return
        val vehicle = player.vehicle
        pendingAction?.invoke()
        pendingAction = null

        // One button: lock-on for weapons that can seek, free camera otherwise
        val seekWeapon = vehicle is VehicleEntity && vehicle.getGunData(player)?.get(GunProp.SEEK_WEAPON_INFO) != null
        val sharedKey = if (seekWeapon) ModKeyMappings.VEHICLE_SEEK else ModKeyMappings.FREE_CAMERA
        val sharedDown = seekOrFreeCamera.on(current).digitalNow()

        for ((key, supplier) in bindings) {
            val down = supplier.on(current).digitalNow() || key in pulses || (sharedDown && key == sharedKey)
            if (!ModKeyMappings.updateControllerKey(key, down)) continue
            if (key == ModKeyMappings.CHANGE_SEAT) {
                if (down) changeSeat()
            } else if (down && vehicle is VehicleEntity && vehicle.firstPassenger != player &&
                (key == ModKeyMappings.FIRE || key == ModKeyMappings.HOLD_ZOOM || key == ModKeyMappings.SWITCH_ZOOM)
            ) {
                if (key == ModKeyMappings.FIRE) {
                    ClickEventHandler.handleWeaponFirePress(player, player.mainHandItem)
                } else {
                    ClickEventHandler.handleWeaponUsePress(player, player.mainHandItem)
                    ClickEventHandler.switchZoom =
                        key == ModKeyMappings.SWITCH_ZOOM && !ClickEventHandler.switchZoom
                }
            } else {
                ClickEventHandler.handleKeyInput(key, if (down) GLFW.GLFW_PRESS else GLFW.GLFW_RELEASE)
            }

            if (key == ModKeyMappings.SWITCH_ZOOM) toggledZoom = ClickEventHandler.switchZoom
        }

        pulses.replaceAll { _, ticks -> ticks - 1 }
        pulses.values.removeIf { it <= 0 }

        for ((scroll, supplier) in scrollBindings) {
            if (supplier.on(current).justPressed()) ClickEventHandler.handleScroll(scroll, shiftDown = false)
        }

        if (vehicleReload.on(current).justPressed()) {
            ClickEventHandler.handleKeyInput(ModKeyMappings.RELOAD, GLFW.GLFW_PRESS)
        }

        if (ControlifyBindings.JUMP.on(current).justPressed()) {
            ClickEventHandler.handleKeyInput(mc.options.keyJump, GLFW.GLFW_PRESS)
        }

        if (ControlifyBindings.SNEAK.on(current).justPressed()) {
            ClickEventHandler.handleKeyInput(mc.options.keyShift, GLFW.GLFW_PRESS)
        }

        if (!canInput()) resetInput()
    }

    private fun resetInput() {
        ClientMouseHandler.controllerMovement = Vec2.ZERO
        pulses.clear()
        for (key in bindings.keys) {
            if (!ModKeyMappings.updateControllerKey(key, false)) continue
            when (key) {
                ModKeyMappings.FIRE -> ClickEventHandler.cancelWeaponFire()
                ModKeyMappings.HOLD_ZOOM -> ClickEventHandler.handleWeaponUseRelease()
                ModKeyMappings.BREATH -> ClientEventHandler.breath = false
            }
        }

        if (toggledZoom) {
            ClickEventHandler.switchZoom = false
            ClickEventHandler.handleWeaponUseRelease()
            toggledZoom = false
        }
        controller = null
    }

    private fun changeSeat() {
        val player = localPlayer ?: return
        val vehicle = player.vehicle as? VehicleEntity ?: return
        val current = vehicle.getSeatIndex(player)

        for (offset in 1 until vehicle.maxPassengers) {
            val index = (current + offset) % vehicle.maxPassengers
            if (vehicle.getNthEntity(index) != null) continue

            sendPacketToServer(ChangeVehicleSeatMessage(index))
            vehicle.changeSeat(player, index)
            return
        }
    }

    private fun vehicleRadialItems(player: LocalPlayer, vehicle: VehicleEntity): Array<RadialMenuScreen.RadialItem> {
        val seat = vehicle.getSeatIndex(player)
        val driver = vehicle.firstPassenger == player
        val items = mutableListOf(radialItem(ModKeyMappings.DISMOUNT.name, Items.OAK_DOOR) {
            pulses[ModKeyMappings.DISMOUNT] = 1
        })

        if (vehicle.allowEjection(seat)) {
            items += radialItem("controlify.radial.superbwarfare.eject", ModItems.PARACHUTE.get()) {
                pulses[ModKeyMappings.DISMOUNT] = 12
            }
        }

        if (vehicle.hasWeapon(seat)) {
            items += radialItem("controlify.binding.superbwarfare.vehicle_reload", Items.ARROW) {
                pulses[ModKeyMappings.RELOAD] = 1
            }
        }

        if ((vehicle.getGunData(player)?.get(GunProp.AMMO_CONSUMER)?.size ?: 0) > 1) {
            items += radialItem(ModKeyMappings.CHANGE_AMMO_FORWARD.name, Items.SPECTRAL_ARROW) {
                pulses[ModKeyMappings.CHANGE_AMMO_FORWARD] = 1
            }
        }

        if (vehicle.hasDecoy()) {
            items += radialItem(ModKeyMappings.RELEASE_DECOY.name, Items.FIREWORK_ROCKET) {
                pulses[ModKeyMappings.RELEASE_DECOY] = 2
            }
        }

        if (vehicle.computed().seats().getOrNull(seat)?.hasThermalImaging == true) {
            items += radialItem(ModKeyMappings.ACTIVE_THERMAL_IMAGING.name, ModItems.THERMAL_IMAGING_GOGGLES.get()) {
                pulses[ModKeyMappings.ACTIVE_THERMAL_IMAGING] = 1
            }
        }

        if (driver && vehicle.passengers.size > 1) {
            items += radialItem(ModKeyMappings.UNLOAD_PASSENGERS.name, Items.SADDLE) {
                sendPacketToServer(VehicleUnloadPassengersMessage(true))
            }
        }

        if (driver) {
            items += radialItem(ModKeyMappings.DISCONNECT_TOWING.name, Items.LEAD) {
                sendPacketToServer(VehicleDisconnectTowingMessage)
            }
        }

        if (driver && vehicle.computed().engineType == EngineType.AIRCRAFT) {
            items += radialItem(ModKeyMappings.LOITER_CONFIG.name, Items.COMPASS) {
                pulses[ModKeyMappings.LOITER_CONFIG] = 1
            }
        }
        return items.toTypedArray()
    }

    private fun radialItem(name: String, item: Item, action: () -> Unit) = object : RadialMenuScreen.RadialItem {
        private val stack = ItemStack(item)
        override fun name(): Component = Component.translatable(name)
        override fun icon() = RadialIcon { graphics, x, y, _ -> graphics.renderFakeItem(stack, x, y) }
        override fun playAction(): Boolean {
            // When the menu is open, `notInGame` is true, so key press handling is skipped, and execution is delayed.
            pendingAction = action
            return true
        }
    }

    private fun modifyLook(event: LookInputModifier) {
        if (!canInput()) {
            ClientMouseHandler.controllerMovement = Vec2.ZERO
            val player = localPlayer
            if (player != null && !player.isSpectator && player.hasEffect(ModMobEffects.SHOCK.get())) {
                event.lookInput().zero()
            }
            return
        }

        val look = event.lookInput()
        // Entity.turn uses 0.15 degrees per mouse delta; aircraft controls consume those deltas.
        ClientMouseHandler.controllerMovement = Vec2(look.x / 0.15f, look.y / 0.15f)
        look.mul(ClientMouseHandler.changeSensitivity(1.0).toFloat())
        look.y *= ClientMouseHandler.invertY()

        val vehicle = localPlayer?.vehicle as? VehicleEntity ?: return
        if (mc.options.cameraType != CameraType.FIRST_PERSON) return

        val roll = vehicle.roll
        val weight = abs(roll) / 90f
        val direction = if (roll < 0) 1f else -1f

        val bank = direction * (if (abs(roll) > 90) 1 - (abs(roll) - 90) / 90 else 1f)
        val x = look.x

        look.x = (1 - weight) * x + weight * look.y * bank
        look.y = (1 - weight) * look.y - weight * x * direction
    }
}
