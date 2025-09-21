package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.RepairToolItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.world.phys.EntityResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;
import static com.atsuishio.superbwarfare.tools.SeekTool.teamFilter;

public class RepairToolItem extends GunGeoItem {

    public RepairToolItem() {
        super(new Properties().rarity(Rarity.COMMON));
    }

    @Override
    public int getEnergyBarColor(GunData data) {
        return 0xFFFF00;
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return RepairToolItemRenderer::new;
    }

    @OnlyIn(Dist.CLIENT)
    public IClientItemExtensions getClientExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = RepairToolItem.this.getRenderer().get();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            private static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create("RepairTool", false, (model, entity, arm) -> {
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = -67.5f * Mth.DEG_TO_RAD + model.head.xRot + 0.05f * model.rightArm.xRot;
                    model.rightArm.yRot = 5f * Mth.DEG_TO_RAD + model.head.yRot;
                }
            });

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                if (!stack.isEmpty()) {
                    if (entityLiving.getUsedItemHand() == hand) {
                        return POSE;
                    }
                }
                return HumanoidModel.ArmPose.EMPTY;
            }
        };
    }

    @OnlyIn(Dist.CLIENT)
    private PlayState idlePredicate(AnimationState<RepairToolItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.repair_tool.idle"));

        var data = GunData.from(stack);
        if (ClientEventHandler.holdFire && gunItem.canShoot(data, player)) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.repair_tool.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.repair_tool.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        AnimationController<RepairToolItem> idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public void onRayHitBlock(Entity shooter, ServerLevel level, @Nullable Entity target, @NotNull GunData data, Vec3 shootDirection, BlockHitResult result, @NotNull Vec3 pos) {
        super.onRayHitBlock(shooter, level, target, data, shootDirection, result, pos);
        BlockPos blockPos = result.getBlockPos();
        BlockState state = level.getBlockState(blockPos);
        this.summonRayHitParticle(level, state, pos, shootDirection.scale(-1).normalize());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (entity instanceof Player player) {
            for (var cell : player.getInventory().items) {
                if (cell.getItem() instanceof BatteryItem) {
                    assert stack.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent();
                    var stackStorage = stack.getCapability(ForgeCapabilities.ENERGY).resolve().get();
                    int stackMaxEnergy = stackStorage.getMaxEnergyStored();
                    int stackEnergy = stackStorage.getEnergyStored();

                    assert cell.getCapability(ForgeCapabilities.ENERGY).resolve().isPresent();
                    var cellStorage = cell.getCapability(ForgeCapabilities.ENERGY).resolve().get();
                    int cellEnergy = cellStorage.getEnergyStored();

                    int stackEnergyNeed = Math.min(cellEnergy, stackMaxEnergy - stackEnergy);

                    if (cellEnergy > 0) {
                        stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                                iEnergyStorage -> iEnergyStorage.receiveEnergy(stackEnergyNeed, false)
                        );
                    }
                    cell.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                            cEnergy -> cEnergy.extractEnergy(stackEnergyNeed, false)
                    );
                }
            }
        }
    }

    @Override
    public SoundEvent getRayHitBlockSound(GunData data) {
        return ModSounds.REPAIRING.get();
    }

    @Override
    public void onRayHitEntity(Entity shooter, ServerLevel level, @NotNull GunData data, EntityResult result) {
        var target = result.getEntity();

        // 修理实体（多重含义）
        if (target instanceof VehicleEntity vehicle) {
            Entity lastDriver = EntityFindUtil.findEntity(level, vehicle.getEntityData().get(LAST_DRIVER_UUID));
            if ((lastDriver != null && !teamFilter(shooter, lastDriver) && lastDriver.getTeam() != null) || shooter.isShiftKeyDown()) {
                vehicle.hurt(ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter), 0.5f);
                if (shooter instanceof ServerPlayer player) {
                    player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            } else {
                vehicle.heal(0.5f + 0.0025f * vehicle.getMaxHealth());
            }
        } else if (target instanceof LivingEntity living) {
            if (target.getType().is(ModTags.EntityTypes.CAN_REPAIR) && !shooter.isShiftKeyDown()) {
                living.heal(0.5f + 0.0025f * living.getMaxHealth());
            } else {
                ICustomKnockback iCustomKnockback = ICustomKnockback.getInstance(living);
                iCustomKnockback.superbWarfare$setKnockbackStrength(0);

                float damage = data.get(GunProp.DAMAGE).floatValue();
                DamageHandler.doDamage(living, ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter), damage);
                target.invulnerableTime = 0;

                iCustomKnockback.superbWarfare$resetKnockbackStrength();

                if (shooter instanceof ServerPlayer player) {
                    player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }
        }
    }

    @Override
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/repair_tool_icon.png");
    }

    @Override
    public boolean canZoom(GunData data, @Nullable Entity shooter) {
        return false;
    }

    public void summonRayHitParticle(ServerLevel serverLevel, BlockState state, Vec3 pos, Vec3 dir) {
        BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 1; i++) {
            Vec3 vec3 = this.randomVec(dir, 40);
            sendParticle(serverLevel, particleData, pos.x + 0.05 * i * dir.x, pos.y + 0.05 * i * dir.y, pos.z + 0.05 * i * dir.z, 0, vec3.x, vec3.y, vec3.z, 10, true);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 vec3 = this.randomVec(dir, 20);
            sendParticle(serverLevel, ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.05, true);
        }
        for (int i = 0; i < 2; i++) {
            Vec3 vec3 = this.randomVec(dir, 80);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.2 + 0.1 * Math.random(), true);
        }
    }
}
