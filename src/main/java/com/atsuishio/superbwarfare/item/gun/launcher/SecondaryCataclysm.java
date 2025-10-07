package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.model.item.SecondaryCataclysmModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.BatteryItem;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
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
import java.util.function.Supplier;

public class SecondaryCataclysm extends GunGeoItem {

    public SecondaryCataclysm() {
        super(new Properties().fireResistant().rarity(ModRarities.LEGENDARY));
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
        var meleeController = new AnimationController<>(this, "meleeController", 0, this::meleePredicate);
        data.add(meleeController);
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
    public ResourceLocation getGunIcon(GunData data) {
        return Mod.loc("textures/gun_icon/secondary_cataclysm_icon.png");
    }

    @Override
    public boolean shootBullet(@NotNull ShootParameters parameters) {
        var data = parameters.data();
        var level = parameters.level();
        var shootPosition = parameters.shootPosition();
        var shootDirection = parameters.shootDirection();
        var zoom = parameters.zoom();

        var stack = data.stack;

        var hasEnoughEnergy = stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.getEnergyStored() >= 3000)
                .orElse(false);

        boolean isChargedFire = zoom && hasEnoughEnergy;

        if (isChargedFire) {
            data.setTempProperty(GunProp.DAMAGE, (pm, d, v) -> v * 1.25);
            data.setTempProperty(GunProp.VELOCITY, (pm, d, v) -> v * 4);
        }

        if (!super.shootBullet(parameters)) return false;

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
                float soundRadius = data.get(GunProp.SOUND_RADIUS).floatValue();

                shooter.playSound(ModSounds.SECONDARY_CATACLYSM_FIRE_3P_CHARGE.get(), soundRadius * 0.4f, 1f);
                shooter.playSound(ModSounds.SECONDARY_CATACLYSM_FAR_CHARGE.get(), soundRadius * 0.7f, 1f);
                shooter.playSound(ModSounds.SECONDARY_CATACLYSM_VERYFAR_CHARGE.get(), soundRadius, 1f);
            } else {
                super.playFireSounds(data, shooter, zoom);
            }
        });
    }

}