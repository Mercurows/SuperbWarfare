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

    public static class Items {
        public static final TagKey<Item> GUN = tag("gun");
        public static final TagKey<Item> SMG = tag("smg");
        public static final TagKey<Item> RIFLE = tag("rifle");
        public static final TagKey<Item> SNIPER_RIFLE = tag("sniper_rifle");
        public static final TagKey<Item> SHOTGUN = tag("shotgun");
        public static final TagKey<Item> HEAVY_WEAPON = tag("heavy_weapon");

        public static final TagKey<Item> LAUNCHER = tag("launcher");
        public static final TagKey<Item> LAUNCHER_GRENADE = tag("launcher/grenade");

        public static final TagKey<Item> NORMAL_GUN = tag("normal_gun");

        public static final TagKey<Item> MILITARY_ARMOR = tag("military_armor");
        public static final TagKey<Item> MILITARY_ARMOR_HEAVY = tag("military_armor_heavy");

        public static final TagKey<Item> INGOTS_STEEL = tag("ingots/steel");
        public static final TagKey<Item> STORAGE_BLOCK_STEEL = tag("storage_blocks/steel");

        public static final TagKey<Item> INGOTS_CEMENTED_CARBIDE = tag("ingots/cemented_carbide");
        public static final TagKey<Item> STORAGE_BLOCK_CEMENTED_CARBIDE = tag("storage_blocks/cemented_carbide");

        public static final TagKey<Item> BLUEPRINT = tag("blueprint");
        public static final TagKey<Item> COMMON_BLUEPRINT = tag("blueprint/common");
        public static final TagKey<Item> RARE_BLUEPRINT = tag("blueprint/rare");
        public static final TagKey<Item> EPIC_BLUEPRINT = tag("blueprint/epic");
        public static final TagKey<Item> LEGENDARY_BLUEPRINT = tag("blueprint/legendary");
        public static final TagKey<Item> CANNON_BLUEPRINT = tag("blueprint/cannon");

        public static final TagKey<Item> WRENCHES_TAG = ItemTags.create(new ResourceLocation("forge", "wrenches"));
        public static final TagKey<Item> WRENCH_TAG = ItemTags.create(new ResourceLocation("forge", "tools/wrench"));

        private static TagKey<Item> tag(String name) {
            return ItemTags.create(Mod.loc(name));
        }
    }

    public static class Blocks {
        public static final TagKey<Block> SOFT_COLLISION = tag("soft_collision");
        public static final TagKey<Block> HARD_COLLISION = tag("hard_collision");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(Mod.loc(name));
        }
    }

    public static class DamageTypes {
        public static final TagKey<DamageType> PROJECTILE = tag("projectile");
        public static final TagKey<DamageType> PROJECTILE_ABSOLUTE = tag("projectile_absolute");

        // 在载具上的实体受到带有此标签的伤害类型的伤害时，不会将伤害转移到载具上
        public static final TagKey<DamageType> VEHICLE_IGNORE = tag("vehicle_ignore");
        // 在载具上的实体受到带有此标签的伤害类型的伤害时，只会受到伤害减免，而不会转移到载具上
        public static final TagKey<DamageType> VEHICLE_NOT_ABSORB = tag("vehicle_not_absorb");

        private static TagKey<DamageType> tag(String name) {
            return TagKey.create(Registries.DAMAGE_TYPE, Mod.loc(name));
        }
    }

    public static class EntityTypes {
        public static final TagKey<EntityType<?>> AERIAL_BOMB = tag("aerial_bomb");
        public static final TagKey<EntityType<?>> DESTROYABLE_PROJECTILE = tag("destroyable_projectile");
        public static final TagKey<EntityType<?>> DECOY = tag("decoy");
        public static final TagKey<EntityType<?>> NO_EXPERIENCE = tag("no_experience");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, Mod.loc(name));
        }
    }
}
