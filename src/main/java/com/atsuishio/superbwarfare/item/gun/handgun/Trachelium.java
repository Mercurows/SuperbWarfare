package com.atsuishio.superbwarfare.item.gun.handgun;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.ClickHandler;
import com.atsuishio.superbwarfare.client.PoseTool;
import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.client.renderer.gun.TracheliumItemRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class Trachelium extends GunItem {

    public Trachelium() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public Set<SoundEvent> getReloadSound() {
        return Set.of(ModSounds.TRACHELIUM_RELOAD_EMPTY.get());
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new TracheliumItemRenderer();
                }
                return this.renderer;
            }

            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entityLiving, InteractionHand hand, ItemStack stack) {
                return PoseTool.pose(entityLiving, hand, stack);
            }
        });
    }

    private PlayState fireAnimPredicate(AnimationState<Trachelium> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));

        boolean stock = GunData.from(stack).attachment.get(AttachmentType.STOCK) == 2;
        boolean grip = GunData.from(stack).attachment.get(AttachmentType.GRIP) > 0 || GunData.from(stack).attachment.get(AttachmentType.SCOPE) > 0;

        if (ClientEventHandler.firePosTimer > 0 && ClientEventHandler.firePosTimer < 1.7) {
            if (stock) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire_stock_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire_stock"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.fire"));
                }
            }
        }

        if (stock) {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock"));
            }
        } else {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));
            }
        }
    }

    private PlayState idlePredicate(AnimationState<Trachelium> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));

        boolean stock = GunData.from(stack).attachment.get(AttachmentType.STOCK) == 2;
        boolean grip = GunData.from(stack).attachment.get(AttachmentType.GRIP) > 0 || GunData.from(stack).attachment.get(AttachmentType.SCOPE) > 0;

        if (GunData.from(stack).bolt.actionTimer.get() > 0) {
            if (stock) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action_stock_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action_stock"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.action"));
                }
            }
        }

        if (GunData.from(stack).reload.empty()) {
            if (stock) {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload_stock_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload_stock"));
                }
            } else {
                if (grip) {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload_grip"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.reload"));
                }
            }
        }

        if (player.isSprinting() && player.onGround() && ClientEventHandler.cantSprint == 0 && ClientEventHandler.drawTime < 0.01) {
            if (stock) {
                if (grip) {
                    if (ClientEventHandler.tacticalSprint) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast_stock"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_stock_grip"));
                    }
                } else {
                    if (ClientEventHandler.tacticalSprint) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast_stock"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_stock"));
                    }
                }
            } else {
                if (grip) {
                    if (ClientEventHandler.tacticalSprint) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_grip"));
                    }
                } else {
                    if (ClientEventHandler.tacticalSprint) {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run_fast"));
                    } else {
                        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.run"));
                    }
                }
            }
        }

        if (stock) {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_stock"));
            }
        } else {
            if (grip) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle_grip"));
            } else {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));
            }
        }
    }

    private PlayState editPredicate(AnimationState<Trachelium> event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return PlayState.STOP;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem)) return PlayState.STOP;
        if (event.getData(DataTickets.ITEM_RENDER_PERSPECTIVE) != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));

        if (ClickHandler.isEditing) {
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.trachelium.edit"));
        }

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.trachelium.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        var fireAnimController = new AnimationController<>(this, "fireAnimController", 0, this::fireAnimPredicate);
        data.add(fireAnimController);
        var idlePredicate = new AnimationController<>(this, "idlePredicate", 3, this::idlePredicate);
        data.add(idlePredicate);
        var editController = new AnimationController<>(this, "editController", 1, this::editPredicate);
        data.add(editController);
    }

    @Override
    @ParametersAreNonnullByDefault
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.empty());
        list.add(Component.translatable("des.superbwarfare.trachelium_1").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
        list.add(Component.translatable("des.superbwarfare.trachelium_2").withStyle(ChatFormatting.GRAY));

        TooltipTool.addHideText(list, Component.empty());
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_3").withStyle(ChatFormatting.WHITE));
        TooltipTool.addHideText(list, Component.translatable("des.superbwarfare.trachelium_4").withStyle(Style.EMPTY.withColor(0xF4F0FF)));
    }

    @Override
    @ParametersAreNonnullByDefault
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        int stockType = GunData.from(stack).attachment.get(AttachmentType.STOCK);
        CompoundTag tags = GunData.from(stack).attachment();

        if (stockType == 1) {
            tags.putInt("Stock", 2);
        }

        if (scopeType == 3) {
            tags.putInt("Scope", 0);
        }
    }

    @Override
    public boolean canSwitchScope(ItemStack stack) {
        return GunData.from(stack).attachment.get(AttachmentType.SCOPE) == 2;
    }

    private boolean useSpecialAttributes(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        int gripType = GunData.from(stack).attachment.get(AttachmentType.GRIP);
        return scopeType > 0 || gripType > 0;
    }

    @Override
    public double getCustomDamage(ItemStack stack) {
        if (useSpecialAttributes(stack)) {
            return 2;
        }
        return super.getCustomDamage(stack);
    }

    @Override
    public double getCustomZoom(ItemStack stack) {
        int scopeType = GunData.from(stack).attachment.get(AttachmentType.SCOPE);
        return scopeType == 2 ? (stack.getOrCreateTag().getBoolean("ScopeAlt") ? 0 : 2.75) : 0;
    }

    @Override
    public double getCustomVelocity(ItemStack stack) {
        if (useSpecialAttributes(stack)) {
            return 15;
        }
        return super.getCustomVelocity(stack);
    }

    @Override
    public double getCustomHeadshot(ItemStack stack) {
        if (useSpecialAttributes(stack)) {
            return 0.5;
        }
        return super.getCustomHeadshot(stack);
    }

    @Override
    public double getCustomBypassArmor(ItemStack stack) {
        if (useSpecialAttributes(stack)) {
            return 0.1;
        }
        return super.getCustomBypassArmor(stack);
    }

    @Override
    public ResourceLocation getGunIcon() {
        return Mod.loc("textures/gun_icon/trachelium_icon.png");
    }

    @Override
    public String getGunDisplayName() {
        return "TRACHELIUM";
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
    public boolean hasCustomScope(ItemStack stack) {
        return true;
    }

    @Override
    public boolean hasCustomStock(ItemStack stack) {
        return true;
    }
}