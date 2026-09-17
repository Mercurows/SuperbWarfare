package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.data.SingleOrList
import com.atsuishio.superbwarfare.data.StringOrObject
import com.atsuishio.superbwarfare.data.gun.*
import com.atsuishio.superbwarfare.init.RegistryName
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

@RegistryName("empty_gun")
class EmptyGunItem : GunItem(Properties()) {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.empty_gun").withStyle(ChatFormatting.RED))
    }

    companion object {

        const val EMPTY_GUN_ID = "superbwarfare:empty_gun"

        @JvmField
        val EMPTY_GUN_DATA: DefaultGunData = DefaultGunData(
            maxDurability = 0,
            durabilityPerShoot = 0,
            maxEnergy = 0,
            maxReceiveEnergy = 0,
            maxExtractEnergy = 0,

            recoilX = 0.0,
            recoilY = 0.0,
            recoil = 0.0,
            recoilTime = 0,
            recoilForce = 0f,
            shootShake = null,

            defaultZoom = 0.0,
            minZoom = 0.0,
            maxZoom = 0.0,
            spread = 0.0,
            damage = 0.0,
            headshot = 0.0,
            velocity = 0.0,
            magazine = SingleOrList(0),
            range = 0,
            meleeDamage = 0.0,
            meleeDuration = 0,
            meleeDamageTime = 0,
            meleeAngle = 0,
            meleeRange = 0.0,

            projectile = StringOrObject(ProjectileInfo().apply { itemId = "empty" }),
            shootPos = ShootPos(
                positions = arrayListOf(),
                directions = arrayListOf()
            ),
            seekWeaponInfo = null,
            projectileDummyInfo = null,

            ammoCostPerShoot = 0,
            projectileAmount = 0,
            weight = 0.0,

            defaultFireMode = "",
            availableFireModes = SingleOrList(),
            reloadTypes = emptySet(),
            seekType = null,

            autoReload = null,
            withdrawAmmoWhenChangeSlot = false,
            zoomReload = false,
            clearHoldProgressAfterShoot = false,
            burstAmount = 0,
            bypassesArmor = 0.0,
            ammoConsumers = SingleOrList(
                mutableListOf(
                    StringOrObject(AmmoConsumer(ammo = SingleOrList("empty")))
                )
            ),
            useNacelleCamera = false,

            normalReloadTime = SingleOrList(0),
            emptyReloadTime = SingleOrList(0),
            boltActionTime = SingleOrList(0),
            prepareTime = SingleOrList(0),
            prepareLoadTime = SingleOrList(0),
            prepareAmmoLoadTime = SingleOrList(0),
            prepareEmptyTime = SingleOrList(0),
            iterativeTime = SingleOrList(0),
            iterativeAmmoLoadTime = SingleOrList(0),
            iterativeLoadAmount = 0,
            finishTime = SingleOrList(0),
            burstCooldown = 0,
            soundRadius = 0.0,
            rpm = 0,
            explosionDamage = 0.0,
            explosionRadius = 0.0,
            gravity = 0.0,
            shootDelay = 0,
            shootDelayTime = 0,
            heatPerShoot = 0.0,
            availablePerks = SingleOrList(),

            naturalCooldown = 0.0,
            inWaterCooldownRate = 0.0,
            inSnowCooldownRate = 0.0,
            inFireCooldownRate = 0.0,
            inLavaCooldownRate = 0.0,
            zoomSpreadRate = 0.0,

            seekTime = 0,
            seekAngle = 0.0,
            seekRange = 0.0,
            maxGuidedRange = 0.0,
            canGuidedByRadar = false,
            affectedByStealthTarget = false,
            minTargetHeight = 0.0,
            maxTargetHeight = 0.0,

            shootAnimationTime = 0,
            spreadAmount = 0,
            spreadAngle = 0,
            apDurability = 0,
            projectileLife = 0,
            addShooterDeltaMovement = false,
            underwaterMotionScale = 0f,
            explosionDestroy = false,
        ).apply {
            // 这两项是不参与序列化的运行时状态，data class 里留在类体中
            itemId = EMPTY_GUN_ID
            isDefaultData = true
        }
    }

    override fun getDefaultData(data: GunData): DefaultGunData = EMPTY_GUN_DATA
}
