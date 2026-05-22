package net.minecraft.world.gen.chunk.placement;

import com.mojang.datafixers.Products.P4;
import com.mojang.datafixers.Products.P5;
import com.mojang.datafixers.Products.P9;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.Optional;

/**
 * Размещение структур по концентрическим кольцам вокруг центра мира.
 * Используется для Крепостей (Strongholds): структуры равномерно распределяются
 * по кольцам, каждое из которых расположено дальше от центра, чем предыдущее.
 * Позиции вычисляются асинхронно через {@link StructurePlacementCalculator}.
 */
public class ConcentricRingsStructurePlacement extends StructurePlacement {

	public static final MapCodec<ConcentricRingsStructurePlacement> CODEC = RecordCodecBuilder.mapCodec(
			instance -> buildConcentricRingsCodec(instance).apply(instance, ConcentricRingsStructurePlacement::new)
	);

	private final int distance;
	private final int spread;
	private final int count;
	private final RegistryEntryList<Biome> preferredBiomes;

	private static P9<Mu<ConcentricRingsStructurePlacement>, Vec3i, FrequencyReductionMethod, Float, Integer, Optional<ExclusionZone>, Integer, Integer, Integer, RegistryEntryList<Biome>> buildConcentricRingsCodec(
			Instance<ConcentricRingsStructurePlacement> instance
	) {
		P5<Mu<ConcentricRingsStructurePlacement>, Vec3i, FrequencyReductionMethod, Float, Integer, Optional<ExclusionZone>>
				base = buildCodec(instance);
		P4<Mu<ConcentricRingsStructurePlacement>, Integer, Integer, Integer, RegistryEntryList<Biome>>
				rings = instance.group(
				Codec.intRange(0, 1023)
				     .fieldOf("distance")
				     .forGetter(ConcentricRingsStructurePlacement::getDistance),
				Codec.intRange(0, 1023)
				     .fieldOf("spread")
				     .forGetter(ConcentricRingsStructurePlacement::getSpread),
				Codec.intRange(1, 4095)
				     .fieldOf("count")
				     .forGetter(ConcentricRingsStructurePlacement::getCount),
				RegistryCodecs.entryList(RegistryKeys.BIOME)
				              .fieldOf("preferred_biomes")
				              .forGetter(ConcentricRingsStructurePlacement::getPreferredBiomes)
		);
		return new P9(base.t1(), base.t2(), base.t3(), base.t4(), base.t5(), rings.t1(), rings.t2(), rings.t3(), rings.t4());
	}

	public ConcentricRingsStructurePlacement(
			Vec3i locateOffset,
			FrequencyReductionMethod frequencyReductionMethod,
			float frequency,
			int salt,
			Optional<ExclusionZone> exclusionZone,
			int distance,
			int spread,
			int structureCount,
			RegistryEntryList<Biome> preferredBiomes
	) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
		this.distance = distance;
		this.spread = spread;
		this.count = structureCount;
		this.preferredBiomes = preferredBiomes;
	}

	public ConcentricRingsStructurePlacement(
			int distance,
			int spread,
			int structureCount,
			RegistryEntryList<Biome> preferredBiomes
	) {
		this(
				Vec3i.ZERO,
				FrequencyReductionMethod.DEFAULT,
				1.0F,
				0,
				Optional.empty(),
				distance,
				spread,
				structureCount,
				preferredBiomes
		);
	}

	public int getDistance() {
		return distance;
	}

	public int getSpread() {
		return spread;
	}

	public int getCount() {
		return count;
	}

	public RegistryEntryList<Biome> getPreferredBiomes() {
		return preferredBiomes;
	}

	@Override
	protected boolean isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
		List<ChunkPos> positions = calculator.getPlacementPositions(this);
		return positions != null && positions.contains(new ChunkPos(chunkX, chunkZ));
	}

	@Override
	public StructurePlacementType<?> getType() {
		return StructurePlacementType.CONCENTRIC_RINGS;
	}
}
