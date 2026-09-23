package com.otectus.runic_races.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** No blocks or entities are created. The owner holds at most one field and three recipient ledgers. */
public final class RecoveryField {
    private static final class Recipient { float offered, restored; boolean cleansed; }
    private final Vec3 center;
    private final AbilityTuning tuning;
    private final long expires;
    private long nextPulse;
    private final Map<UUID, Recipient> recipients = new LinkedHashMap<>();
    public RecoveryField(ServerPlayer owner, AbilityTuning tuning, long now) {
        center = owner.position(); this.tuning = tuning; expires = now + tuning.durationTicks();
        nextPulse = now + Math.max(1, tuning.ticks("interval"));
        recipients.put(owner.getUUID(), new Recipient());
    }
    public Vec3 center() { return center; }
    public boolean tick(ServerPlayer owner, long now, Map<UUID, Long> allowance) {
        if (now > expires || !owner.level().hasChunkAt(net.minecraft.core.BlockPos.containing(center))) return false;
        if (now < nextPulse) return true;
        nextPulse += Math.max(1, tuning.ticks("interval"));
        var candidates = owner.level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(tuning.number("radius")),
                        e -> TargetPolicy.support(owner, e) && e.distanceToSqr(center) <= Math.pow(tuning.number("radius"), 2)
                                && TargetPolicy.visible(owner, center.add(0, 0.5, 0), e.getBoundingBox().getCenter()))
                .stream().sorted(Comparator.<LivingEntity>comparingDouble(e -> e.getHealth() / e.getMaxHealth())
                        .thenComparingDouble(e -> e.distanceToSqr(center)).thenComparing(LivingEntity::getUUID)).toList();
        for (LivingEntity e : candidates) {
            if (!recipients.containsKey(e.getUUID()) && recipients.size() >= tuning.ticks("targets")) continue;
            Recipient r = recipients.computeIfAbsent(e.getUUID(), k -> new Recipient());
            if (r.offered >= tuning.amount("budget") || r.restored >= tuning.amount("final_cap")
                    || allowance.getOrDefault(e.getUUID(), 0L) > now) continue;
            boolean poison = !r.cleansed && e.hasEffect(MobEffects.POISON);
            if (e.getHealth() >= e.getMaxHealth() && !poison) continue;
            if (!RacialTargetEvent.allowed(owner, e, RacialTargetEvent.Action.SUPPORT)) continue;
            float offer = Math.min(tuning.amount("healing"), tuning.amount("budget") - r.offered);
            float restored = HealingBudget.heal(e, offer, tuning.amount("final_cap") - r.restored);
            boolean removed = poison && e.removeEffect(MobEffects.POISON);
            if (restored <= 0 && !removed) continue;
            r.cleansed = true; r.offered += offer; r.restored += restored;
            allowance.put(e.getUUID(), now + Math.max(40, tuning.ticks("interval")));
            AbilityFeedback.healing(owner, e, AbilityKind.MOSS_ONE);
        }
        return now < expires;
    }
}
