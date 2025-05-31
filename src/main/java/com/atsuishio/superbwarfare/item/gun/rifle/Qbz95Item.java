package com.atsuishio.superbwarfare.item.gun.rifle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.renderer.gun.Qbz95ItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.atsuishio.superbwarfare.tools.GunsTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Qbz95Item extends GunItem {

    public Qbz95Item() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new Qbz95ItemRenderer();
                }
                return renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return PoseTool.pose(entityLiving, hand, stack);
            }
        });
    }

    private PlayState idlePredicate(AnimationState<Qbz95Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.idle"));

        boolean drum = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE) == 2;
        boolean grip = GunData.from(stack).attachment.get(AttachmentType.GRIP) == 1 || GunData.from(stack).attachment.get(AttachmentType.GRIP) == 2;

        if (GunData.from(stack).reload.empty()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_empty_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_empty_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_empty_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_empty"));
                }
            }
        }

        if (GunData.from(stack).reload.normal()) {
            if (drum) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_normal_drum_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_normal_drum"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_normal_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.reload_normal"));
                }
            }
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (ClientEventHandler.tacticalSprint) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.run_fast"));
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.run_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.run"));
                }
            }
        }

        if (grip) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.idle_grip"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.idle"));
        }
    }

    private PlayState editPredicate(AnimationState<Qbz95Item> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.idle"));

        if (ClickHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.qbz_95.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.qbz_95.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var idleController = new AnimationController<>(this, "idleController", 3, this::idlePredicate);
        data.add(idleController);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        if (magType == 1) {
            CompoundTag tag = GunData.from(stack).attachment();
            tag.putInt("Magazine", 2);
        }
    }

    @Override
    public int getCustomMagazine(ItemStack stack) {
        int magType = GunData.from(stack).attachment.get(AttachmentType.MAGAZINE);
        return magType == 2 ? 30 : 0;
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        return switch (scopeType) {
            case 2 -> 2.15;
            case 3 -> GunsTool.getGunDoubleTag(stack, "CustomZoom");
            default -> 0;
        };
    }

    @Override
    public boolean canAdjustZoom(ItemStack stack) {
        return GunData.from(stack).attachment.get(AttachmentType.SCOPE) == 3;
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.QBZ_95_RELOAD_EMPTY.get(), ModSounds.QBZ_95_RELOAD_NORMAL.get());
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/qbz_95_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "QBZ-95-1";
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
    public boolean isCustomizable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomBarrel(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomGrip(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomMagazine(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canEjectShell(ItemStack stack) {
        return true;
    }

    @Override
    public void addReloadTimeBehavior(Map<Integer, Consumer<GunData>> behaviors) {
        super.addReloadTimeBehavior(behaviors);

        behaviors.put(14, data -> data.holdOpen.set(false));
    }
}