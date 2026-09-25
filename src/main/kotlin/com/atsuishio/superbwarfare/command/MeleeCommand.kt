package com.atsuishio.superbwarfare.command

import com.atsuishio.superbwarfare.command.builder.buildCommand
import com.atsuishio.superbwarfare.command.builder.entityArg
import com.atsuishio.superbwarfare.command.builder.intArg
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.melee.MeleeAction
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.resource.gun.GunAnimationNames
import com.atsuishio.superbwarfare.resource.gun.GunResource
import com.mojang.brigadier.context.CommandContext
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

private const val ENTITY_ARG = "entity"
private const val INDEX_ARG = "index"

/**
 * ```
 * /sbw melee info [<entity>]
 * /sbw melee actions [<entity>]
 * /sbw melee force <index> [<entity>]
 * ```
 *
 * 三条指令都作用于实体主手的枪械（不写实体时就是执行者自己），主手不是 [GunItem] 时失败。
 *
 * - `info`：打印**解析后**的近战总览（近战判定来源 / 距离 / 形状 / 横扫 / 连招窗口 / 冷却表）
 * - `actions`：逐段打印动作表（`Animation` 候选链及其拼出来的 clip 名 / `Duration` / `HitTime` / 形状 / 伤害 / 倍率 / 冷却）
 * - `force <index>`：把连招下标强制设成 `index` 并**立刻触发一次挥击**——调参时不用先挥两下进第二段
 *
 * 判定体可视化不在服务端：客户端按住 `F3 + B`（原版 hitbox 显示）再持一把能近战的枪即可看到
 * 判定体外框（见 `MeleeDebugRenderer`）。
 */
val MELEE_COMMAND = buildCommand("melee") {
    requirePermission(2)

    "info" {
        execute { printMeleeInfo(this, source.entity) }

        entityArg(ENTITY_ARG) {
            execute { printMeleeInfo(this, entity) }
        }
    }

    "actions" {
        execute { printActions(this, source.entity) }

        entityArg(ENTITY_ARG) {
            execute { printActions(this, entity) }
        }
    }

    "force" {
        intArg(INDEX_ARG, min = 0) {
            execute { forceSwing(this, intArg, source.entity) }

            entityArg(ENTITY_ARG) {
                execute { forceSwing(this, intArg, entity) }
            }
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

private fun mainHandGunData(entity: Entity?): GunData? {
    val stack = (entity as? LivingEntity)?.mainHandItem ?: return null
    if (stack.item !is GunItem) return null
    return GunData.from(stack)
}

private fun notGunMessage(): Component =
    Component.translatable("commands.superbwarfare.melee.fail.not_gun")

/** 主手枪械的 id（动画 clip 名的前缀就是它），主手不是枪时返回 `null` */
private fun mainHandGunId(entity: Entity?): String? {
    val stack = (entity as? LivingEntity)?.mainHandItem ?: return null
    if (stack.item !is GunItem) return null
    return GunResource.from(stack).id
}

/**
 * 把本段动作的动画候选链打成一行：`hit_bayonet/anim.ak_47.hit_bayonet | hit/anim.ak_47.hit`。
 *
 * 短名拼成什么由 [GunAnimationNames] 决定；**具体哪一支存在只有客户端知道**（要读动画文件），
 * 所以这里只展示"客户端会按顺序试哪些名字"。
 */
private fun animationChain(action: MeleeAction, gunId: String?): String {
    val candidates = action.animationCandidates()
    if (candidates.isEmpty()) return "<from GunAnimation.Melee>"
    if (gunId == null) return candidates.joinToString(" | ")

    return candidates.joinToString(" | ") { "$it -> ${GunAnimationNames.resolve(it, gunId)}" }
}

private fun printMeleeInfo(context: CommandContext<CommandSourceStack>, entity: Entity?): Int {
    val data = mainHandGunData(entity) ?: return context.failWith(notGunMessage())
    val target = entity ?: return context.failWith(notGunMessage())

    context.ok(Component.literal("[Melee] ${target.name.string} / ${data.id}").withStyle(ChatFormatting.AQUA))

    val projectile = data.get(GunProp.PROJECTILE).itemId
    val meleeOnly = data.meleeOnly()
    val explicit = data.projectileIsMelee()
    context.ok(
        Component.literal(
            "meleeOnly = $meleeOnly " + when {
                explicit -> "(Projectile = @melee)"
                meleeOnly -> "(implicit: ProjectileAmount <= 0 && MeleeDamage > 0 — migrate to \"Projectile\": \"@melee\")"
                else -> "(Projectile = $projectile)"
            }
        ).withStyle(if (meleeOnly && !explicit) ChatFormatting.YELLOW else ChatFormatting.GRAY)
    )

    context.ok(
        Component.literal(
            "MeleeDamage=${data.get(GunProp.MELEE_DAMAGE)}  MeleeDuration=${data.get(GunProp.MELEE_DURATION)}  " +
                    "MeleeDamageTime=${data.get(GunProp.MELEE_DAMAGE_TIME)}  MeleeRange=${data.get(GunProp.MELEE_RANGE)}  " +
                    "MeleeHeadshot=${data.get(GunProp.MELEE_HEADSHOT)}  MeleeLegshot=${data.get(GunProp.MELEE_LEGSHOT)}  " +
                    "MeleeComboReset=${data.get(GunProp.MELEE_COMBO_RESET)}"
        )
    )

    val hitbox = data.get(GunProp.MELEE_HITBOX)
    context.ok(
        if (hitbox == null) {
            Component.literal(
                "MeleeHitbox = <none> -> default Box (Width=1.8, Height=1.8, YOffset=-0.2)"
            ).withStyle(ChatFormatting.GRAY)
        } else {
            Component.literal(
                "MeleeHitbox = ${hitbox.type} range=${hitbox.range} angle=${hitbox.angle} pitch=${hitbox.pitch} " +
                        "width=${hitbox.width} height=${hitbox.height} " +
                        "yOffset=${hitbox.yOffset} zFrom=${hitbox.zFrom} radius=${hitbox.radius} occlusion=${hitbox.occlusion}"
            )
        }
    )

    val sweep = data.get(GunProp.MELEE_SWEEP)
    context.ok(
        Component.literal(
            if (sweep == null) "MeleeSweep = <none> -> static"
            else "MeleeSweep = ${sweep.from}..${sweep.to} steps=${sweep.resolvedSteps()}"
        )
    )

    val cooldowns = data.cooldown.entries()
    context.ok(Component.literal("Cooldowns = ${if (cooldowns.isEmpty()) "<empty>" else cooldowns}"))

    return 1
}

private fun printActions(context: CommandContext<CommandSourceStack>, entity: Entity?): Int {
    val data = mainHandGunData(entity) ?: return context.failWith(notGunMessage())
    val gunId = mainHandGunId(entity)

    val actions = data.meleeActions()
    context.ok(Component.literal("[Melee] actions = ${actions.size}").withStyle(ChatFormatting.AQUA))

    for (index in actions.indices) {
        val raw = actions[index]
        val resolved = data.resolveMeleeAction(index)
        context.ok(
            Component.literal(
                "#$index animation=${animationChain(raw, gunId)} " +
                        "duration=${resolved.duration} hitTime=${resolved.hitTime} " +
                        // 伤害/距离都来自枪的全局属性，动作只给倍率（不写 = ×1.0）
                        "damage=${"%.2f".format(resolved.damage)} (x${raw.damageMultiplier ?: 1.0} of MeleeDamage) " +
                        "hitbox=${resolved.hitbox.type} range=${"%.2f".format(resolved.hitbox.range)} " +
                        "(x${raw.rangeMultiplier ?: 1.0} of Range+MeleeRange) " +
                        "angle=${"%.1f".format(resolved.hitbox.angle)} " +
                        "sweep=${resolved.sweep?.let { "${it.from}..${it.to}" } ?: "<static>"} " +
                        "maxTargets=${resolved.maxTargets} falloff=${resolved.falloff} sortBy=${resolved.sortBy} " +
                        "knockback=${resolved.knockback} bypassesArmor=${resolved.bypassesArmor} " +
                        "headshot=${resolved.resolvedHeadshot(data.get(GunProp.MELEE_HEADSHOT))} " +
                        "legshot=${resolved.resolvedLegshot(data.get(GunProp.MELEE_LEGSHOT))} " +
                        "durability=${resolved.durability} cooldown=${resolved.cooldown} " +
                        "effects=${raw.effects?.size ?: 0}"
            )
        )
    }
    return 1
}

private fun forceSwing(context: CommandContext<CommandSourceStack>, index: Int, entity: Entity?): Int {
    val data = mainHandGunData(entity) ?: return context.failWith(notGunMessage())
    if (entity !is Player) {
        return context.failWith(Component.translatable("commands.superbwarfare.melee.fail.not_player"))
    }

    val actions = data.meleeActions()
    if (index >= actions.size) {
        return context.failWith(
            Component.translatable("commands.superbwarfare.melee.fail.index", index, actions.size)
        )
    }

    MeleeDebugHooks.forceSwing(entity, data, index)

    if (!MeleeDebugHooks.isAvailable) {
        return context.failWith(Component.translatable("commands.superbwarfare.melee.fail.no_client"))
    }

    context.source.sendSuccess(
        { Component.literal("[Melee] forced action #$index").withStyle(ChatFormatting.GREEN) }, true
    )
    return 1
}
