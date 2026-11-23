package com.mataflex.clan.reputation;

import com.mataflex.clan.VikingClan;
import com.mataflex.clan.reputation.ClanReputationData.ReputationChangeResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.UUID;

/**
 * Fachada centralizada para consultar y modificar la reputación de jugadores con los clanes vikingos.
 * Gestiona además la rivalidad cruzada leve (WOLF <-> SERPENT y BEAR <-> RAVEN) para eventos de juego
 * y las notificaciones visuales en Action Bar y chat.
 */
public final class ClanReputationManager {

    private static final int RIVALRY_MIN_GAIN_THRESHOLD = 4;

    private ClanReputationManager() {
    }

    public static int getReputation(ServerLevel level, UUID playerId, VikingClan clan) {
        return ClanReputationData.get(level).getReputation(playerId, clan);
    }

    public static int getReputation(ServerPlayer player, VikingClan clan) {
        return getReputation(player.level(), player.getUUID(), clan);
    }

    public static ClanReputationLevel getLevel(ServerLevel level, UUID playerId, VikingClan clan) {
        return ClanReputationData.get(level).getLevel(playerId, clan);
    }

    public static ClanReputationLevel getLevel(ServerPlayer player, VikingClan clan) {
        return getLevel(player.level(), player.getUUID(), clan);
    }

    public static EnumMap<VikingClan, Integer> getAllReputations(ServerLevel level, UUID playerId) {
        return ClanReputationData.get(level).getAllReputations(playerId);
    }

    /**
     * Calcula la penalización por rivalidad cruzada para una ganancia positiva de reputación:
     * aproximadamente un 25% de la ganancia (mínimo -1 si la ganancia es >= 4).
     */
    public static int calculateRivalPenalty(int positiveDelta) {
        if (positiveDelta < RIVALRY_MIN_GAIN_THRESHOLD) {
            return 0;
        }
        return -Math.max(1, positiveDelta / 4);
    }

    /**
     * Aplica un cambio de reputación originado por un evento de juego (combate, ofrendas, etc.).
     * Incluye rivalidad cruzada cuando {@code delta > 0} y notifica al jugador.
     */
    public static ReputationUpdateOutcome addGameplayReputation(ServerPlayer player, VikingClan clan, int delta) {
        return addReputation(player, clan, delta, true, true);
    }

    /**
     * Modifica la reputación de un jugador con control explícito sobre rivalidad cruzada y notificaciones.
     * Los comandos administrativos usan {@code applyRivalry = false}.
     */
    public static ReputationUpdateOutcome addReputation(
            ServerPlayer player,
            VikingClan clan,
            int delta,
            boolean applyRivalry,
            boolean notifyPlayer
    ) {
        ClanReputationData data = ClanReputationData.get(player.level());
        ReputationChangeResult primary = data.addReputation(player.getUUID(), clan, delta);

        ReputationChangeResult rivalResult = null;
        if (applyRivalry && delta > 0) {
            int rivalPenalty = calculateRivalPenalty(delta);
            if (rivalPenalty < 0) {
                VikingClan rivalClan = clan.getRival();
                rivalResult = data.addReputation(player.getUUID(), rivalClan, rivalPenalty);
            }
        }

        ReputationUpdateOutcome outcome = new ReputationUpdateOutcome(primary, rivalResult);
        if (notifyPlayer) {
            notifyReputationOutcome(player, outcome);
        }
        return outcome;
    }

    /**
     * Establece una puntuación exacta de reputación sin aplicar rivalidad cruzada.
     */
    public static ReputationChangeResult setReputation(
            ServerPlayer player,
            VikingClan clan,
            int value,
            boolean notifyPlayer
    ) {
        ClanReputationData data = ClanReputationData.get(player.level());
        ReputationChangeResult result = data.setReputation(player.getUUID(), clan, value);
        if (notifyPlayer && result.changed()) {
            notifyReputationOutcome(player, new ReputationUpdateOutcome(result, null));
        }
        return result;
    }

    /**
     * Restablece la reputación de un jugador con un clan específico o con los 4 clanes si {@code clan == null}.
     */
    public static boolean resetReputation(ServerPlayer player, @Nullable VikingClan clan) {
        return ClanReputationData.get(player.level()).resetReputation(player.getUUID(), clan);
    }

    /**
     * Envía feedback no intrusivo al jugador (Action Bar con el cambio y mensaje en chat si cambió de nivel).
     */
    public static void notifyReputationOutcome(ServerPlayer player, ReputationUpdateOutcome outcome) {
        ReputationChangeResult primary = outcome.primary();
        ReputationChangeResult rival = outcome.rival();

        boolean primaryChanged = primary.changed();
        boolean rivalChanged = rival != null && rival.changed();

        if (!primaryChanged && !rivalChanged) {
            return;
        }

        if (primaryChanged) {
            MutableComponent actionBar = formatDeltaSegment(primary);
            if (rivalChanged) {
                actionBar.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(formatDeltaSegment(rival));
            }
            player.sendOverlayMessage(actionBar);
        } else {
            player.sendOverlayMessage(formatDeltaSegment(rival));
        }

        if (primary.levelChanged()) {
            player.sendSystemMessage(Component.translatable(
                    "clan_reputation.valhalla-echoes.level_changed",
                    primary.clan().getDisplayName(),
                    primary.newLevel().getDisplayName()
            ));
        }
        if (rival != null && rival.levelChanged()) {
            player.sendSystemMessage(Component.translatable(
                    "clan_reputation.valhalla-echoes.level_changed",
                    rival.clan().getDisplayName(),
                    rival.newLevel().getDisplayName()
            ));
        }
    }

    private static MutableComponent formatDeltaSegment(ReputationChangeResult result) {
        int delta = result.actualDelta();
        String signedDelta = delta > 0 ? "+" + delta : Integer.toString(delta);
        ChatFormatting deltaColor = delta >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;

        return Component.empty()
                .append(result.clan().getDisplayName())
                .append(Component.literal(" "))
                .append(Component.literal(signedDelta).withStyle(deltaColor))
                .append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY))
                .append(result.newLevel().getDisplayName())
                .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * Resultado completo de una actualización de reputación, incluyendo el efecto opcional sobre el clan rival.
     */
    public record ReputationUpdateOutcome(
            ReputationChangeResult primary,
            @Nullable ReputationChangeResult rival
    ) {
        public boolean hasRivalEffect() {
            return this.rival != null && this.rival.changed();
        }
    }
}
