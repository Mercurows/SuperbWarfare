package com.atsuishio.superbwarfare.item.gun.machinegun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.renderer.item.MinigunItemRenderer;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModEnumExtensions;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.item.gun.data.GunData;
import com.atsuishio.superbwarfare.perk.Perk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Supplier;

public class MinigunItem extends GunItem implements GeoItem {
    @Override
    public int getCustomRPM(ItemStack stack) {
        return GunData.from(stack).data().getInt("CustomRPM");
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public static ItemDisplayContext transformType;

    public MinigunItem() {
        super(new Properties().stacksTo(1).rarity(ModEnumExtensions.getLegendary()));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        return data.heat.get() != 0;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        return Math.round((float) data.heat.get() * 13.0F / 100F);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var data = GunData.from(stack);
        double f = 1 - data.heat.get() / 100.0F;
        return Mth.hsvToRgb((float) f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public Supplier<GeoItemRenderer<? extends Item>> getRenderer() {
        return MinigunItemRenderer::new;
    }

    public void getTransformType(ItemDisplayContext type) {
        transformType = type;
    }

    private PlayState idlePredicate(AnimationState<MinigunItem> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;

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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/minigun_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "M134 MINIGUN";
    }

    @Override
    public boolean canApplyPerk(Perk perk) {
        return switch (perk.type) {
            case AMMO -> perk != ModPerks.MICRO_MISSILE.get() && perk != ModPerks.LONGER_WIRE.get();
            case FUNCTIONAL -> perk == ModPerks.FIELD_DOCTOR.get() || perk == ModPerks.INTELLIGENT_CHIP.get();
            case DAMAGE -> perk == ModPerks.MONSTER_HUNTER.get() || perk == ModPerks.KILLING_TALLY.get();
        };
    }

    @Override
    public boolean isAutoWeapon(ItemStack stack) {
        return true;
    }

}