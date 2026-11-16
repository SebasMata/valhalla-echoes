package com.mataflex.clan;

import com.mataflex.ValhallaEchoes;
import com.mataflex.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.function.Supplier;

/**
 * Representa los 4 clanes vikingos principales dentro de Valhalla Echoes.
 * Cada aldea generada se asocia a uno de estos clanes según su región,
 * determinando sus estandartes y el molde de telar disponible en sus cofres.
 */
public enum VikingClan {
    WOLF(
            "wolf",
            DyeColor.WHITE,
            DyeColor.GRAY,
            () -> ModItems.WOLF_BANNER_PATTERN
    ),
    RAVEN(
            "raven",
            DyeColor.WHITE,
            DyeColor.CYAN,
            () -> ModItems.RAVEN_BANNER_PATTERN
    ),
    BEAR(
            "bear",
            DyeColor.WHITE,
            DyeColor.RED,
            () -> ModItems.BEAR_BANNER_PATTERN
    ),
    SERPENT(
            "serpent",
            DyeColor.WHITE,
            DyeColor.GREEN,
            () -> ModItems.SERPENT_BANNER_PATTERN
    );

    public static final VikingClan[] VALUES = values();

    private final String id;
    private final DyeColor baseColor;
    private final DyeColor patternColor;
    private final Supplier<Item> bannerPatternItemSupplier;
    private final ResourceKey<BannerPattern> patternKey;
    private final TagKey<BannerPattern> patternItemTag;

    VikingClan(
            String id,
            DyeColor baseColor,
            DyeColor patternColor,
            Supplier<Item> bannerPatternItemSupplier
    ) {
        this.id = id;
        this.baseColor = baseColor;
        this.patternColor = patternColor;
        this.bannerPatternItemSupplier = bannerPatternItemSupplier;
        this.patternKey = ResourceKey.create(
                Registries.BANNER_PATTERN,
                Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, id)
        );
        this.patternItemTag = TagKey.create(
                Registries.BANNER_PATTERN,
                Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, "pattern_item/" + id)
        );
    }

    public String getId() {
        return this.id;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }

    public DyeColor getPatternColor() {
        return this.patternColor;
    }

    public Block getStandingBanner() {
        return Blocks.BANNER.pick(this.baseColor);
    }

    public Block getWallBanner() {
        return Blocks.WALL_BANNER.pick(this.baseColor);
    }

    public Item getBannerPatternItem() {
        return this.bannerPatternItemSupplier.get();
    }

    public ResourceKey<BannerPattern> getPatternKey() {
        return this.patternKey;
    }

    public TagKey<BannerPattern> getPatternItemTag() {
        return this.patternItemTag;
    }
}
