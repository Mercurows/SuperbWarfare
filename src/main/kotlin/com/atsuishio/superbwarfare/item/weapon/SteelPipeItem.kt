package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.function.Consumer

@RegistryName("steel_pipe")
class SteelPipeItem : GeoGunItem(Properties()) {

    override fun isDamageable(stack: ItemStack?): Boolean = true

    override fun <T : LivingEntity> damageItem(
        stack: ItemStack,
        amount: Int,
        entity: T,
        onBroken: Consumer<T>,
    ): Int {
        if (stack.damageValue + amount >= stack.maxDamage) {
            entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack(Items.STICK))
            return 0
        }
        return amount
    }
}
