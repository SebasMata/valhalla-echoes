package com.mataflex.clan.banner;

import com.mataflex.clan.VikingClan;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Define la configuración y transformación de los estandartes (Banners) asociados
 * a cada clan vikingo para decorar las aldeas.
 */
public class ClanBannerHelper {

    private ClanBannerHelper() {
        // Clase auxiliar para construir los estandartes de cada clan
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
}
