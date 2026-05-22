package net.minecraft.world.gen.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.structure.OceanMonumentGenerator;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.structure.StructurePiecesList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

import java.util.Objects;
import java.util.Optional;

/**
 * Структура подводного монумента. Перед размещением проверяет, что все биомы в радиусе 29 блоков
 * принадлежат тегу {@link net.minecraft.registry.tag.BiomeTags#REQUIRED_OCEAN_MONUMENT_SURROUNDING}.
 */
public class OceanMonumentStructure extends Structure {

	public static final MapCodec<OceanMonumentStructure> CODEC = createCodec(OceanMonumentStructure::new);

	public OceanMonumentStructure(Structure.Config config) {
		super(config);
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		int centerX = context.chunkPos().getOffsetX(9);
		int centerZ = context.chunkPos().getOffsetZ(9);

		for (RegistryEntry<Biome> biome : context.biomeSource()
		                                         .getBiomesInArea(
				                                         centerX,
				                                         context.chunkGenerator().getSeaLevel(),
				                                         centerZ,
				                                         29,
				                                         context.noiseConfig().getMultiNoiseSampler()
		                                         )) {
			if (!biome.isIn(BiomeTags.REQUIRED_OCEAN_MONUMENT_SURROUNDING)) {
				return Optional.empty();
			}
		}

		return getStructurePosition(context, Heightmap.Type.OCEAN_FLOOR_WG, collector -> addPieces(collector, context));
	}

	private static StructurePiece createBasePiece(ChunkPos pos, ChunkRandom random) {
		int startX = pos.getStartX() - 29;
		int startZ = pos.getStartZ() - 29;
		Direction direction = Direction.Type.HORIZONTAL.random(random);
		return new OceanMonumentGenerator.Base(random, startX, startZ, direction);
	}

	private static void addPieces(StructurePiecesCollector collector, Structure.Context context) {
		collector.addPiece(createBasePiece(context.chunkPos(), context.random()));
	}

	/**
	 * Пересоздаёт базовый кусок монумента при загрузке из сохранения, используя сид мира для
	 * воспроизводимого направления. Необходимо из-за изменений формата сохранения в старых версиях.
	 */
	public static StructurePiecesList modifyPiecesOnRead(ChunkPos pos, long worldSeed, StructurePiecesList pieces) {
		if (pieces.isEmpty()) {
			return pieces;
		}

		ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
		chunkRandom.setCarverSeed(worldSeed, pos.x, pos.z);
		StructurePiece firstPiece = pieces.pieces().get(0);
		BlockBox boundingBox = firstPiece.getBoundingBox();
		int startX = boundingBox.getMinX();
		int startZ = boundingBox.getMinZ();
		Direction randomDirection = Direction.Type.HORIZONTAL.random(chunkRandom);
		Direction facing = Objects.requireNonNullElse(firstPiece.getFacing(), randomDirection);
		StructurePiece rebuiltPiece = new OceanMonumentGenerator.Base(chunkRandom, startX, startZ, facing);
		StructurePiecesCollector collector = new StructurePiecesCollector();
		collector.addPiece(rebuiltPiece);
		return collector.toList();
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.OCEAN_MONUMENT;
	}
}
