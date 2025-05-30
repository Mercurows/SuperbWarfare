package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.renderer.gun.AureliaSceptreRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AureliaSceptre extends GunItem {

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
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));

        if (player.isSprinting() && player.onGround()
                && ClientEventHandler.cantSprint == 0
                && !(GunData.from(stack).reload.normal() || GunData.from(stack).reload.empty()) && ClientEventHandler.drawTime < 0.01 && ClientEventHandler.gunMelee == 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.run"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    private PlayState firePredicate(AnimationState<AureliaSceptre> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));

        var data = GunData.from(stack);

        if (ClientEventHandler.holdFire && gunItem.canShoot(data) && !data.overHeat.get()) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    private PlayState meleePredicate(AnimationState<AureliaSceptre> event) {
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));

        if (ClientEventHandler.gunMelee > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.aurelia_sceptre.hit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("des.superbwarfare.aurelia_sceptre_1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("des.superbwarfare.aurelia_sceptre_2").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

        TooltipTool.addHideText(tooltipComponents, Component.empty());
        TooltipTool.addHideText(tooltipComponents, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(tooltipComponents, Component.translatable("des.superbwarfare.aurelia_sceptre_3").withStyle(Style.EMPTY.withColor(0xABCDEF)));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 6, this::idlePredicate);
        data.add(idleController);
        var fireController = new AnimationController<>(this, "fireController", 3, this::firePredicate);
        data.add(fireController);
        var meleeController = new AnimationController<>(this, "meleeController", 0, this::meleePredicate);
        data.add(meleeController);
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