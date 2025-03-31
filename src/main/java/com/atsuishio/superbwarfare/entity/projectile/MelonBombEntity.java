package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

public class MelonBombEntity extends FastThrowableProjectile {

    public MelonBombEntity(EntityType<? extends MelonBombEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public MelonBombEntity(LivingEntity entity, Level level) {
        super(ModEntities.MELON_BOMB.get(), entity, level);
    }

    public MelonBombEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.MELON_BOMB.get(), level);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.MELON;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        ProjectileTool.causeCustomExplode(this, 1145f, 51.4f, 1.5f);
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 600) {
            this.discard();
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, 1145f, 51.4f, 1.5f);
            }
        }
    }

    @Override
    protected float getGravity() {
        return 0.05F;
    }
}
