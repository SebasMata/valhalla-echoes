package com.mataflex.clan;

import com.mataflex.clan.banner.ClanBannerHelper;
import com.mataflex.clan.reputation.ClanReputationLevel;
import com.mataflex.entity.MysticalVikingEntity;
import com.mataflex.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalInt;

/**
 * Define las ofrendas aceptadas por los vikingos (generales y predilectas por clan)
 * y las recompensas recíprocas otorgadas a jugadores Amistosos y Aliados.
 */
public final class ClanOfferings {

    private ClanOfferings() {
    }

    /**
     * Devuelve la reputación que otorga entregar {@code stack} a un vikingo de {@code clan},
     * o vacío si el ítem no es una ofrenda válida.
     */
    public static OptionalInt getOfferingReputation(VikingClan clan, ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalInt.empty();
        }
        Item item = stack.getItem();

        // 1. Ofrendas predilectas de cada clan (identidad cultural)
        int preferredBonus = switch (clan) {
            case WOLF -> {
                if (item == Items.COOKED_MUTTON || item == Items.COOKED_BEEF) yield 6;
                if (item == Items.BONE) yield 3;
                yield 0;
            }
            case RAVEN -> {
                if (item == Items.ENDER_PEARL) yield 8;
                if (item == Items.AMETHYST_SHARD) yield 6;
                if (item == Items.FEATHER) yield 3;
                yield 0;
            }
            case BEAR -> {
                if (item == Items.HONEY_BOTTLE || item == Items.COOKED_SALMON) yield 6;
                if (item == Items.SWEET_BERRIES) yield 3;
                yield 0;
            }
            case SERPENT -> {
                if (item == Items.NAUTILUS_SHELL) yield 8;
                if (item == Items.PRISMARINE_SHARD || item == Items.EMERALD) yield 6;
                yield 0;
            }
        };
        if (preferredBonus > 0) {
            return OptionalInt.of(preferredBonus);
        }

        // 2. Ofrendas generales apreciadas por todos los clanes
        if (item == ModItems.RUNIC_GEM) {
            return OptionalInt.of(15);
        }
        if (item == ModItems.MYSTICAL_INGOT) {
            return OptionalInt.of(12);
        }
        if (item == ModItems.MYSTICAL_ESSENCE) {
            return OptionalInt.of(10);
        }
        if (item == Items.GOLD_INGOT || item == Items.EMERALD) {
            return OptionalInt.of(4);
        }

        return OptionalInt.empty();
    }

    /**
     * Otorga una recompensa o bendición de clan al jugador cuando entrega una ofrenda
     * con reputación {@link ClanReputationLevel#FRIENDLY} o {@link ClanReputationLevel#ALLY}.
     */
    public static void grantReciprocalGift(
            ServerLevel level,
            MysticalVikingEntity viking,
            ServerPlayer player,
            ClanReputationLevel levelAfter
    ) {
        if (!levelAfter.isAtLeastFriendly()) {
            return;
        }

        RandomSource random = viking.getRandom();
        VikingClan clan = viking.getClan();

        if (levelAfter.isAlly()) {
            // Bendición de batalla de los aliados (45s)
            player.addEffect(new MobEffectInstance(
                    random.nextBoolean() ? MobEffects.STRENGTH : MobEffects.REGENERATION,
                    45 * 20,
                    0
            ));

            if (random.nextFloat() < 0.75F) {
                ItemStack gift = rollAllyGift(clan, level, random);
                tossGiftTowardsPlayer(level, viking, player, gift);
            }
        } else if (random.nextFloat() < 0.45F) {
            ItemStack gift = rollFriendlyGift(clan, random);
            tossGiftTowardsPlayer(level, viking, player, gift);
        }
    }

    private static ItemStack rollFriendlyGift(VikingClan clan, RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.08F) {
            return new ItemStack(clan.getBannerPatternItem());
        }
        if (roll < 0.35F) {
            return new ItemStack(Items.IRON_INGOT, random.nextIntBetweenInclusive(1, 2));
        }
        if (roll < 0.65F) {
            return new ItemStack(Items.GOLD_NUGGET, random.nextIntBetweenInclusive(3, 6));
        }
        return new ItemStack(Items.COOKED_SALMON, random.nextIntBetweenInclusive(1, 2));
    }

    private static ItemStack rollAllyGift(VikingClan clan, ServerLevel level, RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.15F) {
            return new ItemStack(clan.getBannerPatternItem());
        }
        if (roll < 0.25F) {
            return ClanBannerHelper.createClanBannerStack(clan, level.registryAccess());
        }
        if (roll < 0.42F) {
            return new ItemStack(ModItems.MYSTICAL_ESSENCE, 1);
        }
        if (roll < 0.72F) {
            return new ItemStack(Items.GOLD_INGOT, random.nextIntBetweenInclusive(1, 2));
        }
        return new ItemStack(Items.IRON_INGOT, random.nextIntBetweenInclusive(2, 4));
    }

    private static void tossGiftTowardsPlayer(
            ServerLevel level,
            MysticalVikingEntity viking,
            ServerPlayer player,
            ItemStack gift
    ) {
        if (gift.isEmpty()) {
            return;
        }
        ItemEntity dropped = new ItemEntity(
                level,
                viking.getX(),
                viking.getEyeY() - 0.3,
                viking.getZ(),
                gift
        );
        Vec3 direction = player.position().subtract(viking.position()).normalize();
        dropped.setDeltaMovement(direction.x * 0.25, 0.22, direction.z * 0.25);
        dropped.setDefaultPickUpDelay();
        level.addFreshEntity(dropped);
    }
}
