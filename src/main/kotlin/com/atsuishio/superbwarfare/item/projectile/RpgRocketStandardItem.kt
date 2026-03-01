package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity
import com.atsuishio.superbwarfare.init.ModCriteriaTriggers
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.ParticleTool
import net.minecraft.core.Position
import net.minecraft.core.dispenser.BlockSource
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.Level

open class RpgRocketStandardItem : Item(Properties().stacksTo(16)), DispenserLaunchable {
    override fun getDefaultAttributeModifiers(stack: ItemStack): ItemAttributeModifiers {
        val list = ArrayList(super.getDefaultAttributeModifiers(stack).modifiers())

        list.addAll(
            listOf(
                ItemAttributeModifiers.Entry(
                    Attributes.ATTACK_DAMAGE,
                    AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5.0, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
                ),
                ItemAttributeModifiers.Entry(
                    Attributes.ATTACK_SPEED,
                    AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
                )
            )
        )

        return ItemAttributeModifiers(list, true)
    }

    override fun hurtEnemy(stack: ItemStack, entity: LivingEntity, source: LivingEntity): Boolean {
        val level = entity.level()
        if (level is ServerLevel && Math.random() < 0.25) {
            level.explode(source, source.x, source.y + 1, source.z, 5f, Level.ExplosionInteraction.NONE)
            level.explode(null, source.x, source.y + 1, source.z, 5f, Level.ExplosionInteraction.NONE)

            if (!source.level().isClientSide() && source.server != null) {
                ParticleTool.spawnMediumExplosionParticles(source.level(), source.position())
            }

            if (source is ServerPlayer) {
                ModCriteriaTriggers.RPG_MELEE_EXPLOSION.get().trigger(source)
                if (!source.isCreative) {
                    stack.shrink(1)
                }
            } else {
                stack.shrink(1)
            }
        }

        return super.hurtEnemy(stack, entity, source)
    }

    override fun getLaunchBehavior(): AbstractProjectileDispenseBehavior {
        return object : AbstractProjectileDispenseBehavior() {
            override fun getPower(): Float {
                return 2f
            }

            override fun getProjectile(level: Level, position: Position, stack: ItemStack): Projectile {
                return RpgRocketStandardEntity(
                    ModEntities.RPG_ROCKET_STANDARD.get(),
                    position.x(),
                    position.y(),
                    position.z(),
                    level,
                    340f,
                    80f,
                    5f
                )
            }

            override fun playSound(source: BlockSource) {
                source.level.playSound(null, source.pos, ModSounds.RPG_FIRE_3P.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}