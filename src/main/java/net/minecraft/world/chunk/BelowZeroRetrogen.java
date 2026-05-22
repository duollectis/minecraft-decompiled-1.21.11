package net.minecraft.world.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSupplier;

import java.util.BitSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.LongStream;

/**
 * Данные ретрогенерации чанков ниже нулевой отметки (Y < 0).
 * Хранит целевой статус генерации и битовую маску колонок, в которых отсутствует бедрок.
 */
public final class BelowZeroRetrogen {

	private static final int BELOW_ZERO_HEIGHT = 64;
	private static final int BELOW_ZERO_BOTTOM_Y = -64;
	private static final int BEDROCK_SCAN_TOP_Y = 4;
	private static final int CHUNK_SIDE = 16;

	private static final BitSet EMPTY_MISSING_BEDROCK_BIT_SET = new BitSet(0);
	private static final Set<RegistryKey<Biome>> CAVE_BIOMES = Set.of(
			BiomeKeys.LUSH_CAVES,
			BiomeKeys.DRIPSTONE_CAVES,
			BiomeKeys.DEEP_DARK
	);

	private static final Codec<BitSet> MISSING_BEDROCK_CODEC = Codec.LONG_STREAM
			.xmap(
					stream -> BitSet.valueOf(stream.toArray()),
					bitSet -> LongStream.of(bitSet.toLongArray())
			);

	private static final Codec<ChunkStatus> STATUS_CODEC = Registries.CHUNK_STATUS
			.getCodec()
			.comapFlatMap(
					status -> status == ChunkStatus.EMPTY
							? DataResult.error(() -> "target_status cannot be empty")
							: DataResult.success(status),
					Function.identity()
			);

	public static final Codec<BelowZeroRetrogen> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					STATUS_CODEC.fieldOf("target_status").forGetter(BelowZeroRetrogen::getTargetStatus),
					MISSING_BEDROCK_CODEC.lenientOptionalFieldOf("missing_bedrock")
							.forGetter(retrogen -> retrogen.missingBedrock.isEmpty()
									? Optional.empty()
									: Optional.of(retrogen.missingBedrock))
			).apply(instance, BelowZeroRetrogen::new)
	);

	public static final HeightLimitView BELOW_ZERO_VIEW = new HeightLimitView() {
		@Override
		public int getHeight() {
			return BELOW_ZERO_HEIGHT;
		}

		@Override
		public int getBottomY() {
			return BELOW_ZERO_BOTTOM_Y;
		}
	};

	private final ChunkStatus targetStatus;
	private final BitSet missingBedrock;

	private BelowZeroRetrogen(ChunkStatus targetStatus, Optional<BitSet> missingBedrock) {
		this.targetStatus = targetStatus;
		this.missingBedrock = missingBedrock.orElse(EMPTY_MISSING_BEDROCK_BIT_SET);
	}

	/**
	 * Заменяет старый бедрок (Y 0–4) на глубинный сланец в переданном чанке.
	 * Используется при миграции старых чанков до введения ретрогенерации.
	 */
	public static void replaceOldBedrock(ProtoChunk chunk) {
		BlockPos.iterate(0, 0, 0, CHUNK_SIDE - 1, BEDROCK_SCAN_TOP_Y, CHUNK_SIDE - 1).forEach(pos -> {
			if (chunk.getBlockState(pos).isOf(Blocks.BEDROCK)) {
				chunk.setBlockState(pos, Blocks.DEEPSLATE.getDefaultState());
			}
		});
	}

	/**
	 * Заполняет воздухом все колонки чанка, в которых отсутствует бедрок согласно
	 * битовой маске {@link #missingBedrock}.
	 */
	public void fillColumnsWithAirIfMissingBedrock(ProtoChunk chunk) {
		HeightLimitView heightLimit = chunk.getHeightLimitView();
		int bottomY = heightLimit.getBottomY();
		int topY = heightLimit.getTopYInclusive();

		for (int x = 0; x < CHUNK_SIDE; x++) {
			for (int z = 0; z < CHUNK_SIDE; z++) {
				if (isColumnMissingBedrock(x, z)) {
					BlockPos.iterate(x, bottomY, z, x, topY, z)
							.forEach(pos -> chunk.setBlockState(pos, Blocks.AIR.getDefaultState()));
				}
			}
		}
	}

	public ChunkStatus getTargetStatus() {
		return targetStatus;
	}

	public boolean hasMissingBedrock() {
		return !missingBedrock.isEmpty();
	}

	public boolean isColumnMissingBedrock(int x, int z) {
		return missingBedrock.get((z & 15) * CHUNK_SIDE + (x & 15));
	}

	/**
	 * Оборачивает {@code biomeSupplier} так, чтобы пещерные биомы (лush caves, dripstone caves,
	 * deep dark) заменялись биомом поверхности чанка при ретрогенерации ниже нуля.
	 */
	public static BiomeSupplier getBiomeSupplier(BiomeSupplier biomeSupplier, Chunk chunk) {
		if (!chunk.hasBelowZeroRetrogen()) {
			return biomeSupplier;
		}

		Predicate<RegistryKey<Biome>> isCaveBiome = CAVE_BIOMES::contains;
		return (x, y, z, noise) -> {
			RegistryEntry<Biome> biome = biomeSupplier.getBiome(x, y, z, noise);
			return biome.matches(isCaveBiome) ? biome : chunk.getBiomeForNoiseGen(x, 0, z);
		};
	}
}
