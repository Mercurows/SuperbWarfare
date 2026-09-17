package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
data class ShootPos(
    @SerialName("Transform")
    val transform: String = "Default",

    // TODO 后续替换成kt序列化和kt List

    // 注意这个是复数
    // TODO 允许普通枪使用Positions
    @SerialName("Positions")
    val positions: java.util.ArrayList<SerializedVec3> = arrayListOf(Vec3.ZERO),

    // TODO 允许普通枪使用Directions
    @SerialName("Directions")
    val directions: java.util.ArrayList<StringOrVec3> = arrayListOf(StringOrVec3("Default")),

    @SerialName("ShootPositionForHud")
    val shootPositionForHud: SerializedVec3? = null,

    @SerialName("ShootDirectionForHud")
    val shootDirectionForHud: StringOrVec3? = null,

    @SerialName("BoundUpWithAmmoAmount")
    val boundUpWithAmmoAmount: Boolean = false,

    @SerialName("ViewPosition")
    val viewPosition: SerializedVec3? = null,

    @SerialName("ViewDirection")
    val viewDirection: StringOrVec3? = null,

    @SerialName("DefaultBarrelDirection")
    val defaultBarrelDirection: StringOrVec3? = null,

    @SerialName("DefaultTransform")
    val defaultTransform: String = "Default",
)
