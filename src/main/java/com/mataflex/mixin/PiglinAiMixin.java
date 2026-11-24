package com.mataflex.mixin;

import com.mataflex.entity.MysticalVikingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Desacopla la IA vanilla de {@link PiglinAi} cuando el sujeto es un {@link MysticalVikingEntity}.
 * Evita lecturas de memorias no registradas (como {@code nearby_adult_piglins} o
 * {@code nearest_visible_attackable_player}), evita que pierdan su escudo Targe al recibir daño
 * y evita que abrir cofres o picar oro dispare represalias vanilla sobre vikingos.
 */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @Inject(method = "wasHurtBy", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingHurt(
            ServerLevel level,
            Piglin piglin,
            LivingEntity attacker,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "maybeRetaliate", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingRetaliate(
            ServerLevel level,
            AbstractPiglin piglin,
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "broadcastAngerTarget", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingBroadcastAnger(
            ServerLevel level,
            AbstractPiglin piglin,
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "setAngerTarget", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingSetAngerTarget(
            ServerLevel level,
            AbstractPiglin piglin,
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "setAngerTargetToNearestTargetablePlayerIfFound", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingUniversalAnger(
            ServerLevel level,
            AbstractPiglin piglin,
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "initMemories", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingInitMemories(
            Piglin piglin,
            RandomSource random,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }

    @Inject(method = "cancelAdmiring", at = @At("HEAD"), cancellable = true)
    private static void valhalla$ignoreVikingCancelAdmiring(
            ServerLevel level,
            Piglin piglin,
            CallbackInfo ci
    ) {
        if (piglin instanceof MysticalVikingEntity) {
            ci.cancel();
        }
    }
}
