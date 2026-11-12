package com.mataflex.entity;

import com.mataflex.sound.CustomSounds;
import com.mataflex.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class MysticalVikingEntity extends Piglin {

    private static final Brain.Provider<MysticalVikingEntity> BRAIN_PROVIDER = Brain.provider(
            List.of(
                    MemoryModuleType.UNIVERSAL_ANGER,
                    MemoryModuleType.ATE_RECENTLY,
                    MemoryModuleType.SPEAR_FLEEING_TIME,
                    MemoryModuleType.SPEAR_FLEEING_POSITION,
                    MemoryModuleType.SPEAR_CHARGE_POSITION,
                    MemoryModuleType.SPEAR_ENGAGE_TIME
            ),
            List.of(
                    SensorType.NEAREST_LIVING_ENTITIES,
                    SensorType.NEAREST_PLAYERS,
                    SensorType.NEAREST_ITEMS,
                    SensorType.HURT_BY,
                    SensorType.PIGLIN_SPECIFIC_SENSOR
            ),
            MysticalVikingAi::getActivities
    );

    private Brain<MysticalVikingEntity> mysticalVikingBrain;

    public MysticalVikingEntity(EntityType<? extends Piglin> entityType, Level level) {
        super(entityType, level);
        this.setImmuneToZombification(false);
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource randomSource, net.minecraft.world.@NonNull DifficultyInstance difficultyInstance) {
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(randomSource.nextInt(20) == 0 ? com.mataflex.item.ModItems.MYSTICAL_SWORD : com.mataflex.item.ModItems.MYSTICAL_AXE));

        if (!this.isBaby() && randomSource.nextFloat() < 0.35F) {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.mataflex.item.ModItems.HORNED_HELMET));
        } else {
            this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
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
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("mysticalVikingBrain");

        this.getMysticalBrain().tick(serverLevel, this);

        profilerFiller.pop();

        MysticalVikingAi.updateActivity(this);

        super.customServerAiStep(serverLevel);
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
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor serverLevelAccessor, @NonNull DifficultyInstance difficultyInstance, @NonNull EntitySpawnReason entitySpawnReason, @Nullable SpawnGroupData spawnGroupData) {
        RandomSource randomSource = serverLevelAccessor.getRandom();

        if (entitySpawnReason != EntitySpawnReason.STRUCTURE) {
            if (randomSource.nextFloat() < 0.125F) {
                this.setBaby(true);
            }
        }

        this.populateDefaultEquipmentSlots(randomSource, difficultyInstance);

        return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, entitySpawnReason, spawnGroupData);
    }

}