package com.otectus.runic_races.power;

import com.otectus.runic_races.ability.AbilityTuning;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

/** Server input reads this owned configuration. It grants no client-authoritative execution. */
public final class RacialAbilityPower extends PowerFactory<AbilityTuning> {
    public RacialAbilityPower() { super(AbilityTuning.CODEC); }
}
