package com.atsuishio.superbwarfare.capability.player;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.capability.ModCapabilities;
import com.atsuishio.superbwarfare.network.message.receive.PlayerVariablesSyncMessage;
import com.atsuishio.superbwarfare.tools.AmmoType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// TODO 在退出世界时正确持久化弹药数量
@EventBusSubscriber(modid = Mod.MODID)
public class PlayerVariable {
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

    public void syncPlayerVariables(Entity entity) {
        if (entity instanceof ServerPlayer) {
            PacketDistributor.sendToAllPlayers(new PlayerVariablesSyncMessage(entity.getId(), this.writeToNBT()));
        }
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

    public PlayerVariable readFromNBT(Tag tag) {
        CompoundTag nbt = (CompoundTag) tag;

        zoom = nbt.getBoolean("Zoom");
        holdFire = nbt.getBoolean("HoldFire");

        for (var type : AmmoType.values()) {
            type.set(this, type.get(nbt));
        }

        bowPullHold = nbt.getBoolean("BowPullHold");
        bowPull = nbt.getBoolean("BowPull");
        playerDoubleJump = nbt.getBoolean("DoubleJump");
        tacticalSprint = nbt.getBoolean("TacticalSprint");
        tacticalSprintTime = nbt.getInt("TacticalSprintTime");
        tacticalSprintExhaustion = nbt.getBoolean("TacticalSprintExhaustion");
        breath = nbt.getBoolean("Breath");
        breathTime = nbt.getInt("BreathTime");
        breathExhaustion = nbt.getBoolean("BreathExhaustion");
        edit = nbt.getBoolean("EditMode");

        return this;
    }

    @SubscribeEvent
    public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        if (cap != null) cap.syncPlayerVariables(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        if (cap != null) cap.syncPlayerVariables(player);
    }

    @SubscribeEvent
    public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        if (cap != null) cap.syncPlayerVariables(player);
    }

    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        event.getOriginal().revive();
        var original = event.getOriginal().getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        var clone = event.getEntity().getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        if (clone == null || original == null) return;

        clone.zoom = original.zoom;
        clone.holdFire = original.holdFire;
        clone.rifleAmmo = original.rifleAmmo;
        clone.handgunAmmo = original.handgunAmmo;
        clone.shotgunAmmo = original.shotgunAmmo;
        clone.sniperAmmo = original.sniperAmmo;
        clone.heavyAmmo = original.heavyAmmo;
        clone.bowPullHold = original.bowPullHold;
        clone.bowPull = original.bowPull;
        clone.playerDoubleJump = original.playerDoubleJump;
        clone.tacticalSprint = original.tacticalSprint;
        clone.tacticalSprintTime = original.tacticalSprintTime;
        clone.tacticalSprintExhaustion = original.tacticalSprintExhaustion;
        clone.breath = original.breath;
        clone.breathTime = original.breathTime;
        clone.breathExhaustion = original.breathExhaustion;
        clone.edit = original.edit;

        if (event.getEntity().level().isClientSide()) return;

        var player = event.getEntity();
        var cap = player.getCapability(ModCapabilities.PLAYER_VARIABLE, null);
        if (cap != null) cap.syncPlayerVariables(player);
    }
}
