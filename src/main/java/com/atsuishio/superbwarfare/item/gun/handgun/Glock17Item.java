package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.GunRendererBuilder;
import com.atsuishio.superbwarfare.client.model.item.Glock17ItemModel;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Glock17Item extends GunItem {

    public Glock17Item() {
        super(new Properties().rarity(Rarity.COMMON));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return GunRendererBuilder.simple(Glock17ItemModel::new, 0, 0, 0.3022, 0.3);
    }

    private PlayState fireAnimPredicate(AnimationState<Glock17Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.glock_17.idle"));

        if (GunData.from(stack).reload.empty()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.glock_17.reload_empty"));
        }

        if (GunData.from(stack).reload.normal()) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.glock_17.reload_normal"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.glock_17.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.GLOCK_17_RELOAD_EMPTY.get(), ModSounds.GLOCK_17_RELOAD_NORMAL.get());
    }

    @Override
    public ResourceLocation getGunIcon(ItemStack stack) {
        return Mod.loc("textures/gun_icon/glock_17_icon.png");
    }

    @Override
    public boolean isOpenBolt(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasBulletInBarrel(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);

        behaviors.put(9, data -> data.holdOpen.set(false));
    }
}