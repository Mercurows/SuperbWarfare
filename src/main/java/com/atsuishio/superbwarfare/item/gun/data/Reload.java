package com.atsuishio.superbwarfare.item.gun.data;

import net.minecraft.nbt.CompoundTag;

public final class Reload {
    private final CompoundTag data;

    Reload(GunData data) {
        this.data = data.data();
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

    public int stage() {
        return data.getInt("ReloadStage");
    }

    public void setStage(int stage) {
        if (stage <= 0) {
            data.remove("ReloadStage");
        } else {
            data.putInt("ReloadStage", stage);
        }
    }

    public void markStart() {
        data.putBoolean("StartReload", true);
    }

    public void markStarted() {
        data.remove("StartReload");
    }

    public boolean shouldStart() {
        return data.getBoolean("StartReload");
    }

    public int time() {
        return data.getInt("ReloadTime");
    }

    public void setTime(int time) {
        if (time <= 0) {
            data.remove("ReloadTime");
        } else {
            data.putInt("ReloadTime", time);
        }
    }

    public void reduce() {
        setTime(time() - 1);
    }
}
