package com.mataflex.clan;

import com.mataflex.ValhallaEchoes;
import com.mataflex.clan.reputation.ClanReputationManager;
import com.mataflex.entity.DraugrEntity;
import com.mataflex.entity.MysticalVikingEntity;
import com.mataflex.sound.CustomSounds;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Eventos del mundo que conectan el combate con la reputación de los clanes vikingos:
 * - Eliminar Draugrs otorga reputación a los clanes testigos (o al clan territorial) y dispara celebraciones.
 * - Eliminar vikingos penaliza fuertemente la reputación con su clan y alerta a sus compañeros.
 */
public final class ClanEvents {

    public static final int DRAUGR_KILL_TERRITORIAL_REPUTATION = 5;
    public static final int DRAUGR_KILL_WITNESSED_REPUTATION = 10;
    public static final int VIKING_HURT_REPUTATION_PENALTY = -5;
    public static final int VIKING_KILL_REPUTATION_PENALTY = -25;

    private static final double DRAUGR_WITNESS_RADIUS = 32.0D;
    private static final double VIKING_KILL_ALERT_RADIUS = 28.0D;
    private static final long CELEBRATION_DURATION_TICKS = 100L;
    private static final long ANGER_DURATION_TICKS = 600L;

    private ClanEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(ClanEvents::onEntityDeath);
        ValhallaEchoes.LOGGER.info("Registered Viking clan reputation events.");
    }

    private static void onEntityDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(damageSource.getEntity() instanceof ServerPlayer killer)) {
            return;
        }

        if (entity instanceof DraugrEntity draugr) {
            handleDraugrKilledByPlayer(serverLevel, draugr, killer);
        } else if (entity instanceof MysticalVikingEntity victim) {
            handleVikingKilledByPlayer(serverLevel, victim, killer);
        }
    }

    private static void handleDraugrKilledByPlayer(
            ServerLevel level,
            DraugrEntity draugr,
            ServerPlayer killer
    ) {
        BlockPos deathPos = draugr.blockPosition();
        AABB witnessBox = draugr.getBoundingBox().inflate(DRAUGR_WITNESS_RADIUS);
        List<MysticalVikingEntity> witnesses = level.getEntitiesOfClass(
                MysticalVikingEntity.class,
                witnessBox,
                LivingEntity::isAlive
        );

        if (!witnesses.isEmpty()) {
            Set<VikingClan> witnessedClans = EnumSet.noneOf(VikingClan.class);
            for (MysticalVikingEntity witness : witnesses) {
                witnessedClans.add(witness.getClan());
                if (!witness.isTemporarilyAngryAt(killer)) {
                    witness.celebrateAt(deathPos, CELEBRATION_DURATION_TICKS);
                }
            }
            for (VikingClan clan : witnessedClans) {
                ClanReputationManager.addGameplayReputation(killer, clan, DRAUGR_KILL_WITNESSED_REPUTATION);
            }
        } else {
            VikingClan territorialClan = ClanVillageManager.getClanForPos(deathPos);
            ClanReputationManager.addGameplayReputation(killer, territorialClan, DRAUGR_KILL_TERRITORIAL_REPUTATION);
        }
    }

    private static void handleVikingKilledByPlayer(
            ServerLevel level,
            MysticalVikingEntity victim,
            ServerPlayer killer
    ) {
        if (killer.isCreative() || killer.isSpectator()) {
            return;
        }

        ClanReputationManager.addGameplayReputation(killer, victim.getClan(), VIKING_KILL_REPUTATION_PENALTY);
        alertClanMembers(level, victim, killer, VIKING_KILL_ALERT_RADIUS);
    }

    /**
     * Alerta a los vikingos cercanos del mismo clan para que defiendan o venguen a un compañero.
     */
    public static void alertClanMembers(
            ServerLevel level,
            MysticalVikingEntity sourceViking,
            LivingEntity attacker,
            double radius
    ) {
        if (attacker instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        VikingClan clan = sourceViking.getClan();
        AABB alertBox = sourceViking.getBoundingBox().inflate(radius);
        List<MysticalVikingEntity> allies = level.getEntitiesOfClass(
                MysticalVikingEntity.class,
                alertBox,
                other -> other != sourceViking
                        && other.isAlive()
                        && !other.isBaby()
                        && other.getClan() == clan
        );

        for (MysticalVikingEntity ally : allies) {
            ally.getMysticalBrain().setMemoryWithExpiry(
                    MemoryModuleType.ANGRY_AT,
                    attacker.getUUID(),
                    ANGER_DURATION_TICKS
            );
            ally.getMysticalBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attacker);
            ally.makeSound(CustomSounds.VIKING_ANGRY);
        }
    }
}
