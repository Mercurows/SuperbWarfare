package com.atsuishio.superbwarfare.data.vehicle

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import net.minecraftforge.registries.ForgeRegistries

@Serializable
open class VehicleMountSlot @JvmOverloads constructor(
    @SerialName("Id") val id: String,
    @SerialName("Definitions") val definitions: List<SerializedResourceLocation>,
    @SerialName("Capacity") val capacity: Int = 1,
    @SerialName("ControlSeat") val controlSeat: Int = 0,
    @SerialName("Transform") val transform: String = "Default",
    @SerialName("Offset") val offset: SerializedVec3 = Vec3.ZERO,
    @SerialName("Bone") val bone: String = "mount_pos",
)

@Serializable
open class VehicleMountDefinition(
    @SerialName("Ingredient") val ingredient: Ingredient,
    @SerialName("Action") val action: Action,
    @SerialName("Capacity") val capacity: Int = 64,
    @SerialName("Display") val display: Display = Display(),
) {
    @Serializable
    open class Ingredient(
        @SerialName("Item") val item: SerializedResourceLocation? = null,
        @SerialName("Tag") val tag: SerializedResourceLocation? = null,
    ) {
        open fun matches(stack: ItemStack): Boolean = !stack.isEmpty && (
            item != null && item == ForgeRegistries.ITEMS.getKey(stack.item) ||
                tag?.let { stack.`is`(TagKey.create(Registries.ITEM, it)) } == true
            )
    }

    @Serializable
    open class Action(
        @SerialName("Type") val type: Type,
        @SerialName("Entity") val entity: SerializedResourceLocation? = null,
        @SerialName("Data") val data: JsonObject? = null,
        @SerialName("Offset") val offset: SerializedVec3 = Vec3.ZERO,
        @SerialName("Damage") val damage: Float? = null,
        @SerialName("ExplosionDamage") val explosionDamage: Float? = null,
        @SerialName("ExplosionRadius") val explosionRadius: Float? = null,
        @SerialName("Speed") val speed: Float = 2f,
        @SerialName("InheritMotion") val inheritMotion: Double = 0.2,
        @SerialName("OnDestroyed") val onDestroyed: Boolean = false,
        @SerialName("ImpactSpeed") val impactSpeed: Double? = null,
    ) {
        @Serializable
        enum class Type {
            @SerialName("DropItem") DROP_ITEM,
            @SerialName("ReleaseEntity") RELEASE_ENTITY,
            @SerialName("LaunchEntity") LAUNCH_ENTITY,
            @SerialName("Detonate") DETONATE,
        }
    }

    @Serializable
    open class Display @JvmOverloads constructor(
        @SerialName("Offset") val offset: SerializedVec3 = Vec3.ZERO,
        @SerialName("Rotation") val rotation: SerializedVec3 = Vec3.ZERO,
        @SerialName("Scale") val scale: Float = 0.4f,
        @SerialName("Entity") val entity: SerializedResourceLocation? = null,
    )
}
