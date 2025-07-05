package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.M2HBItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class M2HBItem extends GunItem {

    public M2HBItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(M2HBItemModel::new, 0, 0.1, 2.95, 1.2);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        if (!stack.isEmpty()) {
            if (entityLiving.getUsedItemHand() == hand) {
                return Pose.POSE;
            }
        }
        return HumanoidModel.ArmPose.EMPTY;
    }

    private PlayState fireAnimPredicate(AnimationState<M2HBItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_2_hb.idle"));

        if (ClientEventHandler.firePosTimer > 0 && ClientEventHandler.firePosTimer < 0.45) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_2_hb.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_2_hb.idle"));
    }

    private PlayState idlePredicate(AnimationState<M2HBItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_2_hb.idle"));

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_2_hb.reload_empty"));
        }

        if (GunData.from(stack).reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.m_2_hb.reload_normal"));
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_2_hb.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_2_hb.run"));
            }
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.m_2_hb.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idleController = new AnimationController<>(this, "idleController", 4, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.M_2_HB_RELOAD_EMPTY.get(), ModSounds.M_2_HB_RELOAD_NORMAL.get());
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        var data = GunData.from(stack);
        if (data.draw.get()) {
            data.draw.set(false);
            if (data.ammo.get() <= 5) {
                data.hideBulletChain.set(true);
            }
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/m_2_hb_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M2HB";
    }

    @Override
    public boolean isOpenBolt(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasBulletInBarrel(ItemStack stack) {
        return true;
    }

    @Override
    public void beforeShoot(GunData data, Player player, double spread, boolean zoom) {
        super.beforeShoot(data, player, spread, zoom);

        if (data.ammo.get() <= 5) {
            data.hideBulletChain.set(true);
        }
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);

        behaviors.put(70, data -> data.hideBulletChain.reset());
    }

    @OnlyIn(Dist.CLIENT)
    static class Pose {

        private static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create("M2HB", false, (model, entity, arm) -> {
            if (arm != HumanoidArm.LEFT) {
                model.rightArm.xRot = 45f * Mth.DEG_TO_RAD + model.head.xRot;
                model.rightArm.yRot = model.head.yRot;
                model.leftArm.xRot = Mth.clamp(-45f * Mth.DEG_TO_RAD + model.head.xRot, -67.5f * Mth.DEG_TO_RAD, 0f * Mth.DEG_TO_RAD);
                model.leftArm.yRot = Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD);
            }
        });
    }
}