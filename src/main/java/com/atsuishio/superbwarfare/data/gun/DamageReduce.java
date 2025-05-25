package com.atsuishio.superbwarfare.data.gun;

import com.google.gson.annotations.SerializedName;

public class DamageReduce {

    @SerializedName("Type")
    public ReduceType type = null;

    @SerializedName("Rate")
    private double rate;

    @SerializedName("MinDistance")
    private double minDistance;

    public DamageReduce() {
        this(ReduceType.EMPTY);
    }

    public DamageReduce(ReduceType type) {
        this.type = type;
        this.rate = type.rate;
        this.minDistance = type.minDistance;
    }

    public DamageReduce(double rate, double minDistance) {
        this.rate = rate;
        this.minDistance = minDistance;
    }

    public double getRate() {
        return this.type == null ? this.rate : this.type.rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public double getMinDistance() {
        return this.type == null ? this.minDistance : this.type.minDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    public enum ReduceType {
        @SerializedName("Shotgun")
        SHOTGUN("Shotgun", 0.05, 15),
        @SerializedName("Sniper")
        SNIPER("Sniper", 0.001, 150),
        @SerializedName("Heavy")
        HEAVY("Heavy", 0.0007, 250),
        @SerializedName("Handgun")
        HANDGUN("Handgun", 0.03, 40),
        @SerializedName("Rifle")
        RIFLE("Rifle", 0.007, 100),
        @SerializedName("Smg")
        SMG("Smg", 0.02, 50),
        @SerializedName("Empty")
        EMPTY("Empty", 0, 0),
        ;

        public final double rate;
        public final double minDistance;
        public final String name;

        ReduceType(String name, double rate, double minDistance) {
            this.name = name;
            this.rate = rate;
            this.minDistance = minDistance;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
