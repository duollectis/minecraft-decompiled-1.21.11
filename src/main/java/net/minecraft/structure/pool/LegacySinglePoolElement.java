package net.minecraft.structure.pool;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;

import java.util.Optional;

/**
 * Устаревший вариант {@link SinglePoolElement}, используемый для обратной совместимости
 * со старыми структурами. Отличается тем, что игнорирует воздух и structure-блоки
 * вместо только structure-блоков, что соответствует поведению до введения jigsaw-системы.
 */
public class LegacySinglePoolElement extends SinglePoolElement {

	public static final MapCodec<LegacySinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(locationGetter(), processorsGetter(), projectionGetter(), overrideLiquidSettingsGetter())
			.apply(instance, LegacySinglePoolElement::new)
	);

	protected LegacySinglePoolElement(
		Either<Identifier, StructureTemplate> location,
		RegistryEntry<StructureProcessorList> processors,
		StructurePool.Projection projection,
		Optional<StructureLiquidSettings> overrideLiquidSettings
	) {
		super(location, processors, projection, overrideLiquidSettings);
	}

	@Override
	protected StructurePlacementData createPlacementData(
		BlockRotation rotation,
		BlockBox box,
		StructureLiquidSettings liquidSettings,
		boolean keepJigsaws
	) {
		StructurePlacementData placementData = super.createPlacementData(rotation, box, liquidSettings, keepJigsaws);
		placementData.removeProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
		placementData.addProcessor(BlockIgnoreStructureProcessor.IGNORE_AIR_AND_STRUCTURE_BLOCKS);
		return placementData;
	}

	@Override
	public StructurePoolElementType<?> getType() {
		return StructurePoolElementType.LEGACY_SINGLE_POOL_ELEMENT;
	}

	@Override
	public String toString() {
		return "LegacySingle[" + location + "]";
	}
}
