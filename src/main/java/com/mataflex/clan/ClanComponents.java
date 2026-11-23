package com.mataflex.clan;

import com.mataflex.ValhallaEchoes;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

/**
 * Componentes de datos personalizados para asociar ítems (como el TargeShield)
 * a uno de los 4 clanes vikingos, persistentes en disco y sincronizados por red.
 */
public final class ClanComponents {

    public static final DataComponentType<VikingClan> CLAN = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(ValhallaEchoes.MOD_ID, "clan"),
            DataComponentType.<VikingClan>builder()
                    .persistent(VikingClan.CODEC)
                    .networkSynchronized(ByteBufCodecs.idMapper(
                            index -> VikingClan.VALUES[Math.floorMod(index, VikingClan.VALUES.length)],
                            Enum::ordinal
                    ))
                    .build()
    );

    private ClanComponents() {
    }

    public static void initialize() {
        ValhallaEchoes.LOGGER.info("Registered Viking clan data components.");
    }
}
