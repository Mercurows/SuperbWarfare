package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.RenderHelper
import com.atsuishio.superbwarfare.client.screens.DogTagEditorScreen
import com.atsuishio.superbwarfare.client.tooltip.ClientDogTagImageTooltip
import com.atsuishio.superbwarfare.compat.tacz.TACZGunEventHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.config.client.KillMessageConfig
import com.atsuishio.superbwarfare.config.client.KillMessageConfig.KillMessagePosition
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.KillMessageHandler
import com.atsuishio.superbwarfare.init.ModDamageTypes
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.curio.DogTagItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.tools.DamageTypeTool
import com.atsuishio.superbwarfare.tools.LivingKillRecord
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.OwnableEntity
import net.minecraft.world.entity.player.Player
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import top.theillusivec4.curios.api.CuriosApi
import kotlin.math.pow

@OnlyIn(Dist.CLIENT)
object KillMessageOverlay : CommonOverlay("kill_message") {
    private val HEADSHOT = loc("textures/overlay/damage_types/headshot.png")

    private val KNIFE = loc("textures/overlay/damage_types/knife.png")
    private val EXPLOSION = loc("textures/overlay/damage_types/explosion.png")
    private val CLAYMORE = loc("textures/overlay/damage_types/claymore.png")
    private val GENERIC = loc("textures/overlay/damage_types/generic.png")
    private val BEAST = loc("textures/overlay/damage_types/beast.png")
    private val SHOCK = loc("textures/overlay/damage_types/shock.png")
    private val BURN = loc("textures/overlay/damage_types/burn.png")
    private val DRONE = loc("textures/overlay/damage_types/drone.png")
    private val LASER = loc("textures/overlay/damage_types/laser.png")
    private val VEHICLE = loc("textures/overlay/damage_types/vehicle_strike.png")

    override fun shouldRender() = super.shouldRender()
            && KillMessageConfig.SHOW_KILL_MESSAGE.get()
            && !KillMessageHandler.QUEUE.isEmpty()

    override fun RenderContext.render() {
        val pos = KillMessageConfig.KILL_MESSAGE_POSITION.get()
        var posX: Int
        var posY: Float
        var left = false
        var bottom = false

        when (pos) {
            KillMessagePosition.LEFT_TOP -> {
                posX = KillMessageConfig.KILL_MESSAGE_MARGIN_X.get()
                posY = KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get().toFloat()
                left = true
            }

            KillMessagePosition.RIGHT_TOP -> {
                posX = screenWidth - KillMessageConfig.KILL_MESSAGE_MARGIN_X.get()
                posY = KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get().toFloat()
            }

            KillMessagePosition.LEFT_BOTTOM -> {
                posX = KillMessageConfig.KILL_MESSAGE_MARGIN_X.get()
                posY = (screenHeight - KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get() - 10).toFloat()
                left = true
                bottom = true
            }

            KillMessagePosition.RIGHT_BOTTOM -> {
                posX = screenWidth - KillMessageConfig.KILL_MESSAGE_MARGIN_X.get()
                posY = (screenHeight - KillMessageConfig.KILL_MESSAGE_MARGIN_Y.get() - 10).toFloat()
                bottom = true
            }
        }

        val arr = KillMessageHandler.QUEUE.toTypedArray<LivingKillRecord?>()
        val record: LivingKillRecord = arr[0]!!

        if (record.freeze) {
            for (playerKillRecord in arr) {
                playerKillRecord!!.freeze = false
            }
        }

        if (record.tick >= 80) {
            if (arr.size > 1 && record.tick - arr[1]!!.tick < (if (record.fastRemove) 2 else 20)) {
                arr[1]!!.fastRemove = true
                record.fastRemove = true
                for (j in 1..<arr.size) {
                    arr[j]!!.freeze = true
                }
            }
        }

        for (r in KillMessageHandler.QUEUE) {
            posY = renderKillMessages(r, guiGraphics, partialTick, posX, posY, left, bottom)
        }
    }

    private fun renderKillMessages(
        record: LivingKillRecord,
        guiGraphics: GuiGraphics,
        partialTick: Float,
        width: Int,
        baseTop: Float,
        left: Boolean,
        bottom: Boolean
    ): Float {
        var baseTop = baseTop
        val top = baseTop

        val font = Minecraft.getInstance().font

        val target = record.target
        val targetName: String = getTargetName(target)
        val targetNameWidth = font.width(targetName)

        guiGraphics.pose().pushPose()

        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE
        )

        // 入场效果
        if (record.tick < 3) {
            guiGraphics.pose().translate((3 - record.tick - partialTick) * 33 * (if (left) -1 else 1), 0f, 0f)
        }

        // 4s后开始消失
        if (record.tick >= 80) {
            val animationTickCount = if (record.fastRemove) 2 else 20
            val rate = ((record.tick + partialTick - 80) / animationTickCount).toDouble().pow(5.0).toFloat()
            guiGraphics.pose().translate(rate * 100 * (if (left) -1 else 1), 0f, 0f)
            guiGraphics.setColor(1f, 1f, 1f, 1 - rate)
            baseTop += 10 * (1 - rate) * (if (bottom) -1 else 1)
        } else {
            baseTop += (10 * (if (bottom) -1 else 1)).toFloat()
        }

        // 击杀提示默认是右对齐的，这里从右向左渲染
        if (!left) {
            var currentPosX = width - targetNameWidth - 10f

            // 渲染被击杀者名称
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                targetName,
                currentPosX,
                top,
                target.getTeamColor(),
                false
            )

            // 渲染狗牌图标
            if (target is LivingEntity && shouldRenderDogTagIcon(target)) {
                currentPosX -= 14f
                renderDogTagIcon(guiGraphics, target, currentPosX, top - 0.5f)
            }

            // 渲染伤害类型图标
            val damageTypeIcon: ResourceLocation? = getDamageTypeIcon(record)
            if (damageTypeIcon != null) {
                currentPosX -= 18f
                RenderHelper.preciseBlit(
                    guiGraphics,
                    damageTypeIcon,
                    currentPosX,
                    top - 2,
                    0f,
                    0f,
                    12f,
                    12f,
                    12f,
                    12f
                )
            }

            // 渲染武器图标
            val currentWeaponIcon: ResourceLocation? = getWeaponIcon(record)
            if (currentWeaponIcon != null) {
                currentPosX -= 36f
                RenderHelper.preciseBlit(
                    guiGraphics,
                    currentWeaponIcon,
                    currentPosX,
                    top,
                    0f,
                    0f,
                    32f,
                    8f,
                    -32f,
                    8f
                )
            }

            // 渲染击杀者名称
            val attackerName: String = getEntityName(record.attacker)
            currentPosX -= (font.width(attackerName) + 6).toFloat()

            guiGraphics.drawString(
                Minecraft.getInstance().font,
                attackerName,
                currentPosX,
                top,
                record.attacker.getTeamColor(),
                false
            )

            // 渲染狗牌图标
            if (shouldRenderDogTagIcon(record.attacker)) {
                currentPosX -= 14f
                renderDogTagIcon(guiGraphics, record.attacker, currentPosX, top - 0.5f)
            }
        } else {
            var currentPosX = width + 10f

            // 渲染狗牌图标
            if (shouldRenderDogTagIcon(record.attacker)) {
                renderDogTagIcon(guiGraphics, record.attacker, currentPosX, top - 0.5f)
                currentPosX += 14f
            }

            // 渲染击杀者名称
            val attackerName: String = getEntityName(record.attacker)
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                attackerName,
                currentPosX,
                top,
                record.attacker.getTeamColor(),
                false
            )

            currentPosX += (font.width(attackerName) + 6).toFloat()

            // 渲染武器图标
            val currentWeaponIcon: ResourceLocation? = getWeaponIcon(record)
            if (currentWeaponIcon != null) {
                RenderHelper.preciseBlit(
                    guiGraphics,
                    currentWeaponIcon,
                    currentPosX,
                    top,
                    0f,
                    0f,
                    32f,
                    8f,
                    -32f,
                    8f
                )
                currentPosX += 36f
            }

            // 渲染伤害类型图标
            val damageTypeIcon: ResourceLocation? = getDamageTypeIcon(record)
            if (damageTypeIcon != null) {
                RenderHelper.preciseBlit(
                    guiGraphics,
                    damageTypeIcon,
                    currentPosX,
                    top - 2,
                    0f,
                    0f,
                    12f,
                    12f,
                    12f,
                    12f
                )
                currentPosX += 18f
            }

            // 渲染狗牌图标
            if (target is LivingEntity && shouldRenderDogTagIcon(target)) {
                renderDogTagIcon(guiGraphics, target, currentPosX, top - 0.5f)
                currentPosX += 14f
            }

            // 渲染被击杀者名称
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                targetName,
                currentPosX,
                top,
                target.getTeamColor(),
                false
            )
        }

        RenderSystem.defaultBlendFunc()
        RenderSystem.disableBlend()
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()

        guiGraphics.setColor(1f, 1f, 1f, 1f)
        guiGraphics.pose().popPose()

        return baseTop
    }

    private fun getDamageTypeIcon(record: LivingKillRecord): ResourceLocation? {
        var icon: ResourceLocation?
        // 渲染爆头图标
        if (record.headshot) {
            icon = HEADSHOT
        } else {
            val item = record.attacker.mainHandItem
            if (DamageTypeTool.isCompatGunDamage(record.damageType, record.target.level().registryAccess())
                && (item.item is GunItem || item.descriptionId == "tacz:modern_kinetic_gun")
            ) {
                icon = null
                if (record.damageType === ModDamageTypes.PROJECTILE_HIT) {
                    icon = GENERIC
                } else if (record.damageType === ModDamageTypes.LASER || record.damageType === ModDamageTypes.LASER_HEADSHOT) {
                    icon = LASER
                }
                if (TACZGunEventHandler.hasMod() && !TACZGunEventHandler.compatCondition()) {
                    icon = GENERIC
                }
            } else {
                // 如果是其他伤害，则渲染对应图标
                if (record.damageType === DamageTypes.EXPLOSION || record.damageType === DamageTypes.PLAYER_EXPLOSION
                    || record.damageType === ModDamageTypes.PROJECTILE_EXPLOSION || record.damageType === DamageTypes.FIREWORKS
                    || record.damageType === ModDamageTypes.CUSTOM_EXPLOSION
                ) {
                    icon = EXPLOSION
                } else if (record.damageType === DamageTypes.PLAYER_ATTACK) {
                    icon = KNIFE
                } else if (record.damageType === ModDamageTypes.BEAST) {
                    icon = BEAST
                } else if (record.damageType === ModDamageTypes.MINE) {
                    icon = CLAYMORE
                } else if (record.damageType === ModDamageTypes.SHOCK) {
                    icon = SHOCK
                } else if (record.damageType === ModDamageTypes.BURN || record.damageType === DamageTypes.IN_FIRE || record.damageType === DamageTypes.ON_FIRE || record.damageType === DamageTypes.LAVA) {
                    icon = BURN
                } else if (record.damageType === ModDamageTypes.DRONE_HIT) {
                    icon = DRONE
                } else if (record.damageType === ModDamageTypes.LASER_STATIC) {
                    icon = LASER
                } else if (record.damageType === ModDamageTypes.VEHICLE_STRIKE) {
                    icon = VEHICLE
                } else {
                    icon = GENERIC
                }
            }
        }
        return icon
    }

    fun getEntityName(entity: Entity): String {
        val entityName = entity.displayName.string
        var name: String = entityName
        if (entity is LivingEntity && entity is OwnableEntity) {
            val owner = entity.owner
            if (owner is Player) {
                if (DisplayConfig.DOG_TAG_NAME_VISIBLE.get()) {
                    name = owner.displayName.string + " + " + entityName
                    CuriosApi.getCuriosInventory(owner).ifPresent { c ->
                        c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                            if (s.stack().hasCustomHoverName()) {
                                name = s.stack().getHoverName().string + " + " + entityName
                            }
                        }
                    }
                } else {
                    name = owner.displayName.string + " + " + entityName
                }
            }
        } else if (entity is Player) {
            if (!DisplayConfig.DOG_TAG_NAME_VISIBLE.get()) return name
            CuriosApi.getCuriosInventory(entity).ifPresent { c ->
                c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                    if (s.stack().hasCustomHoverName()) {
                        name = s.stack().getHoverName().string
                    }
                }
            }
        }
        return name
    }

    fun getTargetName(entity: Entity): String {
        val entityName = entity.displayName.string
        var name: String = entityName
        if (entity is Player) {
            if (!DisplayConfig.DOG_TAG_NAME_VISIBLE.get()) return name
            CuriosApi.getCuriosInventory(entity).ifPresent { c ->
                c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                    if (s.stack().hasCustomHoverName()) {
                        name = s.stack().getHoverName().string
                    }
                }
            }
        }
        return name
    }

    fun getWeaponIcon(record: LivingKillRecord): ResourceLocation? {
        val attacker = record.attacker
        val vehicleEntity = attacker.vehicle
        val item = record.stack.item
        if (vehicleEntity is VehicleEntity) {
            // 载具图标
            if ((vehicleEntity.banHand(attacker)) || record.damageType === ModDamageTypes.VEHICLE_STRIKE) {
                return vehicleEntity.vehicleIcon
            } else {
                if (item is GunItem) {
                    return item.getGunIcon(record.stack)
                } else if (TACZGunEventHandler.compatCondition()) {
                    return TACZGunEventHandler.getTaczCompatIcon(record.stack)
                }
            }
        } else {
            // 如果是枪械击杀，则渲染枪械图标
            if (item is GunItem) {
                return item.getGunIcon(record.stack)
            } else if (TACZGunEventHandler.compatCondition()) {
                return TACZGunEventHandler.getTaczCompatIcon(record.stack)
            }
        }
        return null
    }

    fun shouldRenderDogTagIcon(living: LivingEntity?): Boolean {
        val flag = booleanArrayOf(false)
        CuriosApi.getCuriosInventory(living).ifPresent { c ->
            c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                val stack = s.stack()
                if (ClientDogTagImageTooltip.shouldRenderIcon(stack)) {
                    flag[0] = true
                }
            }
        }
        return flag[0] && DisplayConfig.DOG_TAG_ICON_VISIBLE.get()
    }

    fun renderDogTagIcon(guiGraphics: GuiGraphics, living: LivingEntity?, x: Float, y: Float) {
        CuriosApi.getCuriosInventory(living).ifPresent { c ->
            c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                val stack = s.stack()
                val icon = DogTagItem.getColors(stack)

                guiGraphics.pose().pushPose()

                for (i in 0..15) {
                    for (j in 0..15) {
                        if (icon[i][j].toInt() == -1) continue
                        RenderHelper.fill(
                            guiGraphics, RenderType.gui(),
                            x + i * 0.6f, y + j * 0.6f, x + (i + 1) * 0.6f, y + (j + 1) * 0.6f,
                            0f, DogTagEditorScreen.getColorByNum(icon[i][j])
                        )
                    }
                }
                guiGraphics.pose().popPose()
            }
        }
    }
}
