package com.atsuishio.superbwarfare.data.gun.subdata;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.value.IntValue;
import com.atsuishio.superbwarfare.data.gun.value.ReloadState;
import com.atsuishio.superbwarfare.data.gun.value.Starter;
import com.atsuishio.superbwarfare.data.gun.value.Timer;
import net.minecraft.nbt.CompoundTag;

public final class Reload {
    private final CompoundTag data;

    public final Timer reloadTimer;
    public final Timer prepareTimer;
    public final Timer prepareLoadTimer;
    public final Timer iterativeLoadTimer;
    public final Timer finishTimer;

    public final Starter reloadStarter;
    public final Starter singleReloadStarter;
    public final Starter stage3Starter;

    public Reload(GunData data) {
        this.data = data.data();

        reloadTimer = new Timer(this.data, "Reload");
        prepareTimer = new Timer(this.data, "Prepare");
        prepareLoadTimer = new Timer(this.data, "PrepareLoad");
        iterativeLoadTimer = new Timer(this.data, "IterativeLoad");
        finishTimer = new Timer(this.data, "Finish");

        reloadStarter = new Starter(this.data, "Reload");
        singleReloadStarter = new Starter(this.data, "SingleReload");
        stage3Starter = new Starter(this.data, "Stage3Forcefully");

        stage = new IntValue(this.data, "ReloadStage");
    }

    public ReloadState state() {
        return switch (data.getInt("ReloadState")) {
            case 1 -> ReloadState.NORMAL_RELOADING;
            case 2 -> ReloadState.EMPTY_RELOADING;
            default -> ReloadState.NOT_RELOADING;
        };
    }

    public boolean normal() {
        return state() == ReloadState.NORMAL_RELOADING;
    }

    public boolean empty() {
        return state() == ReloadState.EMPTY_RELOADING;
    }

    public void setState(ReloadState state) {
        if (state == ReloadState.NOT_RELOADING) {
            data.remove("ReloadState");
        } else {
            data.putInt("ReloadState", state.ordinal());
        }
    }

    public final IntValue stage;

    public int stage() {
        return stage.get();
    }

    public void setStage(int stage) {
        this.stage.set(stage);
    }


    public int time() {
        return reloadTimer.get();
    }

    public void setTime(int time) {
        reloadTimer.set(time);
    }

    public void reduce() {
        reloadTimer.reduce();
    }
}
