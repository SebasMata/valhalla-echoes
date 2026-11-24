package com.mataflex.entity;

import com.mataflex.clan.ClanEvents;
import com.mataflex.clan.ClanMember;
import com.mataflex.clan.ClanOfferings;
import com.mataflex.clan.ClanVillageManager;
import com.mataflex.clan.VikingClan;
import com.mataflex.clan.banner.ClanBannerHelper;
import com.mataflex.clan.reputation.ClanReputationLevel;
import com.mataflex.clan.reputation.ClanReputationManager;
import com.mataflex.item.ModItems;
import com.mataflex.sound.CustomSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public class MysticalVikingEntity extends Piglin implements ClanMember {

    private static final EntityDataAccessor<String> DATA_CLAN = SynchedEntityData.defineId(
            MysticalVikingEntity.class,
            EntityDataSerializers.STRING
    );

    private static final String TAG_CLAN = "VikingClan";
    private static final String TAG_CLAN_INITIALIZED = "ClanInitialized";

    private static final int OFFERING_COOLDOWN_TICKS = 60;
    private static final long HURT_PENALTY_COOLDOWN_TICKS = 40L;
    private static final long PERSONAL_ANGER_TICKS = 600L;
    private static final double CLAN_HURT_ALERT_RADIUS = 24.0D;

    @SuppressWarnings("deprecation")
    private static final Brain.Provider<MysticalVikingEntity> BRAIN_PROVIDER = Brain.provider(
            List.of(
                    MemoryModuleType.UNIVERSAL_ANGER,
                    MemoryModuleType.ANGRY_AT,
                    MemoryModuleType.ATTACK_TARGET,
                    MemoryModuleType.HURT_BY,
                    MemoryModuleType.HURT_BY_ENTITY,
                    MemoryModuleType.CELEBRATE_LOCATION,
                    MemoryModuleType.NEAREST_LIVING_ENTITIES,
                    MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                    MemoryModuleType.NEAREST_PLAYERS,
                    MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                    MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                    MemoryModuleType.WALK_TARGET,
                    MemoryModuleType.LOOK_TARGET,
                    MemoryModuleType.PATH,
                    MemoryModuleType.INTERACTION_TARGET
            ),
            List.of(
                    SensorType.NEAREST_LIVING_ENTITIES,
                    SensorType.NEAREST_PLAYERS,
                    SensorType.HURT_BY
            ),
            MysticalVikingAi::getActivities
    );

    private Brain<MysticalVikingEntity> mysticalVikingBrain;
    private boolean clanInitialized = false;
    private int offeringCooldownTicks = 0;
    private final Map<UUID, Long> lastHurtPenaltyTicks = new HashMap<>();

    public MysticalVikingEntity(EntityType<? extends Piglin> entityType, Level level) {
        super(entityType, level);
        this.setImmuneToZombification(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NonNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CLAN, VikingClan.WOLF.getId());
    }

    @Override
    public VikingClan getClan() {
        return VikingClan.fromId(this.entityData.get(DATA_CLAN)).orElse(VikingClan.WOLF);
    }

    @Override
    public void setClan(VikingClan clan) {
        VikingClan safeClan = clan != null ? clan : VikingClan.WOLF;
        this.entityData.set(DATA_CLAN, safeClan.getId());
        this.clanInitialized = true;
    }

    /**
     * Equipa al vikingo adulto con el escudo Targe 3D de su clan actual en la mano secundaria.
     */
    public void equipClanShield() {
        if (this.isBaby()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            return;
        }
        ItemStack shield = ClanBannerHelper.createClanTargeShield(this.getClan(), this.registryAccess());
        this.setItemSlot(EquipmentSlot.OFFHAND, shield);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.085F);
    }

    /**
     * Activa una breve celebración del vikingo en la posición indicada.
     */
    public void celebrateAt(BlockPos pos, long durationTicks) {
        this.getMysticalBrain().setMemoryWithExpiry(MemoryModuleType.CELEBRATE_LOCATION, pos, durationTicks);
    }

    /**
     * Comprueba si este vikingo está temporalmente enfadado con la entidad dada (por provocación reciente).
     */
    public boolean isTemporarilyAngryAt(LivingEntity entity) {
        Optional<UUID> angryAt = this.getMysticalBrain().getMemory(MemoryModuleType.ANGRY_AT);
        return angryAt.isPresent() && angryAt.get().equals(entity.getUUID());
    }

    @Override
    protected void addAdditionalSaveData(@NonNull ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString(TAG_CLAN, this.getClan().getId());
        output.putBoolean(TAG_CLAN_INITIALIZED, this.clanInitialized);
    }

    @Override
    protected void readAdditionalSaveData(@NonNull ValueInput input) {
        super.readAdditionalSaveData(input);
        this.clanInitialized = input.getBooleanOr(TAG_CLAN_INITIALIZED, false);
        input.getString(TAG_CLAN).flatMap(VikingClan::fromId).ifPresent(clan -> {
            this.entityData.set(DATA_CLAN, clan.getId());
            this.clanInitialized = true;
        });
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource randomSource, @NonNull DifficultyInstance difficultyInstance) {
        this.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(randomSource.nextInt(20) == 0 ? ModItems.MYSTICAL_SWORD : ModItems.MYSTICAL_AXE)
        );

        if (!this.isBaby()) {
            if (randomSource.nextFloat() < 0.75F) {
                this.equipClanShield();
            } else {
                this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }

            float headRoll = randomSource.nextFloat();
            if (headRoll < 0.35F) {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HORNED_HELMET));
            } else if (headRoll < 0.47F) {
                // Portaestandarte del clan (12% de probabilidad cuando no lleva casco con cuernos)
                this.setItemSlot(
                        EquipmentSlot.HEAD,
                        ClanBannerHelper.createClanBannerStack(this.getClan(), this.registryAccess())
                );
                this.setDropChance(EquipmentSlot.HEAD, 0.2F);
            } else {
                this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            }
        } else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    @Override
    public void checkDespawn() {
        super.checkDespawn();
        if (!this.level().isClientSide() && !this.isImmuneToZombification()) {
            this.setImmuneToZombification(true);
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NonNull ServerLevel serverLevel, @NonNull DamageSource damageSource, boolean bl) {
        super.dropCustomDeathLoot(serverLevel, damageSource, bl);

        if (this.random.nextFloat() < 0.10f) {
            this.spawnAtLocation(serverLevel, new ItemStack(ModItems.MYSTICAL_ESSENCE, 1));
        } else if (this.random.nextFloat() < 0.65f) {
            this.spawnAtLocation(serverLevel, new ItemStack(Items.BONE, 3));
        } else {
            this.spawnAtLocation(serverLevel, new ItemStack(Items.IRON_INGOT, 1));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected @NonNull Brain<Piglin> makeBrain(Brain.@NonNull Packed packedBrain) {
        Brain<MysticalVikingEntity> vikingBrain = BRAIN_PROVIDER.makeBrain(this, packedBrain);
        this.mysticalVikingBrain = vikingBrain;
        return (Brain<Piglin>) (Brain<?>) vikingBrain;
    }

    @Override
    protected void customServerAiStep(@NonNull ServerLevel serverLevel) {
        // No invocamos super.customServerAiStep() porque Piglin.customServerAiStep() volvería a
        // ejecutar Brain.tick() y PiglinAi.updateActivity(), pisando la IA basada en reputación de clanes.
        if (this.offeringCooldownTicks > 0) {
            this.offeringCooldownTicks--;
        }

        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("mysticalVikingBrain");
        this.getMysticalBrain().tick(serverLevel, this);
        profilerFiller.pop();

        MysticalVikingAi.updateActivity(this);
    }

    @Override
    public boolean hurtServer(@NonNull ServerLevel serverLevel, @NonNull DamageSource damageSource, float amount) {
        boolean hurt = super.hurtServer(serverLevel, damageSource, amount);
        if (!hurt) {
            return false;
        }

        if (damageSource.getEntity() instanceof LivingEntity attacker) {
            // Evitar fuego amigo entre miembros del mismo clan
            if (attacker instanceof MysticalVikingEntity other && other.getClan() == this.getClan()) {
                return true;
            }

            this.getMysticalBrain().eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);

            if (attacker instanceof ServerPlayer player && !player.isCreative() && !player.isSpectator()) {
                long gameTime = serverLevel.getGameTime();
                long lastPenalty = this.lastHurtPenaltyTicks.getOrDefault(player.getUUID(), Long.MIN_VALUE);
                if (gameTime - lastPenalty >= HURT_PENALTY_COOLDOWN_TICKS) {
                    this.lastHurtPenaltyTicks.put(player.getUUID(), gameTime);
                    ClanReputationManager.addGameplayReputation(
                            player,
                            this.getClan(),
                            ClanEvents.VIKING_HURT_REPUTATION_PENALTY
                    );
                }

                this.getMysticalBrain().setMemoryWithExpiry(
                        MemoryModuleType.ANGRY_AT,
                        player.getUUID(),
                        PERSONAL_ANGER_TICKS
                );
                if (!this.isBaby()) {
                    this.getMysticalBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
                }
                ClanEvents.alertClanMembers(serverLevel, this, player, CLAN_HURT_ALERT_RADIUS);
            } else if (!this.isBaby()) {
                this.getMysticalBrain().setMemory(MemoryModuleType.ATTACK_TARGET, attacker);
            }
        }

        return true;
    }

    @Override
    public @NonNull InteractionResult mobInteract(@NonNull Player player, @NonNull InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        VikingClan clan = this.getClan();
        OptionalInt offeringRep = ClanOfferings.getOfferingReputation(clan, heldStack);

        if (this.level().isClientSide()) {
            if (offeringRep.isPresent() || (hand == InteractionHand.MAIN_HAND && heldStack.isEmpty())) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ServerLevel serverLevel = serverPlayer.level();
        ClanReputationLevel standing = ClanReputationManager.getLevel(serverPlayer, clan);

        // Si el jugador es hostil o provocó recientemente a este vikingo, rechaza toda interacción
        if (standing.isHostile() || this.isTemporarilyAngryAt(serverPlayer)) {
            if (offeringRep.isPresent() || hand == InteractionHand.MAIN_HAND) {
                this.makeSound(CustomSounds.VIKING_ANGRY);
                this.spawnReactionParticles(serverLevel, ParticleTypes.ANGRY_VILLAGER, 5);
                serverPlayer.sendOverlayMessage(Component.translatable(
                        "entity.valhalla-echoes.mystical_viking.interact.hostile",
                        clan.getDisplayName()
                ).withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // Entrega de ofrenda
        if (offeringRep.isPresent()) {
            if (this.offeringCooldownTicks > 0) {
                serverPlayer.sendOverlayMessage(Component.translatable(
                        "entity.valhalla-echoes.mystical_viking.interact.busy",
                        clan.getDisplayName()
                ).withStyle(ChatFormatting.YELLOW));
                return InteractionResult.SUCCESS;
            }

            if (!serverPlayer.isCreative()) {
                heldStack.shrink(1);
            }

            this.offeringCooldownTicks = OFFERING_COOLDOWN_TICKS;
            var outcome = ClanReputationManager.addGameplayReputation(
                    serverPlayer,
                    clan,
                    offeringRep.getAsInt()
            );
            ClanReputationLevel levelAfter = outcome.primary().newLevel();

            this.spawnReactionParticles(serverLevel, ParticleTypes.HAPPY_VILLAGER, 7);
            this.celebrateAt(this.blockPosition(), 60L);
            this.makeSound(levelAfter.isAtLeastFriendly()
                    ? CustomSounds.VIKING_CELEBRATE
                    : CustomSounds.VIKING_ADMIRING);

            ClanOfferings.grantReciprocalGift(serverLevel, this, serverPlayer, levelAfter);
            return InteractionResult.SUCCESS;
        }

        // Saludo con mano vacía (solo mano principal)
        if (hand == InteractionHand.MAIN_HAND && heldStack.isEmpty()) {
            String key = switch (standing) {
                case ALLY -> "entity.valhalla-echoes.mystical_viking.interact.ally";
                case FRIENDLY -> "entity.valhalla-echoes.mystical_viking.interact.friendly";
                default -> "entity.valhalla-echoes.mystical_viking.interact.neutral";
            };
            serverPlayer.sendOverlayMessage(Component.translatable(
                    key,
                    clan.getDisplayName(),
                    standing.getDisplayName()
            ));

            boolean jealous = standing.isNeutral() && MysticalVikingAi.isHoldingOrWearingMythicalGear(serverPlayer);
            this.makeSound(jealous ? CustomSounds.VIKING_JEALOUS : CustomSounds.VIKING_AMBIENT);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private void spawnReactionParticles(ServerLevel level, ParticleOptions particle, int count) {
        level.sendParticles(
                particle,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.85,
                this.getZ(),
                count,
                0.35,
                0.35,
                0.35,
                0.02
        );
    }

    @Override
    public boolean wantsToPickUp(@NonNull ServerLevel serverLevel, @NonNull ItemStack stack) {
        // Evita que los vikingos recojan oro del suelo como Piglins vanilla
        return false;
    }

    @Override
    protected void pickUpItem(@NonNull ServerLevel serverLevel, @NonNull ItemEntity itemEntity) {
        // Desactivado intencionalmente: las interacciones con ítems se realizan mediante ofrendas
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (this.level().isClientSide()) return null;

        return MysticalVikingAi.getSoundForCurrentActivity(this).orElse(null);
    }

    @Override
    protected @NonNull SoundEvent getHurtSound(@NonNull DamageSource damageSource) {
        return CustomSounds.VIKING_GRUNT;
    }

    @Override
    protected @NonNull SoundEvent getDeathSound() {
        return CustomSounds.VIKING_DEATH;
    }

    @Override
    protected void playStepSound(@NonNull BlockPos pos, @NonNull BlockState state) {
        this.playSound(SoundEvents.PIGLIN_STEP, 0.3F, 0.7F);
    }

    public Brain<MysticalVikingEntity> getMysticalBrain() {
        if (this.mysticalVikingBrain == null) {
            @SuppressWarnings("unchecked")
            Brain<MysticalVikingEntity> brain = (Brain<MysticalVikingEntity>) (Brain<?>) super.getBrain();
            this.mysticalVikingBrain = brain;
        }
        return this.mysticalVikingBrain;
    }

    public static boolean checkVikingSpawnRules(EntityType<? extends Monster> type, LevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        return Monster.checkAnyLightMonsterSpawnRules(type, world, spawnReason, pos, random);
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader world) {
        return world.isUnobstructed(this);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
        // Los vikingos místicos son siempre adultos: el modelo bebé de Piglin en 26.3 usa UVs incompatibles con mystical_viking.png
        super.setBaby(false);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor serverLevelAccessor,
            @NonNull DifficultyInstance difficultyInstance,
            @NonNull EntitySpawnReason entitySpawnReason,
            @Nullable SpawnGroupData spawnGroupData
    ) {
        if (!this.clanInitialized) {
            this.setClan(ClanVillageManager.getClanForPos(this.blockPosition()));
        }

        SpawnGroupData result = super.finalizeSpawn(serverLevelAccessor, difficultyInstance, entitySpawnReason, spawnGroupData);

        // Garantizar siempre variante adulta incluso si Piglin.finalizeSpawn intenta marcarlo como bebé
        this.setBaby(false);

        // Los vikingos generados dentro de aldeas siempre portan el escudo Targe 3D de su clan
        if (entitySpawnReason == EntitySpawnReason.STRUCTURE) {
            this.equipClanShield();
        }

        return result;
    }
}