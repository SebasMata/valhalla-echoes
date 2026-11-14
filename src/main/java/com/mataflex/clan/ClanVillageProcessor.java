package com.mataflex.clan;

import com.mataflex.clan.banner.ClanBannerHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.NonNull;

/**
 * Procesador de estructuras que adapta automáticamente los estandartes y cofres
 * de una aldea vikinga al clan correspondiente a su región.
 */
public class ClanVillageProcessor implements StructureProcessor {

    public static final ClanVillageProcessor INSTANCE = new ClanVillageProcessor();
    public static final MapCodec<ClanVillageProcessor> MAP_CODEC = MapCodec.unit(() -> INSTANCE);

    private ClanVillageProcessor() {
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            @NonNull LevelReader level,
            @NonNull BlockPos offset,
            @NonNull BlockPos pos,
            @NonNull BlockPos relativePos,
            StructureTemplate.@NonNull StructureBlockInfo transformedBlockInfo,
            @NonNull StructurePlaceSettings settings
    ) {
        BlockState state = transformedBlockInfo.state();

        // 1. Reemplazar cualquier estandarte por el estandarte y patrón del clan de la aldea
        if (ClanBannerHelper.isBannerBlock(state)) {
            VikingClan clan = ClanVillageManager.getClanForPos(pos);
            BlockState newBannerState = ClanBannerHelper.getClanBannerState(state, clan);
            CompoundTag newBannerNbt = ClanBannerHelper.applyClanPatternToBannerNbt(transformedBlockInfo.nbt(), clan);
            return new StructureTemplate.StructureBlockInfo(
                    transformedBlockInfo.pos(),
                    newBannerState,
                    newBannerNbt
            );
        }

        // 2. Añadir el molde de estandarte del clan a los cofres o barriles de la aldea
        if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
            VikingClan clan = ClanVillageManager.getClanForPos(pos);
            CompoundTag newChestNbt = addClanPatternItemToContainer(transformedBlockInfo.nbt(), clan);
            return new StructureTemplate.StructureBlockInfo(
                    transformedBlockInfo.pos(),
                    state,
                    newChestNbt
            );
        }

        return transformedBlockInfo;
    }

    private static CompoundTag addClanPatternItemToContainer(CompoundTag originalNbt, VikingClan clan) {
        CompoundTag nbt = originalNbt != null ? originalNbt.copy() : new CompoundTag();

        ListTag itemsList = nbt.getListOrEmpty("Items").copy();

        String itemId = BuiltInRegistries.ITEM.getKey(clan.getBannerPatternItem()).toString();
        boolean alreadyPresent = false;
        boolean[] occupiedSlots = new boolean[27];

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompoundOrEmpty(i);
            int slot = itemTag.getByteOr("Slot", (byte) 0) & 255;
            if (slot >= 0 && slot < occupiedSlots.length) {
                occupiedSlots[slot] = true;
            }
            if (itemId.equals(itemTag.getStringOr("id", ""))) {
                alreadyPresent = true;
            }
        }

        if (!alreadyPresent) {
            // Buscar el centro del cofre (slot 13) o el primer slot libre
            int targetSlot = !occupiedSlots[13] ? 13 : -1;
            if (targetSlot == -1) {
                for (int s = 0; s < occupiedSlots.length; s++) {
                    if (!occupiedSlots[s]) {
                        targetSlot = s;
                        break;
                    }
                }
            }

            if (targetSlot != -1) {
                CompoundTag patternItemTag = new CompoundTag();
                patternItemTag.putByte("Slot", (byte) targetSlot);
                patternItemTag.putString("id", itemId);
                patternItemTag.putInt("count", 1);
                itemsList.add(patternItemTag);
                nbt.put("Items", itemsList);
            }
        }

        return nbt;
    }

    @Override
    public @NonNull MapCodec<? extends StructureProcessor> codec() {
        return MAP_CODEC;
    }
}
