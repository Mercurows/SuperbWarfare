package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.renderer.gun.Ql1031ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.item.gun.GunGeoItem;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
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

public class Ql1031Item extends GunGeoItem {

    public Ql1031Item() {
        super(new Properties().rarity(ModRarities.VIRTUAL));
    }

    @Override
    public Supplier<? extends GeoItemRenderer<? extends Item>> getRenderer() {
        return Ql1031ItemRenderer::new;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.empty());
        list.add(Component.translatable("des.superbwarfare.ql_1031_1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

        TooltipTool.addHideText(list, Component.empty());
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.ql_1031_2").withStyle(Style.EMPTY.withColor(0xFFECE7)));
    }

    @OnlyIn(Dist.CLIENT)
    private PlayState editPredicate(AnimationState<Ql1031Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ql_1031.idle"));

        if (ClientEventHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.ql_1031.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ql_1031.idle"));
    }

    @OnlyIn(Dist.CLIENT)
    private PlayState chargePredicate(AnimationState<Ql1031Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem gunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ql_1031.idle"));

        var data = GunData.from(stack);

        if (ClientEventHandler.holdingFireKey && gunItem.canShoot(data, player) && data.selectedFireModeInfo().name.equals("Hold")) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ql_1031.charge"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ql_1031.idle"));
    }

    @Override
    public void afterShoot(@NotNull ShootParameters parameters) {
        super.afterShoot(parameters);
        var data = parameters.data;
        var level = parameters.level;
        var shootPosition = parameters.shootPosition;
        var shootDirection = parameters.shootDirection;

        if (data.selectedFireModeInfo().name.equals("Hold")) {
            for (int i = 0;i < 40;i += 2) {
                Vec3 pos = shootPosition.add(shootDirection.normalize().scale(1 + 0.5 * i + 0.05 * i * i));
                ParticleTool.sendParticle(level, ParticleTypes.CHERRY_LEAVES, pos.x, pos.y - 0.12, pos.z, 1, 0.04, 0.04, 0.04, 1, false);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        var chargeController = new AnimationController<>(this, "chargeController", 1, this::chargePredicate);
        data.add(editController);
        data.add(chargeController);
    }

    @Override
    public boolean canSwitchScope(GunData data) {
        return data.attachment.get(AttachmentType.SCOPE) == 2;
    }

    @Override
    public double getCustomZoom(GunData data) {
        int scopeType = data.attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> data.stack.getOrCreateTag().getBoolean("ScopeAlt") ? 0 : 2.75;
            case 3 -> GunsTool.getGunDoubleTag(data.stack, "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public boolean canAdjustZoom(GunData data) {
        return data.attachment.get(AttachmentType.SCOPE) == 3;
    }

    @Override
    public boolean hasBulletInBarrel(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomBarrel(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomGrip(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomScope(GunData data) {
        return true;
    }

    @Override
    public boolean hasCustomStock(GunData data) {
        return true;
    }

    @Override
    public boolean canEditAttachments(GunData data) {
        return true;
    }
}
