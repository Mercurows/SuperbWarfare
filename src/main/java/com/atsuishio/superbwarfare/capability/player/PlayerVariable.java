package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModAttachments;
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage;
import com.atsuishio.superbwarfare.tools.AmmoType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;


@EventBusSubscriber(modid = Mod.MODID)
public class PlayerVariable implements INBTSerializable<CompoundTag> {
    private PlayerVariable old = null;

    public boolean zoom = false;
    public boolean holdFire = false;
    public int rifleAmmo = 0;
    public int handgunAmmo = 0;
    public int shotgunAmmo = 0;
    public int sniperAmmo = 0;
    public int heavyAmmo = 0;
    public boolean bowPullHold = false;
    public boolean bowPull = false;
    public boolean playerDoubleJump = false;
    public boolean tacticalSprint = false;
    public int tacticalSprintTime = 600;
    public boolean tacticalSprintExhaustion = false;
    public boolean breath = false;
    public int breathTime = 160;
    public boolean breathExhaustion = false;
    public boolean edit = false;

    public void sync(Entity entity) {
        if (!entity.hasData(ModAttachments.PLAYER_VARIABLE)) return;

        var newVariable = entity.getData(ModAttachments.PLAYER_VARIABLE);
        if (old != null && old.equals(newVariable)) return;

        if (entity instanceof ServerPlayer) {
            PacketDistributor.sendToAllPlayers(new PlayerVariablesSyncMessage(entity.getId(), newVariable.writeToNBT()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(player.getId(), player.getData(ModAttachments.PLAYER_VARIABLE).writeToNBT()));
    }

    public PlayerVariable watch() {
        this.old = this.copy();
        return this;
    }

    public CompoundTag writeToNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("Zoom", zoom);
        nbt.putBoolean("HoldFire", holdFire);

        for (var type : AmmoType.values()) {
            type.set(nbt, type.get(this));
        }

        nbt.putBoolean("BowPullHold", bowPullHold);
        nbt.putBoolean("BowPull", bowPull);
        nbt.putBoolean("DoubleJump", playerDoubleJump);
        nbt.putBoolean("TacticalSprint", tacticalSprint);
        nbt.putInt("TacticalSprintTime", tacticalSprintTime);
        nbt.putBoolean("TacticalSprintExhaustion", tacticalSprintExhaustion);
        nbt.putBoolean("Breath", breath);
        nbt.putInt("BreathTime", breathTime);
        nbt.putBoolean("BreathExhaustion", breathExhaustion);
        nbt.putBoolean("EditMode", edit);

        return nbt;
    }

    public PlayerVariable readFromNBT(CompoundTag tag) {
        zoom = tag.getBoolean("Zoom");
        holdFire = tag.getBoolean("HoldFire");

        for (var type : AmmoType.values()) {
            type.set(this, type.get(tag));
        }

        bowPullHold = tag.getBoolean("BowPullHold");
        bowPull = tag.getBoolean("BowPull");
        playerDoubleJump = tag.getBoolean("DoubleJump");
        tacticalSprint = tag.getBoolean("TacticalSprint");
        tacticalSprintTime = tag.getInt("TacticalSprintTime");
        tacticalSprintExhaustion = tag.getBoolean("TacticalSprintExhaustion");
        breath = tag.getBoolean("Breath");
        breathTime = tag.getInt("BreathTime");
        breathExhaustion = tag.getBoolean("BreathExhaustion");
        edit = tag.getBoolean("EditMode");

        return this;
    }

    public PlayerVariable copy() {
        var clone = new PlayerVariable();

        clone.zoom = this.zoom;
        clone.holdFire = this.holdFire;
        clone.rifleAmmo = this.rifleAmmo;
        clone.handgunAmmo = this.handgunAmmo;
        clone.shotgunAmmo = this.shotgunAmmo;
        clone.sniperAmmo = this.sniperAmmo;
        clone.heavyAmmo = this.heavyAmmo;
        clone.bowPullHold = this.bowPullHold;
        clone.bowPull = this.bowPull;
        clone.playerDoubleJump = this.playerDoubleJump;
        clone.tacticalSprint = this.tacticalSprint;
        clone.tacticalSprintTime = this.tacticalSprintTime;
        clone.tacticalSprintExhaustion = this.tacticalSprintExhaustion;
        clone.breath = this.breath;
        clone.breathTime = this.breathTime;
        clone.breathExhaustion = this.breathExhaustion;
        clone.edit = this.edit;

        return clone;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlayerVariable other)) return false;

        return zoom == other.zoom
                && holdFire == other.holdFire
                && rifleAmmo == other.rifleAmmo
                && handgunAmmo == other.handgunAmmo
                && shotgunAmmo == other.shotgunAmmo
                && sniperAmmo == other.sniperAmmo
                && heavyAmmo == other.heavyAmmo
                && bowPullHold == other.bowPullHold
                && bowPull == other.bowPull
                && playerDoubleJump == other.playerDoubleJump
                && tacticalSprint == other.tacticalSprint
//                && tacticalSprintTime == other.tacticalSprintTime
                && tacticalSprintExhaustion == other.tacticalSprintExhaustion
                && breath == other.breath
//                && breathTime == other.breathTime
                && breathExhaustion == other.breathExhaustion
                && edit == other.edit;
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().revive();
        var original = event.getOriginal().getData(ModAttachments.PLAYER_VARIABLE);
        if (event.getEntity().level().isClientSide()) return;

        event.getEntity().setData(ModAttachments.PLAYER_VARIABLE, original.copy());
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        return writeToNBT();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        readFromNBT(nbt);
    }
}
