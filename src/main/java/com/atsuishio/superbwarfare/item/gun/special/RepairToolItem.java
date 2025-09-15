package com.atsuishio.superbwarfare.item.gun.special;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider;
import com.atsuishio.superbwarfare.client.particle.BulletDecalOption;
import com.atsuishio.superbwarfare.client.renderer.gun.RepairToolItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.EnergyImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModParticleTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SoundTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity.LAST_DRIVER_UUID;
import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;
import static com.atsuishio.superbwarfare.tools.SeekTool.teamFilter;

public class RepairToolItem extends GunItem {

    public static final int MAX_ENERGY = 100000;
    protected final RandomSource random = RandomSource.create();

    private final Supplier<Integer> energyCapacity;

    public RepairToolItem() {
        super(new Properties().rarity(Rarity.COMMON));
        this.energyCapacity = () -> MAX_ENERGY;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        if (!pStack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
            return false;
        }

        int[] energy = {0};
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> energy[0] = e.getEnergyStored()
        );
        return energy[0] != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        int[] energy = {0};
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> energy[0] = e.getEnergyStored()
        );

        return Math.round((float) energy[0] * 13.0F / MAX_ENERGY);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
        return new ItemEnergyProvider(stack, energyCapacity.get());
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
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

    /**
     * 服务端处理单次开火
     *
     * @param shooter        射击者
     * @param level          ServerLevel
     * @param shootPosition  子弹位置
     * @param shootDirection 射击方向
     * @param data           GunData
     * @param spread         子弹散布
     * @param zoom           是否开镜
     * @param uuid           已锁定实体UUID
     */
    @Override
    public void shoot(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        if (!data.canShoot(shooter)) return;

        // 检测看到的目标或位置
        if (shooter != null) {
            double range = 3;

            Entity lookingEntity = null;

            double distance = range * range;
            Vec3 eyePos = shooter.getEyePosition(1.0f);
            HitResult hitResult = shooter.pick(range, 1.0f, false);

            Vec3 viewVec = shooter.getViewVector(1.0F);
            Vec3 toVec = eyePos.add(viewVec.x * range, viewVec.y * range, viewVec.z * range);
            AABB aabb = shooter.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D, 1.0D, 1.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(shooter, eyePos, toVec, aabb, p -> !p.isSpectator() && p.isAlive(), distance);
            if (entityhitresult != null) {
                hitResult = entityhitresult;

            }
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                lookingEntity = ((EntityHitResult) hitResult).getEntity();
            }

            BlockHitResult result = shooter.level().clip(new ClipContext(shootPosition, shootPosition.add(shootDirection.scale(3)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));

            BlockPos blockPos = result.getBlockPos();
            BlockState state = level.getBlockState(blockPos);

            Vec3 pos = null;

            if (state.canOcclude()) {
                pos = result.getLocation();
            }

            if (lookingEntity != null) {
                pos = hitResult.getLocation();

                //修理实体（多重含义）
                if (lookingEntity instanceof VehicleEntity vehicle) {
                    Entity lastDriver = EntityFindUtil.findEntity(level, vehicle.getEntityData().get(LAST_DRIVER_UUID));
                    if (shooter.isShiftKeyDown()) {
                        vehicle.onHurt(0.5f, shooter, false);
                        if (shooter instanceof ServerPlayer player) {
                            player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                        }
                    } else {
                        if (lastDriver != null && !teamFilter(shooter, lastDriver) && lastDriver.getTeam() != null) {
                            vehicle.onHurt(0.5f, shooter, false);
                            if (shooter instanceof ServerPlayer player) {
                                player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                            }
                        } else {
                            vehicle.heal(0.5f + 0.0025f * vehicle.getMaxHealth());
                        }
                    }
                } else if (lookingEntity instanceof LivingEntity living) {
                    if (shooter.isShiftKeyDown()) {
                        DamageHandler.doDamage(lookingEntity, ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter), data.get(GunProp.DAMAGE).floatValue());
                        lookingEntity.invulnerableTime = 0;

                        if (shooter instanceof ServerPlayer player) {
                            player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                            Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                        }
                    } else {
                        if (lookingEntity.getType().is(ModTags.EntityTypes.CAN_REPAIR)) {
                            living.heal(0.5f + 0.0025f * living.getMaxHealth());
                        } else {
                            DamageHandler.doDamage(lookingEntity, ModDamageTypes.causeRepairToolDamage(level.registryAccess(), shooter), data.get(GunProp.DAMAGE).floatValue());
                            lookingEntity.invulnerableTime = 0;

                            if (shooter instanceof ServerPlayer player) {
                                player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 0.1f, 1);
                                Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                            }
                        }
                    }
                }
            }

            // 生成粒子
            if (pos != null) {
                summonVectorParticle(level, state, pos, shootDirection.scale(-1).normalize());
                if (lookingEntity == null) {
                    BulletDecalOption bulletDecalOption = new BulletDecalOption(result.getDirection(), result.getBlockPos());
                    ParticleTool.sendParticle(level, bulletDecalOption, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0, true);
                }
                level.playSound(null, pos.x, pos.y, pos.z, ModSounds.REPAIRING.get(), SoundSource.BLOCKS, 0.7F, (float) ((2 * Math.random() - 1) * 0.05f + 1.0f));
            }
        }

        // 添加热量
        data.heat.set(Mth.clamp(data.heat.get() + data.get(GunProp.HEAT_PER_SHOOT), 0, 100));

        // 过热
        if (data.heat.get() >= 100 && !data.overHeat.get()) {
            data.overHeat.set(true);
            if (shooter instanceof ServerPlayer serverPlayer) {
                SoundTool.playLocalSound(serverPlayer, ModSounds.MINIGUN_OVERHEAT.get(), 2f, 1f);
            }
        }

        playFireSounds(data, shooter, zoom);

        // 开火后事件
        data.item.afterShoot(shooter, level, shootPosition, shootDirection, data, spread, zoom, uuid);
    }

    public void summonVectorParticle(ServerLevel serverLevel, BlockState state, Vec3 pos, Vec3 dir) {
        BlockParticleOption particleData = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < 1; i++) {
            Vec3 vec3 = randomVec(dir, 40);
            sendParticle(serverLevel, particleData, pos.x + 0.05 * i * dir.x, pos.y + 0.05 * i * dir.y, pos.z + 0.05 * i * dir.z, 0, vec3.x, vec3.y, vec3.z, 10, true);
        }
        for (int i = 0; i < 3; i++) {
            Vec3 vec3 = randomVec(dir, 20);
            sendParticle(serverLevel, ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.05, true);
        }
        for (int i = 0; i < 2; i++) {
            Vec3 vec3 = randomVec(dir, 80);
            sendParticle(serverLevel, ModParticleTypes.FIRE_STAR.get(), pos.x, pos.y, pos.z, 0, vec3.x, vec3.y, vec3.z, 0.2 + 0.1 * Math.random(), true);
        }
    }

    public Vec3 randomVec(Vec3 vec3, double spread) {
        return vec3.normalize().add(random.triangle(0.0D, 0.0172275D * spread), this.random.triangle(0.0D, 0.0172275D * spread), this.random.triangle(0.0D, 0.0172275D * spread));
    }


    @Override
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/repair_tool_icon.png");
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new EnergyImageComponent(pStack));
    }

    @Override
    public boolean canZoom(GunData data, @Nullable Entity shooter) {
        return false;
    }
}
