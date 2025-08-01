package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.energy.ItemEnergyProvider;
import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.model.item.SecondaryCataclysmModel;
import com.atsuishio.superbwarfare.client.tooltip.component.SecondaryCataclysmImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.GunGrenadeEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.perk.AmmoPerk;
import com.atsuishio.superbwarfare.perk.Perk;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.RarityTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.atsuishio.superbwarfare.tools.EntityFindUtil.findEntity;

public class SecondaryCataclysm extends GunItem {

    private final Supplier<Integer> energyCapacity = () -> 24000;

    public SecondaryCataclysm() {
        super(new Properties().stacksTo(1).fireResistant().rarity(RarityTool.LEGENDARY));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack pStack) {
        if (!pStack.getCapability(ForgeCapabilities.ENERGY).isPresent()) {
            return false;
        }

        AtomicInteger energy = new AtomicInteger(0);
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> energy.set(e.getEnergyStored())
        );
        return energy.get() != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack pStack) {
        AtomicInteger energy = new AtomicInteger(0);
        pStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(
                e -> energy.set(e.getEnergyStored())
        );

        return Math.round((float) energy.get() * 13.0F / 24000F);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, CompoundTag tag) {
        return new ItemEnergyProvider(stack, energyCapacity.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.empty());
        list.add(Component.translatable("des.superbwarfare.secondary_cataclysm_1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

        TooltipTool.addHideText(list, Component.empty());
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.secondary_cataclysm_2").withStyle(Style.EMPTY.withColor(0x68B9F6)));
    }

    @Override
    public int getBarColor(@NotNull ItemStack pStack) {
        return 0x95E9FF;
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(SecondaryCataclysmModel::new, 0, 0, 1.0375, 0.6);
    }

    private PlayState reloadAnimPredicate(AnimationState<SecondaryCataclysm> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.idle"));

        var data = GunData.from(stack);

        if (data.reload.stage() == 1 && data.reload.prepareLoadTimer.get() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.secondary_cataclysm.prepare"));
        }

        if (data.loadIndex.get() == 0 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.secondary_cataclysm.iterativeload"));
        }

        if (data.loadIndex.get() == 1 && data.reload.stage() == 2) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.secondary_cataclysm.iterativeload2"));
        }

        if (data.reload.stage() == 3) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.secondary_cataclysm.finish"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.idle"));
    }

    private PlayState idlePredicate(AnimationState<SecondaryCataclysm> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.idle"));

        var data = GunData.from(stack);

        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !(data.tag().getBoolean("is_empty_reloading"))
                && data.reload.stage() != 1
                && data.reload.stage() != 2
                && data.reload.stage() != 3
                && ClientEventHandler.drawTime < 0.01
                && ClientEventHandler.gunMelee == 0
                && !data.reloading()) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.idle"));
    }

    private PlayState meleePredicate(AnimationState<SecondaryCataclysm> event) {
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.idle"));

        if (ClientEventHandler.gunMelee > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.secondary_cataclysm.hit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.secondary_cataclysm.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var reloadAnimController = new AnimationController<>(this, "reloadAnimController", 1, this::reloadAnimPredicate);
        data.add(reloadAnimController);
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
        var meleeController = new AnimationController<>(this, "meleeController", 0, this::meleePredicate);
        data.add(meleeController);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (entity instanceof Player player) {
            for (var cell : player.getInventory().items) {
                if (cell.is(ModItems.CELL.get())) {
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
    public ResourceLocation getGunIcon(ItemStack stack) {
        return Mod.loc("textures/gun_icon/secondary_cataclysm_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "SECONDARY CATACLYSM";
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new SecondaryCataclysmImageComponent(pStack));
    }

    @Override
    public String getAmmoDisplayName(GunData data, ItemStack stack) {
        return "40mm Grenade";
    }

    // TODO 这玩意能提取吗
    @Override
    public boolean shootBullet(
            @Nullable Entity shooter,
            @NotNull ServerLevel level,
            @NotNull Vec3 shootPosition,
            @NotNull Vec3 shootDirection,
            @NotNull GunData data,
            double spread,
            boolean zoom,
            @Nullable UUID uuid
    ) {
        if (data.reloading()) return false;
        var stack = data.stack;

        var hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.getEnergyStored() >= 3000)
                .orElse(false);

        boolean isChargedFire = zoom && hasEnoughEnergy;

        GunGrenadeEntity gunGrenadeEntity = new GunGrenadeEntity(shooter, level,
                (float) data.damage(),
                (float) data.explosionDamage(),
                (float) data.explosionRadius()
        );

        float velocity = (float) data.velocity();

        for (Perk.Type type : Perk.Type.values()) {
            var instance = data.perk.getInstance(type);
            if (instance != null) {
                instance.perk().modifyProjectile(data, instance, gunGrenadeEntity);
                if (instance.perk() instanceof AmmoPerk ammoPerk) {
                    velocity = (float) ammoPerk.getModifiedVelocity(data, instance);
                }
            }
        }

        gunGrenadeEntity.charged(isChargedFire);

        var x = shootDirection.x;
        var y = shootDirection.y + 0.001f;
        var z = shootDirection.z;

        if (uuid != null && zoom && shooter != null && !shooter.isShiftKeyDown()) {
            Entity target = findEntity(shooter.level(), String.valueOf(uuid));
            var gunData = GunData.from(stack);
            int intelligentChipLevel = gunData.perk.getLevel(ModPerks.INTELLIGENT_CHIP);
            if (intelligentChipLevel > 0 && target != null) {
                Vec3 targetVec = target.getEyePosition();
                Vec3 playerVec = shooter.getEyePosition();
                var hasGravity = gunData.perk.getLevel(ModPerks.MICRO_MISSILE) <= 0;
                Vec3 toVec = RangeTool.calculateFiringSolution(playerVec, targetVec, Vec3.ZERO, (isChargedFire ? 4 : 1) * velocity, hasGravity ? 0.05 : 0);
                x = toVec.x;
                y = toVec.y;
                z = toVec.z;
            }
        }

        gunGrenadeEntity.setPos(shootPosition.x, shootPosition.y - 0.1, shootPosition.z);
        gunGrenadeEntity.shoot(x, y, z, (isChargedFire ? 4 : 1) * velocity,
                (float) (zoom ? 0.1 : spread));
        level.addFreshEntity(gunGrenadeEntity);

        ParticleTool.sendParticle(level, ParticleTypes.CLOUD, shootPosition.x + 1.8 * shootDirection.x,
                shootPosition.y - 0.35 + 1.8 * shootDirection.y,
                shootPosition.z + 1.8 * shootDirection.z,
                4, 0.1, 0.1, 0.1, 0.002, true);

        if (isChargedFire) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> energy.extractEnergy(3000, false));
        }

        return true;
    }

    @Override
    public void playFireSounds(GunData data, Entity shooter, boolean zoom) {
        data.stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap -> {
            if (cap.getEnergyStored() > 3000 && zoom) {
                float soundRadius = (float) data.soundRadius();

                shooter.playSound(ModSounds.SECONDARY_CATACLYSM_FIRE_3P_CHARGE.get(), soundRadius * 0.4f, 1f);
                shooter.playSound(ModSounds.SECONDARY_CATACLYSM_FAR_CHARGE.get(), soundRadius * 0.7f, 1f);
                shooter.playSound(ModSounds.SECONDARY_CATACLYSM_VERYFAR_CHARGE.get(), soundRadius, 1f);
            } else {
                super.playFireSounds(data, shooter, zoom);
            }
        });
    }

}