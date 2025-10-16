package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    public static TagKey<Item> commonItemTag(String name) {
        return ItemTags.create(new ResourceLocation("forge", name));
    }

    public static class Items {
        public static final TagKey<Item> GUN = modItemTag("gun");
        public static final TagKey<Item> SMG = modItemTag("smg");
        public static final TagKey<Item> RIFLE = modItemTag("rifle");
        public static final TagKey<Item> SNIPER_RIFLE = modItemTag("sniper_rifle");
        public static final TagKey<Item> SHOTGUN = modItemTag("shotgun");
        public static final TagKey<Item> HEAVY_WEAPON = modItemTag("heavy_weapon");

        public static final TagKey<Item> LAUNCHER = modItemTag("launcher");
        public static final TagKey<Item> LAUNCHER_GRENADE = modItemTag("launcher/grenade");

        public static final TagKey<Item> MILITARY_ARMOR = modItemTag("military_armor");
        public static final TagKey<Item> MILITARY_ARMOR_HEAVY = modItemTag("military_armor_heavy");

        public static final TagKey<Item> INGOTS_STEEL = modItemTag("ingots/steel");
        public static final TagKey<Item> STORAGE_BLOCK_STEEL = modItemTag("storage_blocks/steel");

        public static final TagKey<Item> INGOTS_CEMENTED_CARBIDE = modItemTag("ingots/cemented_carbide");
        public static final TagKey<Item> STORAGE_BLOCK_CEMENTED_CARBIDE = modItemTag("storage_blocks/cemented_carbide");

        public static final TagKey<Item> BLUEPRINT = modItemTag("blueprint");
        public static final TagKey<Item> COMMON_BLUEPRINT = modItemTag("blueprint/common");
        public static final TagKey<Item> RARE_BLUEPRINT = modItemTag("blueprint/rare");
        public static final TagKey<Item> EPIC_BLUEPRINT = modItemTag("blueprint/epic");
        public static final TagKey<Item> LEGENDARY_BLUEPRINT = modItemTag("blueprint/legendary");
        public static final TagKey<Item> CANNON_BLUEPRINT = modItemTag("blueprint/cannon");

        public static final TagKey<Item> HAMMER = modItemTag("hammer");

        public static final TagKey<Item> WRENCHES = commonItemTag("wrenches");
        public static final TagKey<Item> TOOLS_WRENCH = commonItemTag("tools/wrench");
        public static final TagKey<Item> TOOLS_CROWBAR = commonItemTag("tools/crowbar");
        public static final TagKey<Item> TOOLS_HAMMER = commonItemTag("tools/hammer");

        // 专门给其他模组添加动画用的枪械武器分类 tag
        public static final TagKey<Item> ANIMATED_PISTOL = modItemTag("animated/pistol");
        public static final TagKey<Item> ANIMATED_SNIPER = modItemTag("animated/sniper");
        public static final TagKey<Item> ANIMATED_RIFLE = modItemTag("animated/rifle");
        public static final TagKey<Item> ANIMATED_SHOTGUN = modItemTag("animated/shotgun");
        public static final TagKey<Item> ANIMATED_SMG = modItemTag("animated/smg");
        public static final TagKey<Item> ANIMATED_RPG = modItemTag("animated/rpg");
        public static final TagKey<Item> ANIMATED_MG = modItemTag("animated/mg");
        public static final TagKey<Item> ANIMATED_MINIGUN = modItemTag("animated/minigun");
    }

    public static TagKey<Item> modItemTag(String name) {
        return ItemTags.create(Mod.loc(name));
    }

    public static class Blocks {
        public static final TagKey<Block> SOFT_COLLISION = tag("soft_collision");
        public static final TagKey<Block> NORMAL_COLLISION = tag("normal_collision");
        public static final TagKey<Block> HARD_COLLISION = tag("hard_collision");

        // 子弹会穿过的方块
        public static final TagKey<Block> BULLET_IGNORE = tag("bullet_ignore");
        // 子弹会破坏的方块
        public static final TagKey<Block> BULLET_CAN_DESTROY = tag("bullet_can_destroy");
        // 炮射霰弹会破坏的反馈过
        public static final TagKey<Block> CANNON_SHOT_CAN_DESTROY = tag("cannon_shot_can_destroy");

        // 辅助降落可识别的方块
        public static final TagKey<Block> AUTO_LANDING = tag("auto_landing");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(Mod.loc(name));
        }
    }

    public static class DamageTypes {
        public static final TagKey<DamageType> PROJECTILE = modDamageTag("projectile");
        public static final TagKey<DamageType> PROJECTILE_ABSOLUTE = modDamageTag("projectile_absolute");

        // 在载具上的实体受到带有此标签的伤害类型的伤害时，不会将伤害转移到载具上
        public static final TagKey<DamageType> VEHICLE_IGNORE = modDamageTag("vehicle_ignore");
        // 在载具上的实体受到带有此标签的伤害类型的伤害时，只会受到伤害减免，而不会转移到载具上
        public static final TagKey<DamageType> VEHICLE_NOT_ABSORB = modDamageTag("vehicle_not_absorb");

        // 能够由枪械造成的伤害，可用于perk效果判定
        public static final TagKey<DamageType> GUN_DAMAGE = modDamageTag("gun_damage");

        // 载具减伤不会计算的伤害类型
        public static final TagKey<DamageType> BYPASSES_VEHICLE = modDamageTag("bypasses_vehicle");
    }

    public static TagKey<DamageType> modDamageTag(String name) {
        return TagKey.create(Registries.DAMAGE_TYPE, Mod.loc(name));
    }

    public static class EntityTypes {
        public static final TagKey<EntityType<?>> AERIAL_BOMB = modEntityTag("aerial_bomb");
        public static final TagKey<EntityType<?>> DESTROYABLE_PROJECTILE = modEntityTag("destroyable_projectile");
        public static final TagKey<EntityType<?>> DECOY = modEntityTag("decoy");
        public static final TagKey<EntityType<?>> NO_EXPERIENCE = modEntityTag("no_experience");
        public static final TagKey<EntityType<?>> CAN_REPAIR = modEntityTag("can_repair");
        public static final TagKey<EntityType<?>> MINE = modEntityTag("mine");

        public static final TagKey<EntityType<?>> AT_ROCKET = modEntityTag("at_rocket");
    }

    private static TagKey<EntityType<?>> modEntityTag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Mod.loc(name));
    }
}
