package com.mataflex.clan.reputation;

import com.mataflex.ValhallaEchoes;
import com.mataflex.clan.VikingClan;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistencia por mundo (almacenada en el Overworld) para la reputación de cada jugador
 * con los 4 clanes vikingos.
 */
public class ClanReputationData extends SavedData {

    public static final int MIN_REPUTATION = -100;
    public static final int MAX_REPUTATION = 150;
    public static final int DEFAULT_REPUTATION = 0;

    public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(
            ValhallaEchoes.MOD_ID,
            "clan_reputation"
    );

    private static final Codec<Map<UUID, Map<VikingClan, Integer>>> PLAYERS_MAP_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(VikingClan.CODEC, Codec.INT));

    public static final Codec<ClanReputationData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PLAYERS_MAP_CODEC.optionalFieldOf("players", Map.of()).forGetter(ClanReputationData::toSerializableMap)
    ).apply(instance, ClanReputationData::new));

    public static final SavedDataType<ClanReputationData> TYPE = new SavedDataType<>(
            DATA_ID,
            ClanReputationData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
    );

    private final Map<UUID, EnumMap<VikingClan, Integer>> playerReputations = new HashMap<>();

    public ClanReputationData() {
    }

    private ClanReputationData(Map<UUID, Map<VikingClan, Integer>> serializedPlayers) {
        for (Map.Entry<UUID, Map<VikingClan, Integer>> entry : serializedPlayers.entrySet()) {
            EnumMap<VikingClan, Integer> clanMap = new EnumMap<>(VikingClan.class);
            for (Map.Entry<VikingClan, Integer> clanEntry : entry.getValue().entrySet()) {
                int clamped = clamp(clanEntry.getValue());
                if (clamped != DEFAULT_REPUTATION) {
                    clanMap.put(clanEntry.getKey(), clamped);
                }
            }
            if (!clanMap.isEmpty()) {
                this.playerReputations.put(entry.getKey(), clanMap);
            }
        }
    }

    private Map<UUID, Map<VikingClan, Integer>> toSerializableMap() {
        Map<UUID, Map<VikingClan, Integer>> snapshot = new HashMap<>();
        for (Map.Entry<UUID, EnumMap<VikingClan, Integer>> entry : this.playerReputations.entrySet()) {
            Map<VikingClan, Integer> clanSnapshot = new EnumMap<>(VikingClan.class);
            for (Map.Entry<VikingClan, Integer> clanEntry : entry.getValue().entrySet()) {
                int value = clanEntry.getValue();
                if (value != DEFAULT_REPUTATION) {
                    clanSnapshot.put(clanEntry.getKey(), value);
                }
            }
            if (!clanSnapshot.isEmpty()) {
                snapshot.put(entry.getKey(), Map.copyOf(clanSnapshot));
            }
        }
        return Map.copyOf(snapshot);
    }

    /**
     * Obtiene o inicializa los datos de reputación globales del mundo desde el Overworld.
     */
    public static ClanReputationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * Obtiene o inicializa los datos de reputación globales del mundo a partir de cualquier nivel servidor.
     */
    public static ClanReputationData get(ServerLevel level) {
        return get(level.getServer());
    }

    public static int clamp(int value) {
        return Math.clamp(value, MIN_REPUTATION, MAX_REPUTATION);
    }

    public int getReputation(UUID playerId, VikingClan clan) {
        EnumMap<VikingClan, Integer> clanMap = this.playerReputations.get(playerId);
        if (clanMap == null) {
            return DEFAULT_REPUTATION;
        }
        return clanMap.getOrDefault(clan, DEFAULT_REPUTATION);
    }

    public ClanReputationLevel getLevel(UUID playerId, VikingClan clan) {
        return ClanReputationLevel.fromReputation(this.getReputation(playerId, clan));
    }

    public EnumMap<VikingClan, Integer> getAllReputations(UUID playerId) {
        EnumMap<VikingClan, Integer> result = new EnumMap<>(VikingClan.class);
        EnumMap<VikingClan, Integer> stored = this.playerReputations.get(playerId);
        for (VikingClan clan : VikingClan.VALUES) {
            int value = stored != null ? stored.getOrDefault(clan, DEFAULT_REPUTATION) : DEFAULT_REPUTATION;
            result.put(clan, value);
        }
        return result;
    }

    public ReputationChangeResult setReputation(UUID playerId, VikingClan clan, int newValue) {
        int previousValue = this.getReputation(playerId, clan);
        int clampedValue = clamp(newValue);

        ClanReputationLevel previousLevel = ClanReputationLevel.fromReputation(previousValue);
        ClanReputationLevel newLevel = ClanReputationLevel.fromReputation(clampedValue);

        if (clampedValue != previousValue) {
            if (clampedValue == DEFAULT_REPUTATION) {
                EnumMap<VikingClan, Integer> clanMap = this.playerReputations.get(playerId);
                if (clanMap != null) {
                    clanMap.remove(clan);
                    if (clanMap.isEmpty()) {
                        this.playerReputations.remove(playerId);
                    }
                }
            } else {
                this.playerReputations
                        .computeIfAbsent(playerId, ignored -> new EnumMap<>(VikingClan.class))
                        .put(clan, clampedValue);
            }
            this.setDirty();
        }

        return new ReputationChangeResult(clan, previousValue, clampedValue, previousLevel, newLevel);
    }

    public ReputationChangeResult addReputation(UUID playerId, VikingClan clan, int delta) {
        int current = this.getReputation(playerId, clan);
        return this.setReputation(playerId, clan, current + delta);
    }

    /**
     * Restablece la reputación de un jugador con un clan específico, o con los 4 clanes si {@code clan == null}.
     *
     * @return {@code true} si se modificó al menos una puntuación.
     */
    public boolean resetReputation(UUID playerId, @Nullable VikingClan clan) {
        if (clan == null) {
            EnumMap<VikingClan, Integer> removed = this.playerReputations.remove(playerId);
            if (removed != null && !removed.isEmpty()) {
                this.setDirty();
                return true;
            }
            return false;
        }

        ReputationChangeResult result = this.setReputation(playerId, clan, DEFAULT_REPUTATION);
        return result.changed();
    }

    /**
     * Resultado inmutable de una operación de cambio de reputación sobre un clan.
     */
    public record ReputationChangeResult(
            VikingClan clan,
            int previousValue,
            int newValue,
            ClanReputationLevel previousLevel,
            ClanReputationLevel newLevel
    ) {
        public int actualDelta() {
            return this.newValue - this.previousValue;
        }

        public boolean changed() {
            return this.newValue != this.previousValue;
        }

        public boolean levelChanged() {
            return this.previousLevel != this.newLevel;
        }
    }
}
