package com.atsuishio.superbwarfare.perk;

import com.atsuishio.superbwarfare.init.ModPerks;

import java.util.function.Predicate;

public class PerkHelper {

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
                || perk == ModPerks.SUBSISTENCE.get()
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
