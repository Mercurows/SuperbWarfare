package com.atsuishio.superbwarfare.item.weapon

import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItem
import net.minecraft.world.item.ItemStack

@RegistryName("electric_baton")
class ElectricBatonItem : GeoGunItem(Properties()) {

    override fun isDamageable(stack: ItemStack?): Boolean = true

    // 不覆写 getTooltipImage：近战武器照样要显示伤害 / 爆头 / 等级 / 模组信息。
}
