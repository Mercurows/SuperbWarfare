package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.entityArg
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.subweapon.SubWeaponRuntime
import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

private const val ENTITY_ARG = "entity"

/**
 * ```
 * /sbw subweapon info [<entity>]
 * ```
 *
 * 打印主手枪械上**解析出来的**副武器：槽位 / 配件 id / 枪数据 id / 弹药 / 射速 /
 * 触发冷却（主武器冷却表上的 `sub:<槽位>`）/ 现在能不能开火。
 *
 * 调 `SubWeapon` 数据时用它对账：装上了却什么都不发生，通常就是这几项里有一项不对
 * （配件没带 `SubWeapon` 定义、物品不是 `SubWeaponItem`、`sbw/guns/<id>.json` 不存在、
 * 或者弹药根本不是这把副武器要的那种）。
 */
val SUBWEAPON_COMMAND = buildCommand("subweapon") {
    requirePermission(2)

    "info" {
        execute { printSubWeapons(this, source.entity) }

        entityArg(ENTITY_ARG) {
            execute { printSubWeapons(this, entity) }
        }
    }
}

private fun CommandContext<CommandSourceStack>.failWith(msg: Component): Int {
    source.sendFailure(msg)
    return 0
}

private fun CommandContext<CommandSourceStack>.ok(msg: Component) {
    source.sendSuccess({ msg }, false)
}

private fun printSubWeapons(context: CommandContext<CommandSourceStack>, entity: Entity?): Int {
    val stack = (entity as? LivingEntity)?.mainHandItem
    if (stack == null || !GunItem.isHeldWeapon(stack)) {
        return context.failWith(Component.translatable("commands.superbwarfare.melee.fail.not_gun"))
    }

    val gun = GunData.from(stack)
    val instances = SubWeaponRuntime.installed(gun)

    context.ok(
        Component.literal("[SubWeapon] ${entity.name.string} / ${gun.id} -> ${instances.size}")
            .withStyle(ChatFormatting.AQUA)
    )

    if (instances.isEmpty()) {
        context.ok(
            Component.literal(
                "no sub-weapon installed. A slot counts as one only when the attachment data has a " +
                        "'SubWeapon' block AND its item is a SubWeaponItem."
            ).withStyle(ChatFormatting.GRAY)
        )
        return 1
    }

    for (instance in instances) {
        val data = instance.data
        val shooter = entity as? Player

        context.ok(
            Component.literal(
                "slot=${instance.slotName} attachment=${instance.attachmentId} data=${data.id} " +
                        "ammoSlot=${instance.info.ammoSlot} " +
                        "ammo=${data.ammo.get()}/${data.get(GunProp.MAGAZINE)} " +
                        "rpm=${data.get(GunProp.RPM)} projectile=${data.get(GunProp.PROJECTILE).itemId}"
            )
        )
        context.ok(
            Component.literal(
                "    cooldown=${gun.cooldown.get(instance.cooldownKey)}/${instance.cooldownTicks()} " +
                        "reloading=${data.reloading()} bolting=${data.bolt.actionTimer.get() > 0} " +
                        "canShoot=${data.canShoot(shooter)}"
            ).withStyle(ChatFormatting.DARK_GRAY)
        )
    }

    return 1
}
