package com.atsuishio.superbwarfare.physics

import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import cn.solarmoon.spark_core.physics.body.CollisionGroups
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque

/** Native objects are owned by the client physics thread; this space has no gameplay listeners. */
internal class VehiclePrediction(private val vehicle: VehicleBody) : AutoCloseable {
    private val space = object : PhysicsSpace(BroadphaseType.DBVT) {
        override fun needsCollision(a: PhysicsCollisionObject, b: PhysicsCollisionObject): Boolean =
            a.collisionGroup and b.collideWithGroups != 0 || b.collisionGroup and a.collideWithGroups != 0
    }
    private val body = PhysicsRigidBody(vehicle.body.collisionShape, vehicle.mass)
    private data class Obstacle(val shape: CollisionShape, val position: Vector3f, val rotation: Quaternion,
        val velocity: Vector3f, val omega: Vector3f, val group: Int, val friction: Float)
    private data class History(val input: DriveFrame, val obstacles: List<Obstacle>)
    private val history = ArrayDeque<History>()
    private val proxies = ArrayList<PhysicsRigidBody>()
    private var frame = 0L

    init {
        body.friction = vehicle.body.friction
        body.restitution = vehicle.body.restitution
        body.setDamping(vehicle.settings.linearDamping, vehicle.settings.angularDamping)
        body.collisionGroup = vehicle.body.collisionGroup
        // The display body deliberately does not respond to terrain on the
        // client. Prediction still needs terrain proxies for chassis contacts
        // and suspension sweeps.
        body.isContactResponse = true
        body.setCollideWithGroups(vehicle.body.collideWithGroups or CollisionGroups.TERRAIN)
        body.ccdMotionThreshold = vehicle.body.ccdMotionThreshold
        body.ccdSweptSphereRadius = vehicle.body.ccdSweptSphereRadius
        space.addCollisionObject(body)
        body.setProtectGravity(true)
        body.setGravity(Vector3f(0f, -vehicle.settings.gravity.toFloat(), 0f))
        restore(vehicle.state)
    }

    private fun restore(state: RigidState) {
        // Remove/reinsert to discard warm-start contact caches from the rejected timeline.
        space.removeCollisionObject(body)
        body.clearForces()
        body.setPhysicsLocation(vehicle.center(state.position, state.rotation).bullet())
        body.setPhysicsRotation(state.rotation.bullet())
        body.setLinearVelocity(state.velocity.bullet())
        body.setAngularVelocity(state.angularVelocity.bullet())
        space.addCollisionObject(body)
        body.activate(true)
    }

    fun tick(input: DriveFrame, authority: RigidState?): RigidState {
        if (authority != null) {
            val expired = history.isNotEmpty() && authority.sequence < history.first.input.sequence - 1
            while (history.isNotEmpty() && history.first.input.sequence <= authority.sequence) history.removeFirst()
            if (expired || authority.position.distanceTo(vehicle.state.position) > 16.0) history.clear()
            restore(authority)
            for (entry in history) simulate(entry)
        }
        val center = body.getPhysicsLocation(null)
        val obstacles = vehicle.physics.world.pcoList.asSequence().filter {
            it !== vehicle.body && it.isContactResponse &&
                it.getPhysicsLocation(null).subtract(center).lengthSquared() < 32f * 32f
        }.map { other ->
            val rigid = other as? PhysicsRigidBody
            Obstacle(other.collisionShape, other.getPhysicsLocation(null), other.getPhysicsRotation(null),
                rigid?.getLinearVelocity(null) ?: Vector3f(), rigid?.getAngularVelocity(null) ?: Vector3f(),
                other.collisionGroup, other.friction)
        }.toList()
        val entry = History(input, obstacles)
        if (history.isEmpty() || history.last.input.sequence != input.sequence) {
            history.addLast(entry)
            while (history.size > 40) history.removeFirst()
            simulate(entry)
        }
        val q = body.getPhysicsRotation(null).joml()
        return RigidState(++frame, input.sequence, vehicle.origin(body.getPhysicsLocation(null).minecraft(), q), q,
            body.getLinearVelocity(null).minecraft(), body.getAngularVelocity(null).minecraft(), vehicle.isGrounded)
    }

    private fun simulate(entry: History) {
        proxies.forEach(space::removeCollisionObject)
        proxies.clear()
        entry.obstacles.forEach { value ->
            val proxy = PhysicsRigidBody(value.shape, 0f)
            proxy.isKinematic = true
            proxy.collisionGroup = value.group
            proxy.setCollideWithGroups(body.collisionGroup)
            proxy.friction = value.friction
            proxy.setPhysicsLocation(value.position)
            proxy.setPhysicsRotation(value.rotation)
            proxy.setLinearVelocity(value.velocity)
            proxy.setAngularVelocity(value.omega)
            space.addCollisionObject(proxy)
            proxies.add(proxy)
        }
        repeat(3) { step ->
            proxies.forEachIndexed { index, proxy ->
                val value = entry.obstacles[index]
                proxy.setPhysicsLocation(value.position.add(value.velocity.mult((step + 1) / 60f)))
                val speed = value.omega.length()
                if (speed > .0001f) proxy.setPhysicsRotation(Quaternion().fromAngleNormalAxis(speed * (step+1) / 60f, value.omega.normalize()).mult(value.rotation))
            }
            vehicle.applyDrive(body, space, entry.input)
            if (vehicle.groundVehicle && vehicle.settings.suspensionEnabled && vehicle.trackedVehicle) {
                vehicle.applyTrackSuspension(body, space)
            }
            space.update(1f/60f, 0, false, true, false)
        }
    }

    override fun close() { history.clear(); proxies.clear(); space.destroy() }
}
