package com.mataflex.clan;

import com.mataflex.ValhallaEchoes;
import com.mataflex.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Representa los 4 clanes vikingos principales dentro de Valhalla Echoes.
 * Cada aldea generada se asocia a uno de estos clanes según su región,
 * determinando sus estandartes, el molde de telar disponible en sus cofres,
 * sus rivalidades y la reputación del jugador con cada facción.
 */
public enum VikingClan implements StringRepresentable {
    WOLF(
            "wolf",
            DyeColor.WHITE,
            DyeColor.GRAY,
            ChatFormatting.GRAY,
            () -> ModItems.WOLF_BANNER_PATTERN
    ),
    RAVEN(
            "raven",
            DyeColor.WHITE,
            DyeColor.CYAN,
            ChatFormatting.AQUA,
            () -> ModItems.RAVEN_BANNER_PATTERN
    ),
    BEAR(
            "bear",
            DyeColor.WHITE,
            DyeColor.RED,
            ChatFormatting.RED,
            () -> ModItems.BEAR_BANNER_PATTERN
    ),
    SERPENT(
            "serpent",
            DyeColor.WHITE,
            DyeColor.GREEN,
            ChatFormatting.GREEN,
            () -> ModItems.SERPENT_BANNER_PATTERN
    );

    public static final VikingClan[] VALUES = values();
    public static final StringRepresentable.EnumCodec<VikingClan> CODEC = StringRepresentable.fromEnum(VikingClan::values);

    private final String id;
    private final DyeColor baseColor;
    private final DyeColor patternColor;
    private final ChatFormatting chatFormatting;
    private final Supplier<Item> bannerPatternItemSupplier;
    private final ResourceKey<BannerPattern> patternKey;
    private final TagKey<BannerPattern> patternItemTag;

    VikingClan(
            String id,
            DyeColor baseColor,
            DyeColor patternColor,
            ChatFormatting chatFormatting,
            Supplier<Item> bannerPatternItemSupplier
    ) {
        this.id = id;
        this.baseColor = baseColor;
        this.patternColor = patternColor;
        this.chatFormatting = chatFormatting;
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

    @Override
    public @NonNull String getSerializedName() {
        return this.id;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }

    public DyeColor getPatternColor() {
        return this.patternColor;
    }

    public ChatFormatting getChatFormatting() {
        return this.chatFormatting;
    }

    public String getTranslationKey() {
        return "clan." + ValhallaEchoes.MOD_ID + "." + this.id;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable(this.getTranslationKey()).withStyle(this.chatFormatting);
    }

    /**
     * Devuelve el clan rival con el que existe rivalidad cruzada:
     * WOLF <-> SERPENT y BEAR <-> RAVEN.
     */
    public VikingClan getRival() {
        return switch (this) {
            case WOLF -> SERPENT;
            case SERPENT -> WOLF;
            case BEAR -> RAVEN;
            case RAVEN -> BEAR;
        };
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

    /**
     * Busca un clan por su identificador (insensible a mayúsculas).
     */
    public static Optional<VikingClan> fromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (VikingClan clan : VALUES) {
            if (clan.id.equals(normalized)) {
                return Optional.of(clan);
            }
        }
        return Optional.empty();
    }
}
