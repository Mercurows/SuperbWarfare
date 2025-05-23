package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.AureliaSceptreRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AureliaSceptre extends GunItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AureliaSceptre() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return AureliaSceptreRenderer::new;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        if (!stack.isEmpty()) {
            if (entityLiving.getUsedItemHand() == hand) {
                return ModEnumExtensions.Client.getAureliaSceptrePose();
            }
        }
        return HumanoidModel.ArmPose.EMPTY;
    }

    private PlayState idlePredicate(AnimationState<AureliaSceptre> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

        if (ClientEventHandler.firePosTimer > 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.fire"));
        }

        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !(GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty()) && ClientEventHandler.drawTime < 0.01) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.run"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 6, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/aurelia_sceptre_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "AURELIA SCEPTRE";
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);
    }
}