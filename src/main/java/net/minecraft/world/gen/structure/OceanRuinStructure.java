package net.minecraft.world.gen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.structure.OceanRuinGenerator;
import net.minecraft.structure.StructurePiecesCollector;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.Optional;

/**
 * Структура руин океана. Тип биома (тёплый/холодный) определяет набор используемых шаблонов.
 * Вероятности {@code largeProbability} и {@code clusterProbability} управляют размером и кластеризацией.
 */
public class OceanRuinStructure extends Structure {

	public static final MapCodec<OceanRuinStructure> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(
					                    configCodecBuilder(instance),
					                    OceanRuinStructure.BiomeTemperature.CODEC
							                    .fieldOf("biome_temp")
							                    .forGetter(structure -> structure.biomeTemperature),
					                    Codec
							                    .floatRange(0.0F, 1.0F)
							                    .fieldOf("large_probability")
							                    .forGetter(structure -> structure.largeProbability),
					                    Codec
							                    .floatRange(0.0F, 1.0F)
							                    .fieldOf("cluster_probability")
							                    .forGetter(structure -> structure.clusterProbability)
			                    )
			                    .apply(instance, OceanRuinStructure::new)
	);
	public final OceanRuinStructure.BiomeTemperature biomeTemperature;
	public final float largeProbability;
	public final float clusterProbability;

	public OceanRuinStructure(
			Structure.Config config,
			OceanRuinStructure.BiomeTemperature biomeTemperature,
			float largeProbability,
			float clusterProbability
	) {
		super(config);
		this.biomeTemperature = biomeTemperature;
		this.largeProbability = largeProbability;
		this.clusterProbability = clusterProbability;
	}

	@Override
	public Optional<Structure.StructurePosition> getStructurePosition(Structure.Context context) {
		return getStructurePosition(
				context,
				Heightmap.Type.OCEAN_FLOOR_WG,
				collector -> addPieces(collector, context)
		);
	}

	private void addPieces(StructurePiecesCollector collector, Structure.Context context) {
		BlockPos blockPos = new BlockPos(context.chunkPos().getStartX(), 90, context.chunkPos().getStartZ());
		BlockRotation blockRotation = BlockRotation.random(context.random());
		OceanRuinGenerator.addPieces(
				context.structureTemplateManager(),
				blockPos,
				blockRotation,
				collector,
				context.random(),
				this
		);
	}

	@Override
	public StructureType<?> getType() {
		return StructureType.OCEAN_RUIN;
	}

	/** Температурный тип биома, определяющий набор шаблонов руин. */
	public enum BiomeTemperature implements StringIdentifiable {
		WARM("warm"),
		COLD("cold");

		public static final Codec<OceanRuinStructure.BiomeTemperature> CODEC = StringIdentifiable.createCodec(OceanRuinStructure.BiomeTemperature::values);

		/**
		 * @deprecated Используется только для обратной совместимости при чтении старых сохранений.
		 */
		@Deprecated
		public static final Codec<OceanRuinStructure.BiomeTemperature> ENUM_NAME_CODEC = Codecs.enumByName(OceanRuinStructure.BiomeTemperature::valueOf);
		private final String name;

		BiomeTemperature(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public String asString() {
			return name;
		}
	}
}
