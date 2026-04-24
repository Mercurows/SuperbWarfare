package com.atsuishio.superbwarfare.data.gun

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DamageReduce {
    @SerializedName("Type")
    @SerialName("Type")
    var type: ReduceType? = null

    @SerializedName("Rate")
    @SerialName("Rate")
    var rate: Double = 0.0

    @SerializedName("MinDistance")
    @SerialName("MinDistance")
    var minDistance: Double = 0.0

    @JvmOverloads
    constructor(type: ReduceType = ReduceType.EMPTY) {
        this.type = type
        this.rate = type.rate
        this.minDistance = type.minDistance
    }

    constructor(rate: Double, minDistance: Double) {
        this.rate = rate
        this.minDistance = minDistance
    }

    fun getDamageRate(): Double {
        return if (this.type == null) this.rate else this.type!!.rate
    }

//    fun setDamageRate(rate: Double) {
//        this.rate = rate
//    }
//
//    fun getMinDistance(): Double {
//        return if (this.type == null) this.minDistance else this.type!!.minDistance
//    }
//
//    fun setMinDistance(minDistance: Double) {
//        this.minDistance = minDistance
//    }

    @Serializable
    enum class ReduceType(val typeName: String, val rate: Double, val minDistance: Double) {
        @SerializedName("Shotgun")
        @SerialName("Shotgun")
        SHOTGUN("Shotgun", 0.05, 15.0),

        @SerializedName("Sniper")
        @SerialName("Sniper")
        SNIPER("Sniper", 0.001, 150.0),

        @SerializedName("Heavy")
        @SerialName("Heavy")
        HEAVY("Heavy", 0.0007, 250.0),

        @SerializedName("Handgun")
        @SerialName("Handgun")
        HANDGUN("Handgun", 0.03, 40.0),

        @SerializedName("Rifle")
        @SerialName("Rifle")
        RIFLE("Rifle", 0.007, 100.0),

        @SerializedName("Smg")
        @SerialName("Smg")
        SMG("Smg", 0.02, 50.0),

        @SerializedName("Empty")
        @SerialName("Empty")
        EMPTY("Empty", 0.0, 0.0),
        ;

        override fun toString() = this.typeName
    }
}
