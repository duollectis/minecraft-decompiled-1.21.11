package net.minecraft.structure.pool;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureLiquidSettings;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.structure.processor.JigsawReplacementStructureProcessor;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Элемент пула, основанный на одном структурном шаблоне (.nbt файле).
 * Является наиболее распространённым типом элемента в jigsaw-генерации.
 */
public class SinglePoolElement extends StructurePoolElement {

	private static final Comparator<StructureTemplate.JigsawBlockInfo> JIGSAW_BLOCK_INFO_COMPARATOR =
		Comparator.comparingInt(StructureTemplate.JigsawBlockInfo::selectionPriority).reversed();

	private static final Codec<Either<Identifier, StructureTemplate>> LOCATION_CODEC = Codec.of(
		SinglePoolElement::encodeLocation,
		Identifier.CODEC.map(Either::left)
	);

	public static final MapCodec<SinglePoolElement> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance
			.group(locationGetter(), processorsGetter(), projectionGetter(), overrideLiquidSettingsGetter())
			.apply(instance, SinglePoolElement::new)
	);

	protected final Either<Identifier, StructureTemplate> location;
	protected final RegistryEntry<StructureProcessorList> processors;
	protected final Optional<StructureLiquidSettings> overrideLiquidSettings;

	private static <T> DataResult<T> encodeLocation(
		Either<Identifier, StructureTemplate> location,
		DynamicOps<T> ops,
		T prefix
	) {
		Optional<Identifier> id = location.left();
		return id.isEmpty()
			? DataResult.error(() -> "Can not serialize a runtime pool element")
			: Identifier.CODEC.encode(id.get(), ops, prefix);
	}

	protected static <E extends SinglePoolElement> RecordCodecBuilder<E, RegistryEntry<StructureProcessorList>> processorsGetter() {
		return StructureProcessorType.REGISTRY_CODEC.fieldOf("processors").forGetter(pool -> pool.processors);
	}

	protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Optional<StructureLiquidSettings>> overrideLiquidSettingsGetter() {
		return StructureLiquidSettings.codec
			.optionalFieldOf("override_liquid_settings")
			.forGetter(pool -> pool.overrideLiquidSettings);
	}

	protected static <E extends SinglePoolElement> RecordCodecBuilder<E, Either<Identifier, StructureTemplate>> locationGetter() {
		return LOCATION_CODEC.fieldOf("location").forGetter(pool -> pool.location);
	}

	protected SinglePoolElement(
		Either<Identifier, StructureTemplate> location,
		RegistryEntry<StructureProcessorList> processors,
		StructurePool.Projection projection,
		Optional<StructureLiquidSettings> overrideLiquidSettings
	) {
		super(projection);
		this.location = location;
		this.processors = processors;
		this.overrideLiquidSettings = overrideLiquidSettings;
	}

	@Override
	public Vec3i getStart(StructureTemplateManager structureTemplateManager, BlockRotation rotation) {
		return getStructure(structureTemplateManager).getRotatedSize(rotation);
	}

	private StructureTemplate getStructure(StructureTemplateManager structureTemplateManager) {
		return location.map(structureTemplateManager::getTemplateOrBlank, Function.identity());
	}

	/**
	 * Возвращает список блоков {@code structure_block} в режиме DATA из шаблона.
	 * Используется для обработки метаданных после размещения структуры.
	 */
	public List<StructureTemplate.StructureBlockInfo> getDataStructureBlocks(
		StructureTemplateManager structureTemplateManager,
		BlockPos pos,
		BlockRotation rotation,
		boolean mirroredAndRotated
	) {
		StructureTemplate template = getStructure(structureTemplateManager);
		List<StructureTemplate.StructureBlockInfo> allBlocks = template.getInfosForBlock(
			pos,
			new StructurePlacementData().setRotation(rotation),
			Blocks.STRUCTURE_BLOCK,
			mirroredAndRotated
		);
		List<StructureTemplate.StructureBlockInfo> dataBlocks = Lists.newArrayList();

		for (StructureTemplate.StructureBlockInfo blockInfo : allBlocks) {
			NbtCompound nbt = blockInfo.nbt();
			if (nbt == null) {
				continue;
			}

			StructureBlockMode mode = nbt.<StructureBlockMode>get("mode", StructureBlockMode.CODEC).orElseThrow();
			if (mode == StructureBlockMode.DATA) {
				dataBlocks.add(blockInfo);
			}
		}

		return dataBlocks;
	}

	@Override
	public List<StructureTemplate.JigsawBlockInfo> getStructureBlockInfos(
		StructureTemplateManager structureTemplateManager,
		BlockPos pos,
		BlockRotation rotation,
		Random random
	) {
		List<StructureTemplate.JigsawBlockInfo> jigsawInfos =
			getStructure(structureTemplateManager).getJigsawInfos(pos, rotation);
		Util.shuffle(jigsawInfos, random);
		sort(jigsawInfos);
		return jigsawInfos;
	}

	@VisibleForTesting
	static void sort(List<StructureTemplate.JigsawBlockInfo> blocks) {
		blocks.sort(JIGSAW_BLOCK_INFO_COMPARATOR);
	}

	@Override
	public BlockBox getBoundingBox(
		StructureTemplateManager structureTemplateManager,
		BlockPos pos,
		BlockRotation rotation
	) {
		return getStructure(structureTemplateManager)
			.calculateBoundingBox(new StructurePlacementData().setRotation(rotation), pos);
	}

	@Override
	public boolean generate(
		StructureTemplateManager structureTemplateManager,
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		BlockPos pos,
		BlockPos pivot,
		BlockRotation rotation,
		BlockBox box,
		Random random,
		StructureLiquidSettings liquidSettings,
		boolean keepJigsaws
	) {
		StructureTemplate template = getStructure(structureTemplateManager);
		StructurePlacementData placementData = createPlacementData(rotation, box, liquidSettings, keepJigsaws);

		if (!template.place(world, pos, pivot, placementData, random, 18)) {
			return false;
		}

		for (StructureTemplate.StructureBlockInfo blockInfo : StructureTemplate.process(
			world,
			pos,
			pivot,
			placementData,
			getDataStructureBlocks(structureTemplateManager, pos, rotation, false)
		)) {
			handleJigsawBlock(world, blockInfo, pos, rotation, random, box);
		}

		return true;
	}

	/**
	 * Создаёт конфигурацию размещения шаблона с нужными процессорами.
	 * Если {@code keepJigsaws} равен {@code false}, добавляет процессор замены jigsaw-блоков.
	 */
	protected StructurePlacementData createPlacementData(
		BlockRotation rotation,
		BlockBox box,
		StructureLiquidSettings liquidSettings,
		boolean keepJigsaws
	) {
		StructurePlacementData placementData = new StructurePlacementData();
		placementData.setBoundingBox(box);
		placementData.setRotation(rotation);
		placementData.setUpdateNeighbors(true);
		placementData.setIgnoreEntities(false);
		placementData.addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS);
		placementData.setInitializeMobs(true);
		placementData.setLiquidSettings(overrideLiquidSettings.orElse(liquidSettings));

		if (!keepJigsaws) {
			placementData.addProcessor(JigsawReplacementStructureProcessor.INSTANCE);
		}

		processors.value().getList().forEach(placementData::addProcessor);
		getProjection().getProcessors().forEach(placementData::addProcessor);
		return placementData;
	}

	@Override
	public StructurePoolElementType<?> getType() {
		return StructurePoolElementType.SINGLE_POOL_ELEMENT;
	}

	@Override
	public String toString() {
		return "Single[" + location + "]";
	}

	@VisibleForTesting
	public Identifier getIdOrThrow() {
		return location.orThrow();
	}
}
