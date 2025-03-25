package com.atsuishio.superbwarfare.client;

/**
 * Code based on @EEEAB's EEEABsMobs and @Mercurows's DreamaticVoyage
 */
public class AnimationTicker {

    private int tick;
    private int prevTick;
    private int duration;

    public AnimationTicker(int duration) {
        this.tick = 0;
        this.prevTick = 0;
        this.duration = duration;
    }

    public void setDuration(int duration) {
        this.tick = 0;
        this.prevTick = 0;
        this.duration = duration;
    }

    public int getTick() {
        return this.tick;
    }

    public boolean isStopped() {
        return this.tick == 0 && this.prevTick == 0;
    }

    public boolean isEnded() {
        return this.tick == this.duration || this.prevTick == this.duration;
    }

    public int getPrevTick() {
        return this.prevTick;
    }

    public int getDuration() {
        return this.duration;
    }

    public void changeTimer(boolean flag) {
        changeTimer(flag, 1);
    }

    public void changeTimer(boolean add, int time) {
        if (add) {
            increaseTimer(time);
        } else {
            decreaseTimer(time);
        }
    }

    public void increaseTimer(int time) {
        int newTime = this.tick + time;
        if (newTime <= duration && newTime >= 0) {
            this.tick = newTime;
        } else {
            this.tick = newTime < 0 ? 0 : duration;
        }
    }

    public void decreaseTimer(int time) {
        if (this.tick - time > 0.0D) {
            this.tick -= time;
        } else {
            this.tick = 0;
        }
    }

    public void updatePrevTimer() {
        this.prevTick = this.tick;
    }

    public void resetTimer() {
        this.tick = 0;
        this.prevTick = 0;
    }

}
