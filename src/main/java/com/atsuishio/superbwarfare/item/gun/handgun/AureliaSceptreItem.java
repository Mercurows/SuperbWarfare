package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.model.item.AureliaSceptreItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
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

public class AureliaSceptreItem extends GunGeoItem {

    public AureliaSceptreItem() {
        super(new Properties().rarity(ModRarities.LEGENDARY));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(AureliaSceptreItemModel::new);
    }

    private PlayState idlePredicate(AnimationState<AureliaSceptreItem> event) {
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

    @OnlyIn(Dist.CLIENT)
    private PlayState firePredicate(AnimationState<AureliaSceptreItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));

        var data = GunData.from(stack);

        if (ClientEventHandler.holdFire && gunItem.canShoot(data, player)) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.fire"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    private PlayState meleePredicate(AnimationState<AureliaSceptreItem> event) {
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));

        if (ClientEventHandler.gunMelee > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.aurelia_sceptre.hit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.aurelia_sceptre.idle"));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.empty());
        list.add(Component.translatable("des.superbwarfare.aurelia_sceptre_1").withStyle(ChatFormatting.GRAY));
        list.add(Component.translatable("des.superbwarfare.aurelia_sceptre_2").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

        TooltipTool.addHideText(list, Component.empty());
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.aurelia_sceptre_3").withStyle(Style.EMPTY.withColor(0xABCDEF)));
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

    @OnlyIn(Dist.CLIENT)
    public IClientItemExtensions getClientExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = AureliaSceptreItem.this.getRenderer().get();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            private static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create("AureliaSceptreItem", false, (model, entity, arm) -> {
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
}