package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.MinigunItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.RarityTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.function.Consumer;

public class MinigunItem extends GunItem {

    public MinigunItem() {
        super(new Item.Properties().stacksTo(1).rarity(RarityTool.LEGENDARY));
    }

    @Override
    public int getCustomRPM(ItemStack stack) {
        return GunData.from(stack).data().getInt("CustomRPM");
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new MinigunItemRenderer();
                }
                return renderer;
            }

            private static final HumanoidModel.ArmPose MinigunPose = HumanoidModel.ArmPose.create("Minigun", false, (model, entity, arm) -> {
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = -22.5f * Mth.DEG_TO_RAD + model.head.xRot;
                    model.rightArm.yRot = -10f * Mth.DEG_TO_RAD;
                    model.leftArm.xRot = -45f * Mth.DEG_TO_RAD + model.head.xRot;
                    model.leftArm.yRot = 40f * Mth.DEG_TO_RAD;
                }
            });

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack itemStack) {
                if (!itemStack.isEmpty()) {
                    if (entityLiving.getUsedItemHand() == hand) {
                        return MinigunPose;
                    }
                }
                return HumanoidModel.ArmPose.EMPTY;
            }
        });
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