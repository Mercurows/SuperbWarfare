package com.atsuishio.superbwarfare.item.projectile

import com.atsuishio.superbwarfare.advancement.CriteriaRegister
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity
import com.atsuishio.superbwarfare.init.ModEntities
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.item.DispenserLaunchable
import com.atsuishio.superbwarfare.tools.ParticleTool
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import net.minecraft.core.BlockSource
import net.minecraft.core.Position
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import kotlin.random.Random

open class RpgRocketStandardItem : Item(Properties().stacksTo(16)), DispenserLaunchable {
    override fun getAttributeModifiers(
        slot: EquipmentSlot,
        stack: ItemStack
    ): Multimap<Attribute, AttributeModifier> {
        var map = super.getAttributeModifiers(slot, stack)
        if (slot == EquipmentSlot.MAINHAND) {
            map = HashMultimap.create<Attribute, AttributeModifier>(map)
            map.put(
                Attributes.ATTACK_DAMAGE,
                AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Item modifier", 5.0, AttributeModifier.Operation.ADDITION)
            )
            map.put(
                Attributes.ATTACK_SPEED,
                AttributeModifier(BASE_ATTACK_SPEED_UUID, "Item modifier", -2.4, AttributeModifier.Operation.ADDITION)
            )
        }
        return map
    }

    override fun hurtEnemy(stack: ItemStack, entity: LivingEntity, source: LivingEntity): Boolean {
        val level = entity.level()
        if (level is ServerLevel && Random.nextDouble() < 0.25) {
            level.explode(source, source.x, source.y + 1, source.z, 5f, Level.ExplosionInteraction.NONE)
            level.explode(null, source.x, source.y + 1, source.z, 5f, Level.ExplosionInteraction.NONE)

            if (!source.level().isClientSide()) {
                ParticleTool.spawnMediumExplosionParticles(source.level(), source.position())
            }

            if (source is ServerPlayer) {
                CriteriaRegister.RPG_MELEE_EXPLOSION.trigger(source)
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

            override fun getProjectile(pLevel: Level, pPosition: Position, pStack: ItemStack): Projectile {
                return RpgRocketStandardEntity(
                    ModEntities.RPG_ROCKET_STANDARD.get(),
                    pPosition.x(),
                    pPosition.y(),
                    pPosition.z(),
                    pLevel,
                    340f,
                    80f,
                    5f
                )
            }

            override fun playSound(pSource: BlockSource) {
                pSource.level.playSound(null, pSource.pos, ModSounds.RPG_FIRE_3P.get(), SoundSource.BLOCKS, 1f, 1f)
            }
        }
    }
}