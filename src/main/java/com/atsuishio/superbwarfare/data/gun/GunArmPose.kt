package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GunArmPose {
    @SerialName("Default")
    DEFAULT,

    @SerialName("MiniGun")
    MINI_GUN,

    @SerialName("SuperStarShooter")
    SUPER_STAR_SHOOTER,

    @SerialName("SingleHand")
    SINGLE_HAND,

    @SerialName("LungeMine")
    LUNGE_MINE,

    @SerialName("Item")
    ITEM
}
