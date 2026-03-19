package com.atsuishio.superbwarfare.item.misc

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.network.message.receive.ResetCameraTypeMessage
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.FormatTool.format1D
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.atsuishio.superbwarfare.tools.sendPacket
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.ChatFormatting
import net.minecraft.client.CameraType
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

open class MonitorItem : Item(Properties().stacksTo(1)) {
    private fun resetDroneData(drone: DroneEntity?) {
        if (drone == null) return

        drone.getPersistentData().putBoolean("left", false)
        drone.getPersistentData().putBoolean("right", false)
        drone.getPersistentData().putBoolean("forward", false)
        drone.getPersistentData().putBoolean("backward", false)
        drone.getPersistentData().putBoolean("up", false)
        drone.getPersistentData().putBoolean("down", false)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = player.mainHandItem

        if (stack.tag == null || !stack.tag!!.getBoolean(LINKED)) {
            return super.use(level, player, hand)
        }

        if (stack.getOrCreateTag().getBoolean(USING)) {
            stack.getOrCreateTag().putBoolean(USING, false)
            if (level.isClientSide) {
                if (ClientEventHandler.lastCameraType != null) {
                    mc.options.cameraType = ClientEventHandler.lastCameraType!!
                }
            }
        } else {
            stack.getOrCreateTag().putBoolean(USING, true)
            if (level.isClientSide) {
                ClientEventHandler.lastCameraType = mc.options.cameraType
                mc.options.cameraType = CameraType.THIRD_PERSON_BACK
            }
        }

        val drone = EntityFindUtil.findDrone(player.level(), stack.getOrCreateTag().getString(LINKED_DRONE))
        this.resetDroneData(drone)

        return super.use(level, player, hand)
    }

    override fun getAttributeModifiers(slot: EquipmentSlot, stack: ItemStack): Multimap<Attribute, AttributeModifier> {
        if (slot == EquipmentSlot.MAINHAND) {
            val builder = ImmutableMultimap.builder<Attribute, AttributeModifier>()
            builder.putAll(super.getAttributeModifiers(slot, stack))
            builder.put(
                Attributes.ATTACK_DAMAGE,
                AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Item modifier", 2.0, AttributeModifier.Operation.ADDITION)
            )
            builder.put(
                Attributes.ATTACK_SPEED,
                AttributeModifier(BASE_ATTACK_SPEED_UUID, "Item modifier", -2.4, AttributeModifier.Operation.ADDITION)
            )
            return builder.build()
        }

        return super.getAttributeModifiers(slot, stack)
    }

    @OnlyIn(Dist.CLIENT)
    override fun appendHoverText(stack: ItemStack, world: Level?, list: MutableList<Component>, flag: TooltipFlag) {
        if (!stack.getOrCreateTag().contains(LINKED_DRONE) ||
            stack.getOrCreateTag().getString(LINKED_DRONE) == "none"
        ) return

        val player = localPlayer ?: return

        if (!stack.getOrCreateTag().contains("PosX")
            || !stack.getOrCreateTag().contains("PosY")
            || !stack.getOrCreateTag().contains("PosZ")
        ) return

        val droneVec = Vec3(
            stack.getOrCreateTag().getDouble("PosX"),
            stack.getOrCreateTag().getDouble("PosY"),
            stack.getOrCreateTag().getDouble("PosZ")
        )

        list.add(
            Component.translatable(
                "des.superbwarfare.monitor",
                format1D(player.position().distanceTo(droneVec), "m")
            ).withStyle(ChatFormatting.GRAY)
        )
        list.add(
            Component.literal("X: ${format1D(droneVec.x)} Y: ${format1D(droneVec.y)} Z: ${format1D(droneVec.z)}")
        )
    }

    override fun shouldCauseReequipAnimation(
        oldStack: ItemStack?,
        newStack: ItemStack?,
        slotChanged: Boolean
    ): Boolean {
        return false
    }

    override fun inventoryTick(itemstack: ItemStack, world: Level, entity: Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(itemstack, world, entity, slot, selected)
        val drone = EntityFindUtil.findDrone(entity.level(), itemstack.getOrCreateTag().getString(LINKED_DRONE))

        if (!selected) {
            if (itemstack.getOrCreateTag().getBoolean(USING)) {
                itemstack.getOrCreateTag().putBoolean(USING, false)
                if (entity.level().isClientSide) {
                    if (ClientEventHandler.lastCameraType != null) {
                        mc.options.cameraType = ClientEventHandler.lastCameraType!!
                    }
                }
            }
            this.resetDroneData(drone)
        } else if (drone == null) {
            if (itemstack.getOrCreateTag().getBoolean(USING)) {
                itemstack.getOrCreateTag().putBoolean(USING, false)
                if (entity.level().isClientSide) {
                    if (ClientEventHandler.lastCameraType != null) {
                        mc.options.cameraType = ClientEventHandler.lastCameraType!!
                    }
                }
            }
        }
    }

    companion object {
        const val LINKED: String = "Linked"
        const val LINKED_DRONE: String = "LinkedDrone"
        const val USING: String = "Using"

        @JvmStatic
        fun link(itemstack: ItemStack, id: String) {
            itemstack.getOrCreateTag().putBoolean(LINKED, true)
            itemstack.getOrCreateTag().putString(LINKED_DRONE, id)
        }

        @JvmStatic
        fun disLink(itemstack: ItemStack, player: Player?) {
            itemstack.getOrCreateTag().putBoolean(LINKED, false)
            itemstack.getOrCreateTag().putString(LINKED_DRONE, "none")
            if (player is ServerPlayer) {
                player.sendPacket(ResetCameraTypeMessage)
            }
        }

        @JvmStatic
        fun getDronePos(itemstack: ItemStack, vec3: Vec3) {
            itemstack.getOrCreateTag().putDouble("PosX", vec3.x)
            itemstack.getOrCreateTag().putDouble("PosY", vec3.y)
            itemstack.getOrCreateTag().putDouble("PosZ", vec3.z)
        }
    }
}
