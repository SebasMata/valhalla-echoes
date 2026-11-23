package com.mataflex.entity;

import com.google.common.collect.ImmutableList;
import com.mataflex.clan.reputation.ClanReputationLevel;
import com.mataflex.clan.reputation.ClanReputationManager;
import com.mataflex.item.ModItems;
import com.mataflex.sound.CustomSounds;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MysticalVikingAi {

    private static final double TARGET_DETECTION_RANGE = 16.0D;
    private static final double TARGET_DISENGAGE_RANGE_SQ = 28.0D * 28.0D;

    // === CONSTRUCCIÓN DEL CEREBRO ===

    public static List<ActivityData<MysticalVikingEntity>> getActivities(MysticalVikingEntity viking) {
        return List.of(
                initCoreActivity(viking),
                initIdleActivity(viking),
                initFightActivity(viking),
                initCelebrateActivity()
        );
    }

    // === ACTIVIDADES ===

    private static ActivityData<MysticalVikingEntity> initCoreActivity(MysticalVikingEntity viking) {
        return ActivityData.create(Activity.CORE, 0, ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                InteractWithDoor.create(),
                StopAttackingIfTargetInvalid.create(
                        (level, target) -> shouldStopAttacking(level, viking, target),
                        (level, mob, target) -> {},
                        true
                )
        ));
    }

    @SuppressWarnings("unchecked")
    private static <T extends BehaviorControl<?>> BehaviorControl<MysticalVikingEntity> cast(T behavior) {
        return (BehaviorControl<MysticalVikingEntity>) behavior;
    }

    private static ActivityData<MysticalVikingEntity> initIdleActivity(MysticalVikingEntity viking) {
        return ActivityData.create(Activity.IDLE, 10, ImmutableList.of(
                cast(SetEntityLookTarget.create(entity -> shouldFeelJealousOf(viking, entity), 14.0F)),
                cast(StartAttacking.create(MysticalVikingAi::findNearestValidAttackTarget)),
                new RunOne<MysticalVikingEntity>(ImmutableList.of(
                        Pair.of(cast(SetEntityLookTarget.create(EntityTypes.PLAYER, 8.0F)), 1),
                        Pair.of(cast(SetEntityLookTarget.create(EntityTypes.PIGLIN, 8.0F)), 1),
                        Pair.of(cast(new DoNothing(30, 60)), 1)
                )),
                new RunOne<MysticalVikingEntity>(ImmutableList.of(
                        Pair.of(cast(RandomStroll.stroll(0.6F)), 2),
                        Pair.of(cast(new DoNothing(30, 60)), 1)
                ))
        ));
    }

    private static ActivityData<MysticalVikingEntity> initFightActivity(MysticalVikingEntity viking) {
        return ActivityData.create(
                Activity.FIGHT,
                10,
                ImmutableList.of(
                        StopAttackingIfTargetInvalid.create(
                                (level, target) -> shouldStopAttacking(level, viking, target),
                                (level, mob, target) -> {},
                                true
                        ),
                        SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2F),
                        MeleeAttack.create(20)
                ),
                MemoryModuleType.ATTACK_TARGET
        );
    }

    private static ActivityData<MysticalVikingEntity> initCelebrateActivity() {
        return ActivityData.create(
                Activity.CELEBRATE,
                10,
                ImmutableList.of(
                        SetEntityLookTarget.create(EntityTypes.PLAYER, 8.0F),
                        new RunOne<>(ImmutableList.of(
                                Pair.of(RandomStroll.stroll(0.6F, 2, 1), 1),
                                Pair.of(new DoNothing(10, 20), 1)
                        ))
                ),
                MemoryModuleType.CELEBRATE_LOCATION
        );
    }

    // === ACTUALIZACIÓN DE ACTIVIDAD ===

    public static void updateActivity(MysticalVikingEntity viking) {
        Brain<MysticalVikingEntity> brain = viking.getMysticalBrain();
        Activity currentActivity = brain.getActiveNonCoreActivity().orElse(null);

        brain.setActiveActivityToFirstValid(ImmutableList.of(
                Activity.FIGHT,
                Activity.CELEBRATE,
                Activity.IDLE
        ));

        Activity newActivity = brain.getActiveNonCoreActivity().orElse(null);

        if (currentActivity != newActivity) {
            getSoundForCurrentActivity(viking).ifPresent(viking::makeSound);
        }

        viking.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
    }

    // === SELECCIÓN DE SONIDOS ===

    public static Optional<SoundEvent> getSoundForCurrentActivity(MysticalVikingEntity viking) {
        return viking.getMysticalBrain().getActiveNonCoreActivity()
                .map(activity -> getSoundForActivity(viking, activity));
    }

    private static SoundEvent getSoundForActivity(MysticalVikingEntity viking, Activity activity) {
        if (activity == Activity.FIGHT) {
            return CustomSounds.VIKING_ANGRY;
        } else if (activity == Activity.CELEBRATE) {
            return CustomSounds.VIKING_CELEBRATE;
        } else if (seesJealousPlayer(viking)) {
            return CustomSounds.VIKING_JEALOUS;
        } else {
            return CustomSounds.VIKING_AMBIENT;
        }
    }

    // === LÓGICA DE COMBATE Y REPUTACIÓN ===

    public static boolean isHoldingOrWearingMythicalGear(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);

        return mainHand.is(ModItems.MYSTICAL_SWORD)
                || offHand.is(ModItems.MYSTICAL_SWORD)
                || mainHand.is(ModItems.MYSTICAL_AXE)
                || offHand.is(ModItems.MYSTICAL_AXE)
                || head.is(ModItems.HORNED_HELMET);
    }

    private static boolean shouldFeelJealousOf(MysticalVikingEntity viking, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        ClanReputationLevel level = ClanReputationManager.getLevel(player, viking.getClan());
        return level.isNeutral() && isHoldingOrWearingMythicalGear(player);
    }

    private static boolean seesJealousPlayer(MysticalVikingEntity viking) {
        Optional<List<Player>> nearbyPlayers = viking.getMysticalBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS);
        if (nearbyPlayers.isEmpty()) {
            return false;
        }
        for (Player player : nearbyPlayers.get()) {
            if (player.distanceToSqr(viking) <= 14.0D * 14.0D
                    && viking.hasLineOfSight(player)
                    && shouldFeelJealousOf(viking, player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldStopAttacking(ServerLevel level, MysticalVikingEntity viking, LivingEntity target) {
        if (target == null || !target.isAlive() || target.level() != level) {
            return true;
        }
        if (viking.distanceToSqr(target) > TARGET_DISENGAGE_RANGE_SQ) {
            return true;
        }
        if (target instanceof MysticalVikingEntity other && other.getClan() == viking.getClan()) {
            return true;
        }
        if (target instanceof ServerPlayer player) {
            if (player.isCreative() || player.isSpectator()) {
                return true;
            }
            ClanReputationLevel standing = ClanReputationManager.getLevel(player, viking.getClan());
            if (!standing.isHostile() && !viking.isTemporarilyAngryAt(player)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidTarget(ServerLevel level, MysticalVikingEntity viking, LivingEntity candidate) {
        if (candidate == null || candidate == viking || !candidate.isAlive() || candidate.level() != level) {
            return false;
        }
        if (candidate instanceof MysticalVikingEntity other && other.getClan() == viking.getClan()) {
            return false;
        }
        if (candidate instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return viking.canAttack(candidate);
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(
            ServerLevel level,
            MysticalVikingEntity viking
    ) {
        if (viking.isBaby()) {
            return Optional.empty();
        }

        Brain<MysticalVikingEntity> brain = viking.getMysticalBrain();

        // 1. Defensa propia directa (quien le haya golpeado recientemente)
        Optional<LivingEntity> hurtBy = brain.getMemory(MemoryModuleType.HURT_BY_ENTITY);
        if (hurtBy.isPresent() && isValidTarget(level, viking, hurtBy.get())
                && viking.distanceToSqr(hurtBy.get()) <= TARGET_DISENGAGE_RANGE_SQ) {
            return hurtBy;
        }

        // 2. Ira temporal (por alerta grupal del clan o provocación reciente)
        Optional<UUID> angryAtId = brain.getMemory(MemoryModuleType.ANGRY_AT);
        if (angryAtId.isPresent()) {
            Entity angryEntity = level.getEntity(angryAtId.get());
            if (angryEntity instanceof LivingEntity living
                    && isValidTarget(level, viking, living)
                    && viking.distanceToSqr(living) <= TARGET_DISENGAGE_RANGE_SQ) {
                return Optional.of(living);
            }
        }

        double rangeSq = TARGET_DETECTION_RANGE * TARGET_DETECTION_RANGE;
        List<Player> nearbyPlayers = brain.getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of());

        // 3. Jugadores HOSTILES a la vista
        ServerPlayer closestHostilePlayer = null;
        double closestHostileDistSq = rangeSq;
        for (Player player : nearbyPlayers) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                continue;
            }
            if (!isValidTarget(level, viking, serverPlayer)) {
                continue;
            }
            double distSq = viking.distanceToSqr(serverPlayer);
            if (distSq <= closestHostileDistSq
                    && viking.hasLineOfSight(serverPlayer)
                    && ClanReputationManager.getLevel(serverPlayer, viking.getClan()).isHostile()) {
                closestHostileDistSq = distSq;
                closestHostilePlayer = serverPlayer;
            }
        }
        if (closestHostilePlayer != null) {
            return Optional.of(closestHostilePlayer);
        }

        Optional<NearestVisibleLivingEntities> visibleEntities =
                brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visibleEntities.isEmpty()) {
            return Optional.empty();
        }

        // 4. Defensa activa de jugadores ALIADOS (atacar monstruos que estén combatiendo a un aliado)
        for (Player player : nearbyPlayers) {
            if (!(player instanceof ServerPlayer ally) || !ally.isAlive() || ally.isSpectator()) {
                continue;
            }
            if (viking.distanceToSqr(ally) > rangeSq) {
                continue;
            }
            if (!ClanReputationManager.getLevel(ally, viking.getClan()).isAlly()) {
                continue;
            }

            Optional<LivingEntity> threatToAlly = visibleEntities.get().findClosest(candidate -> {
                if (!(candidate instanceof Monster monster)) {
                    return false;
                }
                if (!isValidTarget(level, viking, monster) || viking.distanceToSqr(monster) > rangeSq) {
                    return false;
                }
                return monster.getTarget() == ally
                        || ally.getLastHurtByMob() == monster
                        || ally.getLastHurtMob() == monster;
            });
            if (threatToAlly.isPresent()) {
                return threatToAlly;
            }
        }

        // 5. Enemigo natural: Draugr cercanos (los vikingos protegen su territorio y aldeas)
        return visibleEntities.get().findClosest(candidate ->
                candidate instanceof DraugrEntity
                        && isValidTarget(level, viking, candidate)
                        && viking.distanceToSqr(candidate) <= rangeSq
        );
    }
}