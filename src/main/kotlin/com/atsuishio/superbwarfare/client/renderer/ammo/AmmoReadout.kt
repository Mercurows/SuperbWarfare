package com.atsuishio.superbwarfare.client.renderer.ammo

import com.atsuishio.superbwarfare.data.attachment.AmmoBarEntry
import com.atsuishio.superbwarfare.data.attachment.AmmoTextEntry

/**
 * Everything a model needs to display the current magazine ammo: which bones and text anchors to
 * drive, and the values they should show.
 *
 * [com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer] resolves this once per render so
 * the render path never has to walk the gun's property modifier chain itself, and [bars] and [texts]
 * travel together so the rendering functions can take one extra parameter instead of two.
 */
data class AmmoReadout(
    val bars: List<AmmoBarEntry> = emptyList(),
    val texts: List<AmmoTextEntry> = emptyList(),
    /**
     * Remaining magazine ratio from `1` (full) down to `0`.
     *
     * For a backpack energy weapon whose ammo *is* its stored FE (see `GunData.energyAmmoReadout`)
     * this is that gun's charge level instead — same direction, so the bar drains the same way.
     */
    val progress: Float = 1f,
    /**
     * Rounds left in the magazine — or, for a backpack energy weapon, how many more shots the stored
     * charge covers under the **current fire mode's** `AmmoCostPerShoot`.
     */
    val count: Int = 0,
    /**
     * Distance to whatever the shooter is looking at, in blocks — what a `%range%` text anchor
     * expands to. [AmmoTextEntry.NO_RANGE] means "no reading this frame", which is the default:
     * only the local player's own held gun in first person measures anything, and only when one of
     * its text anchors actually asks for it (see `GeoGunRenderer.attachmentReadout`).
     */
    val range: Int = AmmoTextEntry.NO_RANGE,
    /**
     * Weapon heat, `0` to `100` — what a `%heat%` text anchor expands to and what that anchor's
     * color tiers are read against. Only a gun has heat, and even for one it is only resolved when
     * some text actually asks for it, so a model with no `%heat%` anchor always sees `0` here.
     */
    val heat: Int = 0,
) {
    /** True when the model has no ammo display configured at all. */
    val isEmpty: Boolean get() = bars.isEmpty() && texts.isEmpty()
}
