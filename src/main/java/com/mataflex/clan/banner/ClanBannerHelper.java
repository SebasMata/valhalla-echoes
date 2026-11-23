package com.mataflex.clan.banner;

import com.mataflex.clan.ClanComponents;
import com.mataflex.clan.VikingClan;
import com.mataflex.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Define la configuración y transformación de los estandartes (Banners) y escudos
 * asociados a cada clan vikingo para decorar aldeas y equipar entidades.
 */
public class ClanBannerHelper {

    private ClanBannerHelper() {
        // Clase auxiliar para construir los estandartes y escudos de cada clan
    }

    /**
     * Comprueba si un estado de bloque corresponde a un estandarte (de pie o de pared).
     */
    public static boolean isBannerBlock(BlockState state) {
        return state.getBlock() instanceof BannerBlock || state.getBlock() instanceof WallBannerBlock;
    }

    /**
     * Transforma cualquier bloque de estandarte al color base del clan indicado,
     * preservando su orientación o rotación exacta.
     */
    public static BlockState getClanBannerState(BlockState originalState, VikingClan clan) {
        if (originalState.getBlock() instanceof BannerBlock) {
            BlockState newState = clan.getStandingBanner().defaultBlockState();
            if (originalState.hasProperty(BannerBlock.ROTATION)) {
                newState = newState.setValue(BannerBlock.ROTATION, originalState.getValue(BannerBlock.ROTATION));
            }
            return newState;
        } else if (originalState.getBlock() instanceof WallBannerBlock) {
            BlockState newState = clan.getWallBanner().defaultBlockState();
            if (originalState.hasProperty(WallBannerBlock.FACING)) {
                newState = newState.setValue(WallBannerBlock.FACING, originalState.getValue(WallBannerBlock.FACING));
            }
            return newState;
        }
        return originalState;
    }

    /**
     * Crea o actualiza el CompoundTag (NBT) de la entidad de bloque del estandarte
     * para incluir la capa con el símbolo y color del clan.
     */
    public static CompoundTag applyClanPatternToBannerNbt(CompoundTag originalNbt, VikingClan clan) {
        CompoundTag nbt = originalNbt != null ? originalNbt.copy() : new CompoundTag();
        if (!nbt.contains("id")) {
            nbt.putString("id", "minecraft:banner");
        }

        ListTag patternsList = new ListTag();
        CompoundTag layerTag = new CompoundTag();
        layerTag.putString("pattern", clan.getPatternKey().identifier().toString());
        layerTag.putString("color", clan.getPatternColor().getName());
        patternsList.add(layerTag);

        nbt.put("patterns", patternsList);
        return nbt;
    }

    /**
     * Construye el componente {@link BannerPatternLayers} con el emblema y color del clan.
     */
    public static BannerPatternLayers createClanBannerLayers(VikingClan clan, HolderLookup.Provider registries) {
        BannerPatternLayers.Builder builder = new BannerPatternLayers.Builder();
        registries.lookup(Registries.BANNER_PATTERN).ifPresent(bannerLookup ->
                builder.addIfRegistered(bannerLookup, clan.getPatternKey(), clan.getPatternColor())
        );
        return builder.build();
    }

    /**
     * Estampa la identidad del clan (componente de modelo 3D, color base y capas de patrón)
     * sobre un {@link ItemStack} de escudo Targe.
     */
    public static ItemStack applyClanToTargeShield(ItemStack shieldStack, VikingClan clan, HolderLookup.Provider registries) {
        shieldStack.set(ClanComponents.CLAN, clan);
        shieldStack.set(DataComponents.BASE_COLOR, clan.getBaseColor());
        BannerPatternLayers layers = createClanBannerLayers(clan, registries);
        if (!layers.layers().isEmpty()) {
            shieldStack.set(DataComponents.BANNER_PATTERNS, layers);
        }
        return shieldStack;
    }

    /**
     * Crea un nuevo {@code TargeShield} con el modelo 3D y patrón heráldico del clan indicado.
     */
    public static ItemStack createClanTargeShield(VikingClan clan, HolderLookup.Provider registries) {
        return applyClanToTargeShield(new ItemStack(ModItems.TARGE_SHIELD), clan, registries);
    }

    /**
     * Crea un {@link ItemStack} de estandarte del clan (para portaestandartes o recompensas).
     */
    public static ItemStack createClanBannerStack(VikingClan clan, HolderLookup.Provider registries) {
        ItemStack bannerStack = new ItemStack(clan.getStandingBanner().asItem());
        BannerPatternLayers layers = createClanBannerLayers(clan, registries);
        if (!layers.layers().isEmpty()) {
            bannerStack.set(DataComponents.BANNER_PATTERNS, layers);
        }
        return bannerStack;
    }
}
