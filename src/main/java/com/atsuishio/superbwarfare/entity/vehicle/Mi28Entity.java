package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PlayMessages;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Mi28Entity extends VehicleEntity implements GeoEntity, WeaponVehicleEntity, OBBEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;
    public OBB obb8;
    public OBB obb9;
    public OBB obb10;
    public OBB obb11;

    public Mi28Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.MI_28.get(), world);
    }

    public Mi28Entity(EntityType<Mi28Entity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(1.25f, 0.75f, 2.75f), new Quaternionf(), OBB.Part.BODY);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.9375f, 0.65625f, 1.875f), new Quaternionf(), OBB.Part.BODY);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.65625f, 3.6875f), new Quaternionf(), OBB.Part.BODY);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.40625f, 0.96875f), new Quaternionf(), OBB.Part.BODY);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(1.8125f, 0.53125f, 3.28125f), new Quaternionf(), OBB.Part.BODY);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.90625f, 4.375f), new Quaternionf(), OBB.Part.BODY);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(0.75f, 0.375f, 0.75f), new Quaternionf(), OBB.Part.TURRET);
        this.obb8 = new OBB(this.position().toVector3f(), new Vector3f(0.96875f, 0.875f, 1.1875f), new Quaternionf(), OBB.Part.BODY);
        this.obb9 = new OBB(this.position().toVector3f(), new Vector3f(0.96875f, 0.875f, 1.1875f), new Quaternionf(), OBB.Part.BODY);
        this.obb9 = new OBB(this.position().toVector3f(), new Vector3f(0.96875f, 0.875f, 1.1875f), new Quaternionf(), OBB.Part.BODY);
        this.obb10 = new OBB(this.position().toVector3f(), new Vector3f(0.375f, 0.84375f, 1.09375f), new Quaternionf(), OBB.Part.SUB_ENGINE);
        this.obb11 = new OBB(this.position().toVector3f(), new Vector3f(0.625f, 0.8125f, 0.625f), new Quaternionf(), OBB.Part.MAIN_ENGINE);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        mi28TerrainCompact();
        updateOBB();
    }

    public void mi28TerrainCompact() {
        if (onGround()) {
            Matrix4f transform = getVehicleTransform(1);

            // 后轮
            Vector4f position = transformPosition(transform, 0, 0.58f, -11.1f);
            Vec3 p = new Vec3(position.x, position.y, position.z);

            var level = level();
            var res = level.clip(new ClipContext(p, p.add(0, -5, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            double heightY;

            BlockPos blockPos = BlockPos.containing(p);
            BlockState state = level.getBlockState(blockPos);
            VoxelShape shape = state.getCollisionShape(level, blockPos);

            if (!shape.isEmpty()) {
                heightY = p.y - (shape.max(Direction.Axis.Y) + blockPos.getY());
            } else if (res.getType() == HitResult.Type.BLOCK && level.noCollision(new AABB(p, p))) {
                heightY = p.y - res.getLocation().y;
            } else {
                heightY = 0;
            }

            setXRot((float) (getXRot() - 5f * heightY));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getMouseSensitivity() {
        return 0.25;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7, this.obb8, this.obb9, this.obb10, this.obb11);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, 0, 1.75f, -0.5625f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 0, 1.84375f, 4.0625f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, 0, 3.15625f, -0.3125f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, 0f, 2.90625f, 4.34375f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, 0f, 3.28125f, 0.09375f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 0, 1.90625f, -7.6875f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0, 0.8125f, 4.1875f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition8 = transformPosition(transform, 2.46875f, 2.125f, -0.0625f);
        this.obb8.center().set(new Vector3f(worldPosition8.x, worldPosition8.y, worldPosition8.z));
        this.obb8.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition9 = transformPosition(transform, -2.46875f, 2.125f, -0.0625f);
        this.obb9.center().set(new Vector3f(worldPosition9.x, worldPosition9.y, worldPosition9.z));
        this.obb9.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition10 = transformPosition(transform, 0, 3.65625f, -11.71875f);
        this.obb10.center().set(new Vector3f(worldPosition10.x, worldPosition10.y, worldPosition10.z));
        this.obb10.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition11 = transformPosition(transform, 0, 4.625f, 0.5f);
        this.obb11.center().set(new Vector3f(worldPosition11.x, worldPosition11.y, worldPosition11.z));
        this.obb11.setRotation(VectorTool.combineRotations(1, this));
    }
}
