package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

open class Hpj11Entity(type: EntityType<Hpj11Entity>, world: Level) : AutoAimableEntity(type, world) {

    // PJM: в CIWS (H/PJ-11) может садиться только оператор с правами администратора (op level >= 2).
    // Попытка обычной посадки — это не-shift взаимодействие, когда игрок ещё не в этой технике.
    // Такой не-админ получает PASS (не садится). Shift-действия (копирование dog tag и т.п.)
    // и уже сидящий игрок проходят в super как обычно.
    override fun interact(player: Player, hand: InteractionHand): InteractionResult {
        if (!player.isShiftKeyDown && player.vehicle !== this && !player.hasPermissions(2)) {
            return InteractionResult.PASS
        }
        return super.interact(player, hand)
    }
}
