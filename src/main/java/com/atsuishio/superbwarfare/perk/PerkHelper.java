package com.atsuishio.superbwarfare.perk;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModPerks;
import com.atsuishio.superbwarfare.item.PerkItem;
import com.atsuishio.superbwarfare.item.gun.GunData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Predicate;

public class PerkHelper {

    private static final String TAG_PERK_ID = "id";
    private static final String TAG_PERK_LEVEL = "level";
    private static final String TAG_PERK = "Perks";

    /**
     * 把一个Perk封装成nbt进行存储
     */
    public static CompoundTag makePerk(@Nullable ResourceLocation pId, int pLevel) {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString(TAG_PERK_ID, String.valueOf(pId));
        compoundtag.putShort(TAG_PERK_LEVEL, (short) pLevel);
        return compoundtag;
    }

    public static CompoundTag setPerkLevel(CompoundTag pCompound, int pLevel) {
        pCompound.putShort(TAG_PERK_LEVEL, (short) pLevel);
        return pCompound;
    }

    public static int getPerkLevel(CompoundTag pCompound) {
        return Mth.clamp(pCompound.getInt(TAG_PERK_LEVEL), 0, 255);
    }

    @Nullable
    public static ResourceLocation getPerkId(CompoundTag pCompoundTag) {
        return ResourceLocation.tryParse(pCompoundTag.getString(TAG_PERK_ID));
    }

    @Nullable
    public static ResourceLocation getPerkId(Perk perk) {
        return switch (perk.type) {
            case AMMO -> ModPerks.AMMO_PERKS.getEntries().stream()
                    .filter(p -> p.get().descriptionId.equals(perk.descriptionId))
                    .findFirst()
                    .map(p -> ResourceLocation.tryParse(p.value().descriptionId))
                    .orElse(null);
            case FUNCTIONAL -> ModPerks.FUNC_PERKS.getEntries().stream()
                    .filter(p -> p.get().descriptionId.equals(perk.descriptionId))
                    .findFirst()
                    .map(p -> ResourceLocation.tryParse(p.value().descriptionId))
                    .orElse(null);
            case DAMAGE -> ModPerks.DAMAGE_PERKS.getEntries().stream()
                    .filter(p -> p.get().descriptionId.equals(perk.descriptionId))
                    .findFirst()
                    .map(p -> ResourceLocation.tryParse(p.value().descriptionId))
                    .orElse(null);
        };
    }

    public static int getItemPerkLevel(Perk perk, GunData data) {
        return getItemPerkLevel(perk, data.getTag());
    }

    public static int getItemPerkLevel(Perk perk, final CompoundTag tag) {
        var tagPerk = tag.getCompound(TAG_PERK);
        if (!tagPerk.contains(perk.type.getName())) {
            return 0;
        }

        var pt = tagPerk.getCompound(perk.type.getName());
        ResourceLocation id = getPerkId(perk);
        if (id == null) {
            return 0;
        }

        if (!pt.getString(TAG_PERK_ID).equals(makeId(id))) {
            return 0;
        }

        return getPerkLevel(getPerkTag(tag, perk.type));
    }

    public static CompoundTag getPerkTag(GunData data, Perk.Type type) {
        return getPerkTag(data.getTag(), type);
    }

    public static CompoundTag getPerkTag(final CompoundTag tag, Perk.Type type) {
        var tagPerk = tag.getCompound(TAG_PERK);
        if (!tagPerk.contains(type.getName())) return new CompoundTag();
        return tagPerk.getCompound(type.getName());
    }

    public static void setPerk(final CompoundTag tag, Perk perk, int level) {
        var perkTag = tag.getCompound(TAG_PERK);
        if (perkTag.isEmpty()) {
            perkTag = new CompoundTag();
        }

        perkTag.put(perk.type.getName(), makePerk(getPerkId(perk), level));
        tag.put(TAG_PERK, perkTag);
    }

    public static void setPerk(final CompoundTag tag, Perk perk) {
        setPerk(tag, perk, 1);
    }

    public static Perk getPerkByType(GunData data, Perk.Type type) {
        return getPerkByType(data.getTag(), type);
    }


    @Nullable
    public static Perk getPerkByType(final CompoundTag tag, Perk.Type type) {
        var tagPerk = tag.getCompound(TAG_PERK);
        if (!tagPerk.contains(type.getName())) {
            return null;
        }

        return switch (type) {
            case AMMO -> ModPerks.AMMO_PERKS.getEntries().stream()
                    .filter(p -> makeId(p.getId()).equals(tagPerk.getCompound(type.getName()).getString(TAG_PERK_ID)))
                    .findFirst()
                    .map(DeferredHolder::value)
                    .orElse(null);
            case FUNCTIONAL -> ModPerks.FUNC_PERKS.getEntries().stream()
                    .filter(p -> makeId(p.getId()).equals(tagPerk.getCompound(type.getName()).getString(TAG_PERK_ID)))
                    .findFirst()
                    .map(DeferredHolder::value)
                    .orElse(null);
            case DAMAGE -> ModPerks.DAMAGE_PERKS.getEntries().stream()
                    .filter(p -> makeId(p.getId()).equals(tagPerk.getCompound(type.getName()).getString(TAG_PERK_ID)))
                    .findFirst()
                    .map(DeferredHolder::value)
                    .orElse(null);
        };
    }

    public static void removePerkByType(final CompoundTag tag, Perk.Type type) {
        var tagPerk = tag.getCompound(TAG_PERK);
        if (!tagPerk.contains(type.getName())) {
            return;
        }

        tagPerk.remove(type.getName());
        tag.put(TAG_PERK, tagPerk);
    }

    public static Optional<DeferredHolder<Item, ? extends Item>> getPerkItem(Perk perk) {
        return ModItems.PERKS.getEntries().stream().filter(p -> {
            if (p.get() instanceof PerkItem<?> perkItem) {
                return perkItem.getPerk() == perk;
            }
            return false;
        }).findFirst();
    }

    public static String makeId(ResourceLocation resourceLocation) {
        return resourceLocation.getNamespace() + ":" + resourceLocation.getPath();
    }

    public static final Predicate<Perk> SHOTGUN_PERKS = perk -> switch (perk.type) {
        case AMMO -> !perk.descriptionId.equals("butterfly_bullet") && perk != ModPerks.MICRO_MISSILE.get()
                && perk != ModPerks.LONGER_WIRE.get();
        case FUNCTIONAL -> perk == ModPerks.SUBSISTENCE.get()
                || perk == ModPerks.POWERFUL_ATTRACTION.get()
                || perk == ModPerks.HEAL_CLIP.get()
                || perk == ModPerks.FIELD_DOCTOR.get()
                || perk == ModPerks.INTELLIGENT_CHIP.get();
        case DAMAGE -> perk == ModPerks.GUTSHOT_STRAIGHT.get()
                || perk == ModPerks.MONSTER_HUNTER.get()
                || perk == ModPerks.KILL_CLIP.get()
                || perk == ModPerks.VORPAL_WEAPON.get();
    };

    public static final Predicate<Perk> RIFLE_PERKS = perk -> switch (perk.type) {
        case AMMO -> perk != ModPerks.MICRO_MISSILE.get() && perk != ModPerks.LONGER_WIRE.get();
        case FUNCTIONAL -> perk == ModPerks.HEAL_CLIP.get() || perk == ModPerks.FIELD_DOCTOR.get() ||
                perk == ModPerks.FOURTH_TIMES_CHARM.get() || perk == ModPerks.SUBSISTENCE.get() ||
                perk == ModPerks.POWERFUL_ATTRACTION.get() || perk == ModPerks.INTELLIGENT_CHIP.get();
        case DAMAGE ->
                perk == ModPerks.KILL_CLIP.get() || perk == ModPerks.GUTSHOT_STRAIGHT.get() || perk == ModPerks.MONSTER_HUNTER.get() ||
                        perk == ModPerks.HEAD_SEEKER.get() || perk == ModPerks.DESPERADO.get() || perk == ModPerks.VORPAL_WEAPON.get();
    };

    public static final Predicate<Perk> HANDGUN_PERKS = perk -> switch (perk.type) {
        case AMMO -> !perk.descriptionId.equals("butterfly_bullet") && perk != ModPerks.MICRO_MISSILE.get()
                && perk != ModPerks.LONGER_WIRE.get();
        case FUNCTIONAL -> perk == ModPerks.HEAL_CLIP.get()
                || perk == ModPerks.FIELD_DOCTOR.get()
                || perk == ModPerks.SUBSISTENCE.get()
                || perk == ModPerks.POWERFUL_ATTRACTION.get()
                || perk == ModPerks.INTELLIGENT_CHIP.get();
        case DAMAGE -> perk == ModPerks.KILL_CLIP.get()
                || perk == ModPerks.GUTSHOT_STRAIGHT.get()
                || perk == ModPerks.MONSTER_HUNTER.get()
                || perk == ModPerks.VORPAL_WEAPON.get();
    };

    public static final Predicate<Perk> SNIPER_RIFLE_PERKS = perk -> switch (perk.type) {
        case AMMO -> !perk.descriptionId.equals("butterfly_bullet") && perk != ModPerks.MICRO_MISSILE.get()
                && perk != ModPerks.LONGER_WIRE.get();
        case FUNCTIONAL -> perk == ModPerks.HEAL_CLIP.get()
                || perk == ModPerks.FOURTH_TIMES_CHARM.get()
                || perk == ModPerks.POWERFUL_ATTRACTION.get()
                || perk == ModPerks.INTELLIGENT_CHIP.get();

        case DAMAGE -> perk == ModPerks.KILL_CLIP.get()
                || perk == ModPerks.MONSTER_HUNTER.get()
                || perk == ModPerks.VORPAL_WEAPON.get();
    };

    public static final Predicate<Perk> SMG_PERKS = perk -> switch (perk.type) {
        case AMMO -> perk != ModPerks.MICRO_MISSILE.get() && perk != ModPerks.LONGER_WIRE.get();
        case FUNCTIONAL -> perk == ModPerks.HEAL_CLIP.get()
                || perk == ModPerks.FOURTH_TIMES_CHARM.get()
                || perk == ModPerks.SUBSISTENCE.get()
                || perk == ModPerks.POWERFUL_ATTRACTION.get()
                || perk == ModPerks.INTELLIGENT_CHIP.get();
        case DAMAGE -> perk == ModPerks.KILL_CLIP.get()
                || perk == ModPerks.GUTSHOT_STRAIGHT.get()
                || perk == ModPerks.MONSTER_HUNTER.get()
                || perk == ModPerks.HEAD_SEEKER.get()
                || perk == ModPerks.DESPERADO.get()
                || perk == ModPerks.VORPAL_WEAPON.get();
    };

    public static final Predicate<Perk> MACHINE_GUN_PERKS = perk -> switch (perk.type) {
        case AMMO -> perk != ModPerks.MICRO_MISSILE.get() && perk != ModPerks.LONGER_WIRE.get();
        case FUNCTIONAL -> perk == ModPerks.FOURTH_TIMES_CHARM.get()
                || perk == ModPerks.SUBSISTENCE.get()
                || perk == ModPerks.POWERFUL_ATTRACTION.get()
                || perk == ModPerks.INTELLIGENT_CHIP.get();
        case DAMAGE -> perk == ModPerks.MONSTER_HUNTER.get()
                || perk == ModPerks.KILLING_TALLY.get()
                || perk == ModPerks.VORPAL_WEAPON.get();
    };

    public static final Predicate<Perk> MAGAZINE_PERKS = perk -> false;

    public static final Predicate<Perk> LAUNCHER_PERKS = perk -> perk == ModPerks.MONSTER_HUNTER.get()
            || perk == ModPerks.POWERFUL_ATTRACTION.get()
            || perk == ModPerks.INTELLIGENT_CHIP.get()
            || perk == ModPerks.VORPAL_WEAPON.get();
}
