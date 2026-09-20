package com.atsuishio.superbwarfare.item.gun.machinegun

import com.atsuishio.superbwarfare.init.ModEnumExtensions
import com.atsuishio.superbwarfare.init.ModRarities
import com.atsuishio.superbwarfare.init.RegistryName
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2
import net.minecraft.client.model.HumanoidModel.ArmPose
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn

@RegistryName("minigun")
class MinigunItem : GeoGunItemV2(Properties().rarity(ModRarities.LEGENDARY)) {

    @OnlyIn(Dist.CLIENT)
    override fun armPose(
        entityLiving: LivingEntity,
        hand: InteractionHand,
        itemStack: ItemStack
    ): ArmPose {
        return if (!itemStack.isEmpty && entityLiving.usedItemHand == hand) ModEnumExtensions.Client.minigunPose else ArmPose.EMPTY
    }
}
