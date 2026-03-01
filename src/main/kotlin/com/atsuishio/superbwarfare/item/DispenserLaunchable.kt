package com.atsuishio.superbwarfare.item

import net.minecraft.core.dispenser.DispenseItemBehavior

interface DispenserLaunchable {
    fun getLaunchBehavior(): DispenseItemBehavior
}
