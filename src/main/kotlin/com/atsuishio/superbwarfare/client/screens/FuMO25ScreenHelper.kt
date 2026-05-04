package com.atsuishio.superbwarfare.client.screens

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity
import com.atsuishio.superbwarfare.inventory.menu.FuMO25Menu
import com.atsuishio.superbwarfare.tools.SeekTool
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent

@EventBusSubscriber(Dist.CLIENT)
object FuMO25ScreenHelper {
    const val TOLERANCE_DISTANCE_SQR = 256

    @JvmStatic
    var pos: BlockPos? = null

    @JvmStatic
    var entities: List<Entity>? = null

    @SubscribeEvent
    @Suppress("unused")
    fun onClientTick(event: ClientTickEvent.Post) {
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
