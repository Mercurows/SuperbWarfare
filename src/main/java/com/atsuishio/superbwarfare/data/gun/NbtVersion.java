package com.atsuishio.superbwarfare.data.gun;

/**
 * Lightweight version counter attached to a single {@link GunData} instance.
 * <p>
 * Two independent counters track different classes of NBT mutation:
 * <ul>
 *   <li><b>structural</b>: changes that affect computed gun properties (attachment
 *       swaps, perk changes, fire-mode selection, property overrides). Any change
 *       here increments the structural version and requires a full PMC rebuild.</li>
 *   <li><b>state</b>: changes to ephemeral runtime values (ammo count, heat,
 *       reload timers, bolt timers). These do NOT require a PMC rebuild.</li>
 * </ul>
 * <p>
 * Consumers compare a cached snapshot of {@link #getStructural()} against the current
 * value; a mismatch means the PMC is stale and must be recomputed.
 *
 * @author superbwarfare contributors
 * @since 0.8.9
 */
public class NbtVersion {

    /**
     * Incremented whenever a <i>structural</i> NBT field changes.
     * Wrap-around on {@link Integer#MAX_VALUE} is safe because comparisons use {@code !=}.
     */
    private int structural = 0;

    /**
     * Incremented whenever a <i>state</i> NBT field changes.
     */
    private int state = 0;

    /**
     * Gets the current structural version counter.
     *
     * @return current structural version.
     */
    public int getStructural() {
        return structural;
    }

    /**
     * Gets the current state version counter.
     *
     * @return current state version.
     */
    public int getState() {
        return state;
    }

    /**
     * Signals that a structural property has changed.
     * Also increments {@link #state} because structural changes encompass state changes.
     */
    public void invalidateStructural() {
        this.structural++;
        this.state++;
    }

    /**
     * Signals that only a state property has changed.
     * Does <b>not</b> increment {@link #structural}.
     */
    public void invalidateState() {
        this.state++;
    }
}