package com.atsuishio.superbwarfare.item.gun.special

import com.atsuishio.superbwarfare.client.GunRendererBuilder
import com.atsuishio.superbwarfare.client.model.item.BeastGunTestModel
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GunGeoItem
import com.atsuishio.superbwarfare.item.weapon.BeastItem.Companion.beastKill
import com.atsuishio.superbwarfare.tools.TraceTool
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.phys.AABB
import software.bernie.geckolib.renderer.GeoItemRenderer
import java.util.function.Supplier
import javax.annotation.ParametersAreNonnullByDefault

@RegistryName("beast_gun_test")
open class BeastGunTestItem : GunGeoItem(Properties().rarity(ModRarities.BEAST)) {
    override fun isDamageable(stack: ItemStack): Boolean {
        return false
    }

    override fun hurtEnemy(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        beastKill(attacker, target)
        return true
    }

    override fun getSweepHitBox(stack: ItemStack, player: Player, target: Entity): AABB {
        return super.getSweepHitBox(stack, player, target).inflate(3.0)
    }

    override fun canBeHurtBy(stack: ItemStack, source: DamageSource): Boolean {
        return false
    }

    override fun isEnchantable(stack: ItemStack): Boolean {
        return false
    }

    @ParametersAreNonnullByDefault
    override fun onEntitySwing(stack: ItemStack, entity: LivingEntity, hand: InteractionHand): Boolean {
        val target = TraceTool.findMeleeEntity(entity, 51.4)
        if (target != null) {
            beastKill(entity, target)
        }
        return super.onEntitySwing(stack, entity, hand)
    }

    override fun onLeftClickEntity(stack: ItemStack, player: Player, entity: Entity): Boolean {
        beastKill(player, entity)
        return super.onLeftClickEntity(stack, player, entity)
    }

    override fun canDisableShield(
        stack: ItemStack,
        shield: ItemStack,
        entity: LivingEntity,
        attacker: LivingEntity
    ): Boolean {
        return true
    }

    @ParametersAreNonnullByDefault
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(Component.translatable("des.superbwarfare.beast").withColor(0xa56855))
    }

    override fun getRenderer(): Supplier<out GeoItemRenderer<*>> =
        GunRendererBuilder.simple { BeastGunTestModel }
}
