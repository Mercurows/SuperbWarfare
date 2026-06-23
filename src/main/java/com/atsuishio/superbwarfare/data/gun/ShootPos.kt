package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
class ShootPos {
    @SerializedName("Transform")
    @SerialName("Transform")
    var transform: String = "Default"

    // TODO 后续替换成kt序列化和kt List

    // 注意这个是复数
    // TODO 允许普通枪使用Positions
    @SerializedName("Positions")
    @SerialName("Positions")
    var positions: java.util.ArrayList<SerializedVec3> = arrayListOf(Vec3.ZERO)

    // TODO 允许普通枪使用Directions
    @SerializedName("Directions")
    @SerialName("Directions")
    var directions: java.util.ArrayList<StringOrVec3> = arrayListOf(StringOrVec3("Default"))

    @SerializedName("ShootPositionForHud")
    @SerialName("ShootPositionForHud")
    var shootPositionForHud: SerializedVec3? = null

    @SerializedName("ShootDirectionForHud")
    @SerialName("ShootDirectionForHud")
    var shootDirectionForHud: StringOrVec3? = null

    @SerializedName("BoundUpWithAmmoAmount")
    @SerialName("BoundUpWithAmmoAmount")
    var boundUpWithAmmoAmount = false


    @SerializedName("ViewPosition")
    @SerialName("ViewPosition")
    var viewPosition: SerializedVec3? = null

    @SerializedName("ViewDirection")
    @SerialName("ViewDirection")
    var viewDirection: StringOrVec3? = null

    @SerializedName("DefaultBarrelDirection")
    @SerialName("DefaultBarrelDirection")
    var defaultBarrelDirection: StringOrVec3? = null

    @SerializedName("DefaultTransform")
    @SerialName("DefaultTransform")
    var defaultTransform: String = "Default"
}
