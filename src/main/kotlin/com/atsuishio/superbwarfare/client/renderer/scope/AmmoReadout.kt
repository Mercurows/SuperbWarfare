package com.atsuishio.superbwarfare.client.renderer.scope

import com.atsuishio.superbwarfare.data.attachment.AmmoBarEntry
import com.atsuishio.superbwarfare.data.attachment.AmmoTextEntry

/**
 * Everything a scope needs to display the current magazine ammo: which bones and text anchors to
 * drive, and the values they should show.
 *
 * [com.atsuishio.superbwarfare.client.renderer.gun.GeoGunRenderer] resolves this once per render so
 * the render path never has to walk the gun's property modifier chain itself, and [bars] and [texts]
 * travel together so the rendering functions can take one extra parameter instead of two.
 */
data class AmmoReadout(
    val bars: List<AmmoBarEntry> = emptyList(),
    val texts: List<AmmoTextEntry> = emptyList(),
    /** Remaining magazine ratio from `1` (full) down to `0`. */
    val progress: Float = 1f,
    /** Rounds left in the magazine. */
    val count: Int = 0,
) {
    /** True when the scope has no ammo display configured at all. */
    val isEmpty: Boolean get() = bars.isEmpty() && texts.isEmpty()
}
