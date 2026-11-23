package com.mataflex.clan.reputation;

import com.mataflex.ValhallaEchoes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Niveles de relación entre un jugador y un clan vikingo.
 * El rango general de reputación es [-100, +150], iniciando en 0 (NEUTRAL).
 */
public enum ClanReputationLevel {
    HOSTILE("hostile", ClanReputationData.MIN_REPUTATION, ChatFormatting.RED),
    NEUTRAL("neutral", -49, ChatFormatting.YELLOW),
    FRIENDLY("friendly", 25, ChatFormatting.GREEN),
    ALLY("ally", 100, ChatFormatting.AQUA);

    public static final ClanReputationLevel[] VALUES = values();

    private final String id;
    private final int minReputation;
    private final ChatFormatting chatFormatting;

    ClanReputationLevel(String id, int minReputation, ChatFormatting chatFormatting) {
        this.id = id;
        this.minReputation = minReputation;
        this.chatFormatting = chatFormatting;
    }

    public String getId() {
        return this.id;
    }

    public int getMinReputation() {
        return this.minReputation;
    }

    public ChatFormatting getChatFormatting() {
        return this.chatFormatting;
    }

    public String getTranslationKey() {
        return "clan_reputation." + ValhallaEchoes.MOD_ID + ".level." + this.id;
    }

    public MutableComponent getDisplayName() {
        return Component.translatable(this.getTranslationKey()).withStyle(this.chatFormatting);
    }

    public boolean isHostile() {
        return this == HOSTILE;
    }

    public boolean isNeutral() {
        return this == NEUTRAL;
    }

    public boolean isAtLeastFriendly() {
        return this == FRIENDLY || this == ALLY;
    }

    public boolean isAlly() {
        return this == ALLY;
    }

    /**
     * Determina el nivel de relación correspondiente a una puntuación numérica de reputación.
     */
    public static ClanReputationLevel fromReputation(int reputation) {
        if (reputation >= ALLY.minReputation) {
            return ALLY;
        }
        if (reputation >= FRIENDLY.minReputation) {
            return FRIENDLY;
        }
        if (reputation >= NEUTRAL.minReputation) {
            return NEUTRAL;
        }
        return HOSTILE;
    }
}
