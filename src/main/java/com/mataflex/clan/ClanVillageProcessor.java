package com.mataflex.clan;

import com.mataflex.ValhallaEchoes;
import com.mataflex.clan.banner.ClanBannerHelper;
import com.mataflex.entity.MysticalVikingEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MagmaBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Procesador de estructuras que adapta automáticamente los estandartes y cofres
 * de una aldea vikinga al clan correspondiente a su región, y genera vikingos
 * residentes persistentes durante la colocación de las piezas de la aldea.
 */
public class ClanVillageProcessor implements StructureProcessor {

    public static final ClanVillageProcessor INSTANCE = new ClanVillageProcessor();
    public static final MapCodec<ClanVillageProcessor> MAP_CODEC = MapCodec.unit(() -> INSTANCE);

    private static final int MAX_RESIDENTS_PER_HUT_SLICE = 2;
    private static final float STREET_PATROL_SPAWN_CHANCE = 0.40F;

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

    @Override
    public @NonNull List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            @NonNull ServerLevelAccessor level,
            @NonNull BlockPos offset,
            @NonNull BlockPos pos,
            @NonNull List<StructureTemplate.StructureBlockInfo> originalBlockInfos,
            @NonNull List<StructureTemplate.StructureBlockInfo> processedBlockInfos,
            @NonNull StructurePlaceSettings settings
    ) {
        if (processedBlockInfos.isEmpty()) {
            return processedBlockInfos;
        }

        Map<BlockPos, BlockState> pieceBlocks = new HashMap<>(processedBlockInfos.size());
        List<BlockPos> hutAnchors = new ArrayList<>();
        List<BlockPos> streetPaths = new ArrayList<>();
        boolean hasDoor = false;
        boolean hasStorageOrBed = false;

        for (StructureTemplate.StructureBlockInfo info : processedBlockInfos) {
            BlockPos blockPos = info.pos();
            BlockState state = info.state();
            pieceBlocks.put(blockPos, state);

            if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                hasDoor = true;
                hutAnchors.add(blockPos);
            } else if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
                hasStorageOrBed = true;
                hutAnchors.add(blockPos);
            } else if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock) {
                hasStorageOrBed = true;
                hutAnchors.add(blockPos);
            } else if (state.is(Blocks.DIRT_PATH)) {
                streetPaths.add(blockPos);
            }
        }

        RandomSource random = settings.getRandom(pos);
        VikingClan clan = ClanVillageManager.getClanForPos(pos);

        // Caso 1: Pieza de cabaña (anclada por puerta para evitar duplicados entre chunks)
        if (hasDoor) {
            int desiredCount = hasStorageOrBed ? MAX_RESIDENTS_PER_HUT_SLICE : 1;
            BlockPos primaryAnchor = hutAnchors.getFirst();
            List<BlockPos> validSpots = findSafeInteriorSpawnSpots(level, pieceBlocks, primaryAnchor);

            int spawned = 0;
            List<BlockPos> chosenSpots = new ArrayList<>();
            for (BlockPos candidate : validSpots) {
                if (spawned >= desiredCount) {
                    break;
                }
                boolean tooClose = false;
                for (BlockPos existing : chosenSpots) {
                    if (existing.distSqr(candidate) < 4.0D) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) {
                    continue;
                }
                spawnResidentViking(level, candidate, clan, random);
                chosenSpots.add(candidate);
                spawned++;
            }
        } else if (!hasStorageOrBed && !streetPaths.isEmpty() && random.nextFloat() < STREET_PATROL_SPAWN_CHANCE) {
            // Caso 2: Pieza de calle/camino — patrullero residente sobre el camino
            BlockPos midPath = streetPaths.get(streetPaths.size() / 2);
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, midPath.getX(), midPath.getZ());
            BlockPos spawnPos = new BlockPos(midPath.getX(), Math.max(surfaceY, midPath.getY() + 1), midPath.getZ());
            spawnResidentViking(level, spawnPos, clan, random);
        }

        return processedBlockInfos;
    }

    private static List<BlockPos> findSafeInteriorSpawnSpots(
            ServerLevelAccessor level,
            Map<BlockPos, BlockState> pieceBlocks,
            BlockPos anchor
    ) {
        List<BlockPos> spots = new ArrayList<>();

        for (Map.Entry<BlockPos, BlockState> entry : pieceBlocks.entrySet()) {
            BlockPos floorPos = entry.getKey();
            BlockState floorState = entry.getValue();

            if (!isSafeFloorBlock(level, floorPos, floorState)) {
                continue;
            }

            BlockPos feetPos = floorPos.above();
            BlockPos headPos = floorPos.above(2);
            BlockState feetState = pieceBlocks.get(feetPos);
            BlockState headState = pieceBlocks.get(headPos);

            // Exigimos que tanto pies como cabeza sean aire explícito dentro de la pieza para evitar asfixia
            if (feetState != null && feetState.isAir() && headState != null && headState.isAir()) {
                spots.add(feetPos);
            }
        }

        // Priorizamos espacios cercanos a la puerta/interior de la cabaña
        spots.sort(Comparator.comparingDouble(spot -> spot.distSqr(anchor)));
        return spots;
    }

    private static boolean isSafeFloorBlock(ServerLevelAccessor level, BlockPos pos, BlockState state) {
        if (state.isAir()
                || state.getBlock() instanceof CampfireBlock
                || state.getBlock() instanceof FireBlock
                || state.getBlock() instanceof MagmaBlock
                || state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof DoorBlock
                || state.getBlock() instanceof BedBlock
                || state.getBlock() instanceof ChestBlock
                || state.getBlock() instanceof BarrelBlock) {
            return false;
        }
        return state.is(Blocks.DIRT_PATH) || state.isCollisionShapeFullBlock(level, pos);
    }

    private static void spawnResidentViking(
            ServerLevelAccessor level,
            BlockPos spawnPos,
            VikingClan clan,
            RandomSource random
    ) {
        MysticalVikingEntity viking = ValhallaEchoes.MYSTICAL_VIKING.create(
                level.getLevel(),
                EntitySpawnReason.STRUCTURE
        );
        if (viking == null) {
            return;
        }

        viking.setClan(clan);
        viking.setPersistenceRequired();
        viking.snapTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );
        viking.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPos),
                EntitySpawnReason.STRUCTURE,
                null
        );
        level.addFreshEntityWithPassengers(viking);
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
