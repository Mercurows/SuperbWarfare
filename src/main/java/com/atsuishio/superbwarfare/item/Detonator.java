package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.projectile.C4Entity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.stream.StreamSupport;

public class Detonator extends Item {

    public Detonator() {
        super(new Properties().stacksTo(1));
    }

    public static List<Entity> getC4(Player player, Level level) {
        return StreamSupport.stream(EntityFindUtil.getEntities(level).getAll().spliterator(), false)
                .filter(e -> e instanceof C4Entity c4 && c4.getOwner() == player)
                .toList();
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(stack.getItem(), 10);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.C4_DETONATOR_CLICK.get(), SoundSource.PLAYERS, 1, 1);
        }

        this.releaseUsing(stack, player.level(), player, 1);

        List<Entity> entities = getC4(player, player.level());
        for (var e : entities) {
            if (e instanceof C4Entity c4 && c4.getEntityData().get(C4Entity.IS_CONTROLLABLE)) {
                c4.explode();
            }
        }

        // 自爆
        CustomExplosion explosion = new CustomExplosion(world, null,
                ModDamageTypes.causeProjectileBoomDamage(world.registryAccess(), player, player), ExplosionConfig.C4_EXPLOSION_DAMAGE.get(),
                player.position().x, player.position().y, player.position().z, ExplosionConfig.C4_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(1);
        explosion.explode();
        net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world, explosion);
        ParticleTool.spawnHugeExplosionParticles(world, player.position());
        explosion.finalizeExplosion(false);

        return InteractionResultHolder.consume(stack);
    }
}
