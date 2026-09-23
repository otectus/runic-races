package com.otectus.runic_races.ability;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/** Magic takes precedence over physical/projectile affinity; explosion/fire remain independent. */
public final class DamagePolicy {
    private DamagePolicy() {}
    public static boolean bypass(DamageSource s) { return s.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || s.is(DamageTypeTags.BYPASSES_EFFECTS); }
    public static boolean magic(DamageSource s) { return !bypass(s) && s.is(RacialTags.MAGIC); }
    public static boolean projectile(DamageSource s) { return s.is(DamageTypeTags.IS_PROJECTILE); }
    public static boolean physical(DamageSource s) { return !bypass(s) && !magic(s) && (projectile(s) || s.is(RacialTags.PHYSICAL)); }
    public static boolean wardEligible(DamageSource s) {
        return !bypass(s) && (physical(s) || magic(s) || s.is(DamageTypeTags.IS_EXPLOSION) || s.is(DamageTypeTags.IS_FIRE));
    }
    public static boolean internal(DamageSource s) { return s instanceof RacialDamageSource; }
}
