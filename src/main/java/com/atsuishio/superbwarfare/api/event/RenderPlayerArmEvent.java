package com.atsuishio.superbwarfare.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import software.bernie.geckolib.cache.object.GeoBone;

@Cancelable
@ApiStatus.AvailableSince("0.8.7.1")
public class RenderPlayerArmEvent extends Event {
    private final LocalPlayer localPlayer;
    private final ItemDisplayContext transformType;
    private final PoseStack stack;
    private final HumanoidArm arm;
    private final GeoBone bone;
    private final MultiBufferSource currentBuffer;
    private final RenderType renderType;
    private final int packedLightIn;
    private final boolean useOldHandRender;

    public RenderPlayerArmEvent(LocalPlayer localPlayer, ItemDisplayContext transformType, PoseStack stack,
                                HumanoidArm arm, GeoBone bone, MultiBufferSource currentBuffer,
                                RenderType renderType, int packedLightIn, boolean useOldHandRender) {
        this.localPlayer = localPlayer;
        this.transformType = transformType;
        this.stack = stack;
        this.arm = arm;
        this.bone = bone;
        this.currentBuffer = currentBuffer;
        this.renderType = renderType;
        this.packedLightIn = packedLightIn;
        this.useOldHandRender = useOldHandRender;
    }

    public LocalPlayer getLocalPlayer() {
        return localPlayer;
    }

    public ItemDisplayContext getTransformType() {
        return transformType;
    }

    public PoseStack getStack() {
        return stack;
    }

    public HumanoidArm getArm() {
        return arm;
    }

    public GeoBone getBone() {
        return bone;
    }

    public MultiBufferSource getCurrentBuffer() {
        return currentBuffer;
    }

    public RenderType getRenderType() {
        return renderType;
    }

    public int getPackedLightIn() {
        return packedLightIn;
    }

    public boolean isUseOldHandRender() {
        return useOldHandRender;
    }
}
