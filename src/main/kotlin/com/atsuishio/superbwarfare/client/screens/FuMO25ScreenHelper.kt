package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object FuMO25ScreenHelper {
    const val TOLERANCE_DISTANCE_SQR = 256

    @JvmStatic
    var pos: BlockPos? = null

    @JvmStatic
    var entities: List<Entity>? = null

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.side != LogicalSide.CLIENT) return
        if (event.phase != TickEvent.Phase.END) return

        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera
        val cameraPos = camera.position

        val menu = player.containerMenu as? FuMO25Menu ?: return
        if (pos == null) return

        if (pos!!.distToCenterSqr(cameraPos) > TOLERANCE_DISTANCE_SQR) {
            pos = BlockPos.containing(cameraPos)
        }

        if (menu.energy <= 0) {
            resetEntities()
            return
        }

        val funcType = menu.funcType
        entities = SeekTool.getEntitiesWithinRange(
            pos, player.level(),
            if (funcType == 1.toLong()) FuMO25BlockEntity.MAX_RANGE.toDouble() else FuMO25BlockEntity.DEFAULT_RANGE.toDouble()
        )
    }

    @JvmStatic
    fun resetEntities() {
        if (entities != null) {
            entities = null
        }
    }
}
