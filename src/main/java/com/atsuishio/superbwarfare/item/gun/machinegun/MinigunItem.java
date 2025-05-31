package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.MinigunItemRenderer;
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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class MinigunItem extends GunItem {

    public MinigunItem() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public int getCustomRPM(ItemStack stack) {
        return GunData.from(stack).data().getInt("CustomRPM");
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return MinigunItemRenderer::new;
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        if (!stack.isEmpty()) {
            if (entityLiving.getUsedItemHand() == hand) {
                return ModEnumExtensions.Client.getMinigunPose();
            }
        }
        return HumanoidModel.ArmPose.EMPTY;
    }

    private PlayState idlePredicate(AnimationState<MinigunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.idle"));

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.run_fast"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.run"));
            }
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.minigun.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 6, this::idlePredicate);
        data.add(idleController);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/minigun_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M134 MINIGUN";
    }
}