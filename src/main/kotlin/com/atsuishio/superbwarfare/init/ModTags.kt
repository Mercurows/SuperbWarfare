package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.perk.Perk
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block

object ModTags {
    @JvmStatic
    fun commonItemTag(name: String): TagKey<Item> {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", name))
    }

    @JvmStatic
    fun commonBlockTag(name: String): TagKey<Block> {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", name))
    }

    @JvmStatic
    fun modItemTag(name: String): TagKey<Item> {
        return ItemTags.create(Mod.loc(name))
    }

    @JvmStatic
    fun modBlockTag(name: String): TagKey<Block> {
        return BlockTags.create(Mod.loc(name))
    }

    @JvmStatic
    fun modDamageTag(name: String): TagKey<DamageType> {
        return TagKey.create(Registries.DAMAGE_TYPE, Mod.loc(name))
    }

    @JvmStatic
    fun modEntityTag(name: String): TagKey<EntityType<*>> {
        return TagKey.create(Registries.ENTITY_TYPE, Mod.loc(name))
    }

    object Items {
        // @formatter:off
        @JvmField val GUN = modItemTag("gun")
        @JvmField val SMG = modItemTag("smg")
        @JvmField val RIFLE = modItemTag("rifle")
        @JvmField val SNIPER_RIFLE = modItemTag("sniper_rifle")
        @JvmField val SHOTGUN = modItemTag("shotgun")
        @JvmField val MACHINE_GUN = modItemTag("machine_gun")
        @JvmField val LAUNCHER = modItemTag("launcher")

        @JvmField val MILITARY_ARMOR = modItemTag("military_armor")
        @JvmField val MILITARY_ARMOR_HEAVY = modItemTag("military_armor_heavy")

        @JvmField val INGOTS_STEEL = modItemTag("ingots/steel")
        @JvmField val INGOTS_LEAD = modItemTag("ingots/lead")
        @JvmField val STORAGE_BLOCK_STEEL = modItemTag("storage_blocks/steel")
        @JvmField val INGOTS_CEMENTED_CARBIDE = modItemTag("ingots/cemented_carbide")
        @JvmField val STORAGE_BLOCK_CEMENTED_CARBIDE = modItemTag("storage_blocks/cemented_carbide")

        @JvmField val BLUEPRINT = modItemTag("blueprint")
        @JvmField val COMMON_BLUEPRINT = modItemTag("blueprint/common")
        @JvmField val RARE_BLUEPRINT = modItemTag("blueprint/rare")
        @JvmField val EPIC_BLUEPRINT = modItemTag("blueprint/epic")
        @JvmField val LEGENDARY_BLUEPRINT = modItemTag("blueprint/legendary")
        @JvmField val SUPERB_BLUEPRINT = modItemTag("blueprint/superb")
        @JvmField val VIRTUAL_BLUEPRINT = modItemTag("blueprint/virtual")
        @JvmField val CANNON_BLUEPRINT = modItemTag("blueprint/cannon")

        // 用于研究台跨级配方的 tag
        @JvmField val ENLARGED_COMMON_BLUEPRINT = modItemTag("blueprint/enlarged/common")
        @JvmField val ENLARGED_RARE_BLUEPRINT = modItemTag("blueprint/enlarged/rare")
        @JvmField val ENLARGED_EPIC_BLUEPRINT = modItemTag("blueprint/enlarged/epic")
        @JvmField val ENLARGED_LEGENDARY_BLUEPRINT = modItemTag("blueprint/enlarged/legendary")

        // Perk tag
        @JvmField val AMMO_PERK = modItemTag("perk/ammo")
        @JvmField val FUNCTIONAL_PERK = modItemTag("perk/functional")
        @JvmField val DAMAGE_PERK = modItemTag("perk/damage")

        @JvmField val RESEARCHABLE_AMMO_PERK = modItemTag("perk/researchable/ammo")
        @JvmField val RESEARCHABLE_FUNCTIONAL_PERK = modItemTag("perk/researchable/functional")
        @JvmField val RESEARCHABLE_DAMAGE_PERK = modItemTag("perk/researchable/damage")

        @JvmField val HAMMER = modItemTag("hammer")
        @JvmField val WRENCHES = commonItemTag("wrenches")
        @JvmField val TOOLS_WRENCH = commonItemTag("tools/wrench")
        @JvmField val TOOLS_CROWBAR = commonItemTag("tools/crowbar")
        @JvmField val TOOLS_HAMMER = commonItemTag("tools/hammer")

        @JvmField val RESEARCH_FUEL = modItemTag("research_fuel")

        // 专门给其他模组添加动画用的枪械武器分类 tag
        @JvmField val ANIMATED_PISTOL = modItemTag("animated/pistol")
        @JvmField val ANIMATED_SNIPER = modItemTag("animated/sniper")
        @JvmField val ANIMATED_RIFLE = modItemTag("animated/rifle")
        @JvmField val ANIMATED_SHOTGUN = modItemTag("animated/shotgun")
        @JvmField val ANIMATED_SMG = modItemTag("animated/smg")
        @JvmField val ANIMATED_RPG = modItemTag("animated/rpg")
        @JvmField val ANIMATED_MG = modItemTag("animated/mg")
        @JvmField val ANIMATED_MINIGUN = modItemTag("animated/minigun")
        // @formatter:on
    }

    object Blocks {
        @JvmField
        val SOFT_COLLISION = modBlockTag("soft_collision")

        @JvmField
        val NORMAL_COLLISION = modBlockTag("normal_collision")

        @JvmField
        val HARD_COLLISION = modBlockTag("hard_collision")

        // 子弹会穿过的方块
        @JvmField
        val BULLET_IGNORE = modBlockTag("bullet_ignore")

        // 子弹会破坏的方块
        @JvmField
        val BULLET_CAN_DESTROY = modBlockTag("bullet_can_destroy")

        // 炮射霰弹会破坏的反馈过
        @JvmField
        val CANNON_SHOT_CAN_DESTROY = modBlockTag("cannon_shot_can_destroy")

        // 辅助降落可识别的方块
        @JvmField
        val AUTO_LANDING = modBlockTag("auto_landing")

        // 载具可以穿过的方块
        @JvmField
        val VEHICLE_PASS_THROUGH = modBlockTag("vehicle_pass_through")

        // TODO 如何移除这个
        // 工兵铲可以挖掘的方块
        @JvmField
        val MINEABLE_WITH_MILITARY_SHOVEL = modBlockTag("mineable/military_shovel")
    }

    object DamageTypes {
        @JvmField
        val PROJECTILE = modDamageTag("projectile")

        @JvmField
        val PROJECTILE_ABSOLUTE = modDamageTag("projectile_absolute")

        // 在载具上的实体受到带有此标签的伤害类型的伤害时，不会将伤害转移到载具上
        @JvmField
        val VEHICLE_IGNORE = modDamageTag("vehicle_ignore")

        // 在载具上的实体受到带有此标签的伤害类型的伤害时，只会受到伤害减免，而不会转移到载具上
        @JvmField
        val VEHICLE_NOT_ABSORB = modDamageTag("vehicle_not_absorb")

        // 载具直接免疫的伤害类型
        @JvmField
        val VEHICLE_IMMUNE = modDamageTag("vehicle_immune")

        // 能够由枪械造成的伤害，可用于perk效果判定
        @JvmField
        val GUN_DAMAGE = modDamageTag("gun_damage")

        // 载具减伤不会计算的伤害类型
        @JvmField
        val BYPASSES_VEHICLE = modDamageTag("bypasses_vehicle")
    }

    object EntityTypes {
        @JvmField
        val AERIAL_BOMB = modEntityTag("aerial_bomb")

        @JvmField
        val DESTROYABLE_PROJECTILE = modEntityTag("destroyable_projectile")

        @JvmField
        val DECOY = modEntityTag("decoy")

        @JvmField
        val NO_EXPERIENCE = modEntityTag("no_experience")

        @JvmField
        val CAN_REPAIR = modEntityTag("can_repair")

        @JvmField
        val MINE = modEntityTag("mine")

        @JvmField
        val AT_ROCKET = modEntityTag("at_rocket")

        @JvmField
        val AA_MISSILE = modEntityTag("aa_missile")

        @JvmField
        val SEEK_BLACKLIST = modEntityTag("seek_blacklist")

        @JvmField
        val BIOGAS_GENERATOR_WHITELIST = modEntityTag("biogas_generator_whitelist")
    }

    object Perks {
        @JvmField
        val TEST: TagKey<Perk> = TagKey.create(ModPerks.PERK_KEY, Mod.loc("test"))
    }
}