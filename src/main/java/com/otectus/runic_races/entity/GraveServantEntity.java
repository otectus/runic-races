package com.otectus.runic_races.entity;

import com.otectus.runic_races.action.SummonMinionAction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Short-lived Grave Servant minion summoned by the {@code runic_races:summon_minion} action
 * fired from {@code skeleton/conscript_the_dead.json}. Extends {@link Zombie} so the vanilla
 * zombie renderer and model can be reused without custom art assets.
 *
 * Behavior differences from a vanilla zombie:
 * <ul>
 *   <li>Targets hostile mobs ({@link Enemy}) — never the owner or other players.</li>
 *   <li>Does not burn in sunlight (grave servants are not driven by flesh hunger).</li>
 *   <li>Honors NBT-persisted owner UUID and expiry game-time set by
 *       {@link SummonMinionAction}. On expiry, despawns with a soul-particle puff.</li>
 * </ul>
 */
public class GraveServantEntity extends Zombie {

    public GraveServantEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        setCanPickUpLoot(false);
        setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true,
                e -> !(e instanceof GraveServantEntity)));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    public boolean isSunBurnTick() {
        return false; // Grave servants do not fear daylight.
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return false;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target == null) return false;
        if (isOwner(target)) return false;
        if (target instanceof Player) return false;
        if (target instanceof GraveServantEntity) return false;
        return super.canAttack(target);
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && (isOwner(target) || target instanceof Player || target instanceof GraveServantEntity)) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (level().isClientSide) return;

        CompoundTag data = getPersistentData();
        if (data.contains(SummonMinionAction.EXPIRY_TAG)) {
            long expiresAt = data.getLong(SummonMinionAction.EXPIRY_TAG);
            if (level().getGameTime() >= expiresAt) {
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SOUL,
                            getX(), getY() + 0.6, getZ(),
                            20, 0.4, 0.6, 0.4, 0.05);
                }
                discard();
            }
        }
    }

    private boolean isOwner(LivingEntity entity) {
        CompoundTag data = getPersistentData();
        if (!data.hasUUID(SummonMinionAction.OWNER_UUID_TAG)) return false;
        UUID ownerId = data.getUUID(SummonMinionAction.OWNER_UUID_TAG);
        return entity.getUUID().equals(ownerId);
    }
}
