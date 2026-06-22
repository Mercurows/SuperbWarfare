package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock
import com.atsuishio.superbwarfare.block.property.BlockPart
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.init.ModDamageTypes.causeVehicleStrikeDamage
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.inventory.menu.VehicleAssemblingMenu
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.HasCustomInventoryScreen
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.network.NetworkHooks
import kotlin.math.max
import kotlin.math.min

open class VehicleAssemblingTableVehicleEntity(type: EntityType<*>, level: Level) : VehicleEntity(type, level),
    HasCustomInventoryScreen, MenuProvider {
    var deltaXo: Float = 0f
    var deltaYo: Float = 0f
    var deltaX: Float = 0f
    var deltaY: Float = 0f
    var jumpCooldown: Int = 0

    constructor(level: Level) : this(ModEntities.VEHICLE_ASSEMBLING_TABLE.get(), level)

    // 变回方块
    override fun onCrowbarInteract(
        stack: ItemStack,
        player: Player,
        hand: InteractionHand
    ): InteractionResult? {
        if (!player.isShiftKeyDown) {
            if (!this.level().isClientSide && this.getPassengers().isEmpty()) {
                val facing = direction
                val currentPos = this.position()
                val targetPos = when (facing) {
                    Direction.WEST -> currentPos.add(-0.5, 0.0, -0.5)
                    Direction.EAST -> currentPos.add(0.5, 0.0, 0.5)
                    Direction.NORTH -> currentPos.add(0.5, 0.0, -0.5)
                    Direction.SOUTH -> currentPos.add(-0.5, 0.0, 0.5)
                    else -> currentPos
                }
                val targetBlockPos = BlockPos.containing(targetPos)

                var canPlace = true
                for (part in BlockPart.entries) {
                    val blockPos = part.relative(targetBlockPos, facing)
                    val blockState = this.level().getBlockState(blockPos)
                    if (!blockState.canBeReplaced()) {
                        canPlace = false
                        break
                    }
                }

                if (canPlace) {
                    for (part in BlockPart.entries) {
                        val blockPos = part.relative(targetBlockPos, facing)
                        val state = ModBlocks.VEHICLE_ASSEMBLING_TABLE.get().defaultBlockState()
                            .setValue(VehicleAssemblingTableBlock.FACING, facing)
                            .setValue(VehicleAssemblingTableBlock.BLOCK_PART, part)

                        this.level().setBlock(blockPos, state, 3)
                    }

                    this.discard()
                    return InteractionResult.SUCCESS
                } else {
                    player.displayClientMessage(
                        Component.translatable("tips.superbwarfare.vehicle_assembling_table.warn")
                            .withStyle(ChatFormatting.RED), true
                    )
                    return InteractionResult.FAIL
                }
            }
            return InteractionResult.PASS
        }
        return super.onCrowbarInteract(stack, player, hand)
    }

    override fun baseTick() {
        deltaXo = deltaX
        deltaYo = deltaY
        super.baseTick()

        if (jumpCooldown > 0) {
            jumpCooldown--
        }

        deltaX = mouseMoveSpeedY
        if (this.leftInputDown && this.rightInputDown) {
            deltaX = 0f
        } else if (this.leftInputDown) {
            deltaX = -1f
        } else if (this.rightInputDown) {
            deltaX = 1f
        }

        val f = if (onGround()) 0.85f else 0.9f
        this.setDeltaMovement(this.deltaMovement.multiply(f.toDouble(), f.toDouble(), f.toDouble()))

        if (this.isInWater && this.tickCount % 4 == 0) {
            this.setDeltaMovement(this.deltaMovement.multiply(0.6, 0.6, 0.6))
            if (lastTickSpeed > 0.4) {
                this.hurt(
                    causeVehicleStrikeDamage(
                        this.level().registryAccess(),
                        this,
                        if (this.getFirstPassenger() == null) this else this.getFirstPassenger()
                    ), (20 * ((lastTickSpeed - 0.4) * (lastTickSpeed - 0.4))).toFloat()
                )
            }
        }
    }

    override fun travel() {
        val passenger = this.getFirstPassenger()

        power *= 0.95f
        if (passenger == null || isInWater) {
            leftInputDown = false
            rightInputDown = false
            forwardInputDown = false
            backInputDown = false
            this.deltaMovement = this.deltaMovement.multiply(0.96, 1.0, 0.96)
        } else if (passenger is Player) {
            if (forwardInputDown) {
                power = min(power + 0.1f, 1f)
            }

            deltaRot *= 0.8f

            if (backInputDown) {
                power = max(power - (if (power > 0) 0.1f else 0.01f), if (onGround()) -0.2f else 0.2f)
                if (rightInputDown) {
                    deltaRot += 0.4f
                } else if (leftInputDown) {
                    deltaRot -= 0.4f
                }
            } else {
                if (rightInputDown) {
                    deltaRot -= 0.4f
                } else if (this.leftInputDown) {
                    deltaRot += 0.4f
                }
            }

            // Shift刹车
            if (downInputDown) {
                power = 0f
            }

            // 跳
            if (upInputDown && onGround() && jumpCooldown == 0) {
                jumpCooldown = 40
                val level = this.level()
                if (level is ServerLevel) {
                    level.playSound(
                        null,
                        this.onPos,
                        ModSounds.WHEEL_CHAIR_JUMP.get(),
                        SoundSource.PLAYERS,
                        2f,
                        1f
                    )
                }
                val movement = this.forward
                    .multiply(1.0, 0.0, 1.0)
                    .normalize()
                    .scale(0.7)
                this.deltaMovement = this.deltaMovement.add(movement.x, 1.0, movement.z)
            }

            val diffY = Mth.wrapDegrees(passenger.getYHeadRot() - this.yRot).coerceIn(-90f, 90f)
            val diffX = Mth.wrapDegrees(passenger.xRot - this.xRot).coerceIn(-60f, 60f)

            val addX = (min(max(deltaMovement.length() - 0.1, 0.01).toFloat(), 0.9f) * diffX).coerceIn(-4f, 4f)
            val addZ = deltaRot - (if (this.onGround()) 0f else 0.01f) * diffY * deltaMovement.length().toFloat()

            val yRotSync = (-(50 * this.deltaMovement.length()).coerceIn(2.0, 4.0) * deltaRot).toFloat()

            this.yRot += yRotSync
            this.xRot = (this.xRot + addX).coerceIn(
                (if (onGround()) -12 else -120).toFloat(),
                (if (onGround()) 3 else 120).toFloat()
            )
            this.setZRot(this.roll - 0.2f * addZ)
        }

        val powerValue = 0.05 * power
        this.setDeltaMovement(
            this.deltaMovement.add(
                forward
                    .multiply(1.0, 0.0, 1.0)
                    .normalize()
                    .multiply(powerValue, powerValue, powerValue)
            )
        )
    }

    override fun destroy() {
        super.destroy()
        if (level() is ServerLevel) {
            val item = ItemEntity(
                level(),
                this.x,
                this.y,
                this.z,
                ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get())
            )
            item.setPickUpDelay(50)
            this.level().addFreshEntity(item)
        }
        discard()
    }

    override fun getRetrieveItems(): MutableList<ItemStack> {
        return mutableListOf(ItemStack(ModItems.VEHICLE_ASSEMBLING_TABLE.get()))
    }

    override fun openCustomInventoryScreen(player: Player) {
        player.openMenu(this)
    }

    override fun openMenu(player: Player) {
        if (player is ServerPlayer && player.vehicle is VehicleAssemblingTableVehicleEntity) {
            NetworkHooks.openScreen(player, SimpleMenuProvider(this, Component.empty()))
        }
    }

    override fun createMenu(pContainerId: Int, pPlayerInventory: Inventory, pPlayer: Player): AbstractContainerMenu? {
        return VehicleAssemblingMenu(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL, true)
    }
}
