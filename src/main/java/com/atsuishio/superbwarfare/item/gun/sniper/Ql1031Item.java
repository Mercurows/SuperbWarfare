package com.atsuishio.superbwarfare.item.gun.sniper;

import com.atsuishio.superbwarfare.client.TooltipTool;
import com.atsuishio.superbwarfare.data.gun.ShootParameters;
import com.atsuishio.superbwarfare.init.ModRarities;
import com.atsuishio.superbwarfare.init.RegistryName;
import com.atsuishio.superbwarfare.item.gun.GeoGunItemV2;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@RegistryName("ql_1031")
public class Ql1031Item extends GeoGunItemV2 {

    public Ql1031Item() {
        super(new Properties().rarity(ModRarities.VIRTUAL));
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

    @Override
    public void afterShoot(@NotNull ShootParameters parameters) {
        super.afterShoot(parameters);
        var data = parameters.data;
        var level = parameters.level;
        var shootPosition = parameters.shootPosition;
        var shootDirection = parameters.shootDirection;

        if (data.selectedFireModeInfo().name.equals("HOLD")) {
            for (int i = 0;i < 40;i += 2) {
                Vec3 pos = shootPosition.add(shootDirection.normalize().scale(1 + 0.5 * i + 0.05 * i * i));
                ParticleTool.sendParticle(level, ParticleTypes.CHERRY_LEAVES, pos.x, pos.y - 0.12, pos.z, 1, 0.04, 0.04, 0.04, 1, false);
            }
        }
    }
}
