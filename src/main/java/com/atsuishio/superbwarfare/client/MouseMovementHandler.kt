package com.atsuishio.superbwarfare.client

import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.MouseHandler
import net.minecraft.world.phys.Vec2
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW

/**
 * Codes from @getItemFromBlock's Create-Tweaked-Controllers
 */
object MouseMovementHandler {
    lateinit var delta: Vec2
    lateinit var lastPos: Vec2
    lateinit var vel: Vec2
    private lateinit var mouseHandler: MouseHandler
    private var mouseLockActive: Boolean = false
    private var savedRot: Vector3f = Vector3f()

    fun getMousePos(): Vec2 {
        if (mouseHandler.isMouseGrabbed) {
            return Vec2(mouseHandler.xpos().toFloat(), mouseHandler.ypos().toFloat())
        } else {
            val x = doubleArrayOf(0.0)
            val y = doubleArrayOf(0.0)
            GLFW.glfwGetCursorPos(mc.window.window, x, y)
            return Vec2(x[0].toFloat(), y[0].toFloat())
        }
    }

    fun resetCenter() {
        delta = Vec2(0f, 0f)
        vel = Vec2(0f, 0f)
        lastPos = getMousePos()
    }

    fun init() {
        delta = Vec2(0f, 0f)
        vel = Vec2(0f, 0f)
        mouseHandler = mc.mouseHandler
        lastPos = getMousePos()
    }

    fun getX(useVelocity: Boolean): Float {
        return if (useVelocity) {
            vel.x
        } else {
            delta.x
        }
    }

    fun getY(useVelocity: Boolean): Float {
        return if (useVelocity) {
            vel.y
        } else {
            delta.y
        }
    }

    fun activateMouseLock() {
        val player = localPlayer ?: return

        savedRot.x = player.xRot
        savedRot.y = player.yRot
        savedRot.z = 0f
        mouseLockActive = true
        lastPos = getMousePos()
    }

    fun deactivateMouseLock() {
        mouseLockActive = false
    }

    fun cancelPlayerTurn() {
        if (!mouseLockActive) return
        val player = localPlayer ?: return

        player.turn((savedRot.y - player.yRot) / 0.15, (savedRot.x - player.xRot) / 0.15)
        player.xBob = savedRot.x
        player.yBob = savedRot.y
        player.xBobO = savedRot.x
        player.yBobO = savedRot.y
    }
}
