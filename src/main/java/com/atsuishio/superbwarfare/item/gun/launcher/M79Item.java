package com.atsuishio.superbwarfare.item.gun.launcher;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.renderer.gun.M79ItemRenderer;
import com.atsuishio.superbwarfare.client.tooltip.component.LauncherImageComponent;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class M79Item extends GunItem {

    public M79Item() {
        super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.RARE));
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.M_79_RELOAD_EMPTY.get());
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new M79ItemRenderer();
                }
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return PoseTool.pose(entityLiving, hand, stack);
            }
        });
    }

    private PlayState idlePredicate(AnimationState<M79Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_79.idle"));

        var data = GunData.from(stack);
        if (data.reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_79.reload"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_79.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_79.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_79.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m_79_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M79 LAUNCHER";
    }

    @Override
    public @NotNull Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack pStack) {
        return Optional.of(new LauncherImageComponent(pStack));
    }

    @Override
    public String getAmmoDisplayName(GunData data) {
        return "40mm Grenade";
    }

    @Override
    public boolean shootBullet(Player player, GunData data, double spread, boolean zoom) {
        if (data.reloading()) return false;
        if (!super.shootBullet(player, data, spread, zoom)) return false;

        if (player.level() instanceof ServerLevel serverLevel) {
            ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, player.getX() + 1.8 * player.getLookAngle().x,
                    player.getY() + player.getBbHeight() - 0.1 + 1.8 * player.getLookAngle().y,
                    player.getZ() + 1.8 * player.getLookAngle().z,
                    4, 0.1, 0.1, 0.1, 0.002, true);
        }
        return true;
    }
}