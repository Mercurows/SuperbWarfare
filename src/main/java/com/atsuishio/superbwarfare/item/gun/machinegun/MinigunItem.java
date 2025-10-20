package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.client.renderer.gun.MinigunItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class MinigunItem extends GunGeoItem {

    public MinigunItem() {
        super(new Item.Properties().rarity(ModRarities.LEGENDARY));
    }

    @Override
    public int getCustomRPM(GunData data) {
        return data.data().getInt("CustomRPM");
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return MinigunItemRenderer::new;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @OnlyIn(Dist.CLIENT)
    public IClientItemExtensions getClientExtensions() {
        return new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = MinigunItem.this.getRenderer().get();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }

            private static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create("Minigun", false, (model, entity, arm) -> {
                if (arm != HumanoidArm.LEFT) {
                    model.rightArm.xRot = 22.5f * Mth.DEG_TO_RAD + model.head.xRot;
                    model.rightArm.yRot = model.head.yRot;
                    model.leftArm.xRot = Mth.clamp(-45f * Mth.DEG_TO_RAD + model.head.xRot, -67.5f * Mth.DEG_TO_RAD, 0f * Mth.DEG_TO_RAD);
                    model.leftArm.yRot = Mth.clamp(45f * Mth.DEG_TO_RAD + model.head.yRot, 45f * Mth.DEG_TO_RAD, 80f * Mth.DEG_TO_RAD);
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