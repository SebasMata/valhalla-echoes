package com.mataflex.clan;

import com.mataflex.ValhallaEchoes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Gestor centralizado para la lógica de clanes en las aldeas vikingas.
 * Asigna un clan de forma determinista por regiones de 512x512 bloques y registra
 * el StructureProcessor encargado de decorar las aldeas.
 */
public class ClanVillageManager {

    /**
     * Tamaño de región en bloques (2^9 = 512 bloques).
     * Todas las piezas de una aldea dentro de la misma región comparten el mismo clan.
     */
    private static final int REGION_SHIFT = 9;

    public static MapCodec<ClanVillageProcessor> CLAN_VILLAGE_PROCESSOR;

    private ClanVillageManager() {
        // Clase de utilidad / gestor estático
    }

    /**
     * Inicializa el sistema de clanes y registra el procesador de estructuras.
     */
    public static void initialize() {
        CLAN_VILLAGE_PROCESSOR = Registry.register(
                BuiltInRegistries.STRUCTURE_PROCESSOR,
                Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, "clan_village"),
                ClanVillageProcessor.MAP_CODEC
        );
        ValhallaEchoes.LOGGER.info("Initialized Viking Clan system and structure processor.");
    }

    /**
     * Obtiene el clan asignado a la región de 512x512 bloques correspondiente a una posición.
     */
    public static VikingClan getClanForPos(BlockPos pos) {
        int regionX = pos.getX() >> REGION_SHIFT;
        int regionZ = pos.getZ() >> REGION_SHIFT;

        long hash = (long) regionX * 341873128712L + (long) regionZ * 132897987541L;
        hash = (hash ^ (hash >>> 30)) * 0xbf58476d1ce4e5b9L;
        hash = (hash ^ (hash >>> 27)) * 0x94d049bb133111ebL;
        hash = hash ^ (hash >>> 31);

        int index = Math.floorMod(hash, VikingClan.VALUES.length);
        return VikingClan.VALUES[index];
    }
}
