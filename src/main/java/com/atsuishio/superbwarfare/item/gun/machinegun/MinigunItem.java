package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.gun.MinigunItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Supplier;

public class MinigunItem extends GunGeoItem {

    public MinigunItem() {
        super(new Properties().rarity(ModRarities.LEGENDARY));
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
    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
        if (!stack.isEmpty()) {
            if (entityLiving.getUsedItemHand() == hand) {
                return ModEnumExtensions.Client.getMinigunPose();
            }
        }
        return HumanoidModel.ArmPose.EMPTY;
    }

    @Override
    public @NotNull ResourceLocation getGunIcon(@NotNull GunData data) {
        return Mod.loc("textures/gun_icon/minigun_icon.png");
    }
}