package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.physics.PhysicsVehicle;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.UUID;

public class PhysicsManager {
    private static PhysicsManager instance;
    private DynamicsWorld dynamicsWorld;
    private HashMap<UUID, PhysicsVehicle> vehicles = new HashMap<>();

    public static PhysicsManager getInstance() {
        if (instance == null) {
            instance = new PhysicsManager();
        }
        return instance;
    }

    private PhysicsManager() {
        // 初始化物理世界
        DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        BroadphaseInterface broadphase = new DbvtBroadphase();
        SequentialImpulseConstraintSolver solver = new SequentialImpulseConstraintSolver();

        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3f(0, -9.8f, 0)); // 设置重力
    }

    public void update() {
        // 更新物理世界
        dynamicsWorld.stepSimulation(1/20f, 10);

        // 同步所有载具
        for (PhysicsVehicle vehicle : vehicles.values()) {
            vehicle.syncWithEntity();
        }
    }

    public void addVehicle(VehicleEntity entity, OBB obb) {
        PhysicsVehicle vehicle = new PhysicsVehicle(entity, obb);
        vehicles.put(entity.getUUID(), vehicle);
        dynamicsWorld.addRigidBody(vehicle.getRigidBody());
    }

    public void removeVehicle(UUID uuid) {
        PhysicsVehicle vehicle = vehicles.remove(uuid);
        if (vehicle != null) {
            dynamicsWorld.removeRigidBody(vehicle.getRigidBody());
        }
    }

    public PhysicsVehicle getVehicle(UUID uuid) {
        return vehicles.get(uuid);
    }
}
