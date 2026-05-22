package net.minecraft.world.chunk;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.SimpleTickScheduler;
import net.minecraft.world.tick.Tick;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;

/**
 * Промежуточное представление чанка в сериализованном виде.
 * Используется как мост между NBT-данными на диске и объектами {@link Chunk} в памяти.
 * Содержит все данные чанка: секции блоков, биомы, освещение, структуры, тики и т.д.
 */
public record SerializedChunk(
		PalettesFactory containerFactory,
		ChunkPos chunkPos,
		int minSectionY,
		long lastUpdateTime,
		long inhabitedTime,
		ChunkStatus chunkStatus,
		BlendingData.@Nullable Serialized blendingData,
		@Nullable BelowZeroRetrogen belowZeroRetrogen,
		UpgradeData upgradeData,
		long @Nullable [] carvingMask,
		Map<Heightmap.Type, long[]> heightmaps,
		Chunk.TickSchedulers packedTicks,
		@Nullable ShortList[] postProcessingSections,
		boolean lightCorrect,
		List<SerializedChunk.SectionData> sectionData,
		List<NbtCompound> entities,
		List<NbtCompound> blockEntities,
		NbtCompound structureData
) {

	private static final Codec<List<Tick<Block>>>
			BLOCK_TICKS_CODEC =
			Tick.createCodec(Registries.BLOCK.getCodec()).listOf();
	private static final Codec<List<Tick<Fluid>>>
			FLUID_TICKS_CODEC =
			Tick.createCodec(Registries.FLUID.getCodec()).listOf();
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String UPGRADE_DATA_KEY = "UpgradeData";
	private static final String BLOCK_TICKS = "block_ticks";
	private static final String FLUID_TICKS = "fluid_ticks";
	public static final String X_POS_KEY = "xPos";
	public static final String Z_POS_KEY = "zPos";
	public static final String HEIGHTMAPS_KEY = "Heightmaps";
	public static final String IS_LIGHT_ON_KEY = "isLightOn";
	public static final String SECTIONS_KEY = "sections";
	public static final String BLOCK_LIGHT_KEY = "BlockLight";
	public static final String SKY_LIGHT_KEY = "SkyLight";

	/**
	 * Десериализует чанк из NBT-данных, прочитанных с диска.
	 * Восстанавливает все секции блоков, биомы, освещение, структуры и тики.
	 * При ошибках парсинга палитр логирует их и продолжает загрузку (recoverable errors).
	 *
	 * @param world           вертикальные границы мира для валидации координат секций
	 * @param palettesFactory фабрика кодеков для десериализации блоков и биомов
	 * @param nbt             корневой NBT-тег чанка
	 * @return десериализованный чанк, или {@code null} если отсутствует поле Status
	 */
	public static SerializedChunk fromNbt(HeightLimitView world, PalettesFactory palettesFactory, NbtCompound nbt) {
		if (nbt.getString("Status").isEmpty()) {
			return null;
		}

		ChunkPos chunkPos = new ChunkPos(nbt.getInt("xPos", 0), nbt.getInt("zPos", 0));
		long lastUpdate = nbt.getLong("LastUpdate", 0L);
		long inhabitedTime = nbt.getLong("InhabitedTime", 0L);
		ChunkStatus chunkStatus = nbt.<ChunkStatus>get("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
		UpgradeData upgradeData = nbt
				.getCompound("UpgradeData")
				.map(upgradeDataNbt -> new UpgradeData(upgradeDataNbt, world))
				.orElse(UpgradeData.NO_UPGRADE_DATA);
		boolean isLightCorrect = nbt.getBoolean("isLightOn", false);
		BlendingData.Serialized blendingData = nbt
				.<BlendingData.Serialized>get("blending_data", BlendingData.Serialized.CODEC)
				.orElse(null);
		BelowZeroRetrogen belowZeroRetrogen = nbt
				.<BelowZeroRetrogen>get("below_zero_retrogen", BelowZeroRetrogen.CODEC)
				.orElse(null);
		long[] carvingMaskData = nbt.getLongArray("carving_mask").orElse(null);
		Map<Heightmap.Type, long[]> heightmaps = new EnumMap<>(Heightmap.Type.class);

		nbt.getCompound("Heightmaps").ifPresent(heightmapsNbt -> {
			for (Heightmap.Type type : chunkStatus.getHeightmapTypes()) {
				heightmapsNbt.getLongArray(type.getId()).ifPresent(data -> heightmaps.put(type, data));
			}
		});

		List<Tick<Block>> blockTicks = Tick.filter(
				nbt.<List<Tick<Block>>>get("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()),
				chunkPos
		);
		List<Tick<Fluid>> fluidTicks = Tick.filter(
				nbt.<List<Tick<Fluid>>>get("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()),
				chunkPos
		);
		Chunk.TickSchedulers tickSchedulers = new Chunk.TickSchedulers(blockTicks, fluidTicks);
		NbtList postProcessingList = nbt.getListOrEmpty("PostProcessing");
		ShortList[] postProcessingSections = new ShortList[postProcessingList.size()];

		for (int sectionIndex = 0; sectionIndex < postProcessingList.size(); sectionIndex++) {
			NbtList sectionShorts = postProcessingList.getList(sectionIndex).orElse(null);
			if (sectionShorts == null || sectionShorts.isEmpty()) {
				continue;
			}

			ShortList shortList = new ShortArrayList(sectionShorts.size());

			for (int entryIndex = 0; entryIndex < sectionShorts.size(); entryIndex++) {
				shortList.add(sectionShorts.getShort(entryIndex, (short) 0));
			}

			postProcessingSections[sectionIndex] = shortList;
		}

		List<NbtCompound> entities = nbt.getList("entities").stream().flatMap(NbtList::streamCompounds).toList();
		List<NbtCompound> blockEntities = nbt.getList("block_entities").stream().flatMap(NbtList::streamCompounds).toList();
		NbtCompound structureData = nbt.getCompoundOrEmpty("structures");
		NbtList sectionsNbt = nbt.getListOrEmpty("sections");
		List<SerializedChunk.SectionData> sections = new ArrayList<>(sectionsNbt.size());
		Codec<ReadableContainer<RegistryEntry<Biome>>> biomeCodec = palettesFactory.biomeContainerCodec();
		Codec<PalettedContainer<BlockState>> blockStatesCodec = palettesFactory.blockStatesContainerCodec();

		for (int sectionIndex = 0; sectionIndex < sectionsNbt.size(); sectionIndex++) {
			Optional<NbtCompound> optionalSection = sectionsNbt.getCompound(sectionIndex);
			if (optionalSection.isEmpty()) {
				continue;
			}

			NbtCompound sectionNbt = optionalSection.get();
			int sectionY = sectionNbt.getByte("Y", (byte) 0);
			ChunkSection chunkSection;

			if (sectionY >= world.getBottomSectionCoord() && sectionY <= world.getTopSectionCoord()) {
				PalettedContainer<BlockState> blockStates = sectionNbt
						.getCompound("block_states")
						.map(blockStatesNbt -> (PalettedContainer<BlockState>) blockStatesCodec
								.parse(NbtOps.INSTANCE, blockStatesNbt)
								.promotePartial(error -> logRecoverableError(chunkPos, sectionY, error))
								.getOrThrow(SerializedChunk.ChunkLoadingException::new)
						)
						.orElseGet(palettesFactory::getBlockStateContainer);

				ReadableContainer<RegistryEntry<Biome>> biomes = sectionNbt
						.getCompound("biomes")
						.map(biomesNbt -> (ReadableContainer<RegistryEntry<Biome>>) biomeCodec
								.parse(NbtOps.INSTANCE, biomesNbt)
								.promotePartial(error -> logRecoverableError(chunkPos, sectionY, error))
								.getOrThrow(SerializedChunk.ChunkLoadingException::new)
						)
						.orElseGet(palettesFactory::getBiomeContainer);

				chunkSection = new ChunkSection(blockStates, biomes);
			} else {
				chunkSection = null;
			}

			ChunkNibbleArray blockLight = sectionNbt.getByteArray("BlockLight").map(ChunkNibbleArray::new).orElse(null);
			ChunkNibbleArray skyLight = sectionNbt.getByteArray("SkyLight").map(ChunkNibbleArray::new).orElse(null);
			sections.add(new SerializedChunk.SectionData(sectionY, chunkSection, blockLight, skyLight));
		}

		return new SerializedChunk(
				palettesFactory,
				chunkPos,
				world.getBottomSectionCoord(),
				lastUpdate,
				inhabitedTime,
				chunkStatus,
				blendingData,
				belowZeroRetrogen,
				upgradeData,
				carvingMaskData,
				heightmaps,
				tickSchedulers,
				postProcessingSections,
				isLightCorrect,
				sections,
				entities,
				blockEntities,
				structureData
		);
	}

	/**
	 * Конвертирует сериализованный чанк в объект {@link ProtoChunk} или {@link WrapperProtoChunk}.
	 * Восстанавливает секции, освещение, карты высот, структуры и данные пост-обработки.
	 * Если чанк имеет тип {@link ChunkType#LEVELCHUNK}, возвращает {@link WrapperProtoChunk}.
	 *
	 * @param world       серверный мир, в который загружается чанк
	 * @param poiStorage  хранилище точек интереса для инициализации POI по палитре блоков
	 * @param key         ключ хранилища для логирования при смещении чанка
	 * @param expectedPos ожидаемая позиция чанка (может не совпадать с сохранённой)
	 * @return готовый {@link ProtoChunk} или {@link WrapperProtoChunk}
	 */
	public ProtoChunk convert(
			ServerWorld world,
			PointOfInterestStorage poiStorage,
			StorageKey key,
			ChunkPos expectedPos
	) {
		if (!Objects.equals(expectedPos, chunkPos)) {
			LOGGER.error(
					"Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})",
					new Object[]{expectedPos, expectedPos, chunkPos}
			);
			world.getServer().onChunkMisplacement(chunkPos, expectedPos, key);
		}

		int sectionCount = world.countVerticalSections();
		ChunkSection[] chunkSections = new ChunkSection[sectionCount];
		boolean hasSkyLight = world.getDimension().hasSkyLight();
		LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
		PalettesFactory palettesFactory = world.getPalettesFactory();
		boolean lightRetainSet = false;

		for (SerializedChunk.SectionData section : sectionData) {
			ChunkSectionPos sectionPos = ChunkSectionPos.from(expectedPos, section.y);

			if (section.chunkSection != null) {
				chunkSections[world.sectionCoordToIndex(section.y)] = section.chunkSection;
				poiStorage.initForPalette(sectionPos, section.chunkSection);
			}

			boolean hasBlockLight = section.blockLight != null;
			boolean hasSectionSkyLight = hasSkyLight && section.skyLight != null;

			if (hasBlockLight || hasSectionSkyLight) {
				if (!lightRetainSet) {
					lightingProvider.setRetainData(expectedPos, true);
					lightRetainSet = true;
				}

				if (hasBlockLight) {
					lightingProvider.enqueueSectionData(LightType.BLOCK, sectionPos, section.blockLight);
				}

				if (hasSectionSkyLight) {
					lightingProvider.enqueueSectionData(LightType.SKY, sectionPos, section.skyLight);
				}
			}
		}

		ChunkType chunkType = chunkStatus.getChunkType();
		Chunk chunk;

		if (chunkType == ChunkType.LEVELCHUNK) {
			ChunkTickScheduler<Block> blockTickScheduler = new ChunkTickScheduler<>(packedTicks.blocks());
			ChunkTickScheduler<Fluid> fluidTickScheduler = new ChunkTickScheduler<>(packedTicks.fluids());
			chunk = new WorldChunk(
					world.toServerWorld(),
					expectedPos,
					upgradeData,
					blockTickScheduler,
					fluidTickScheduler,
					inhabitedTime,
					chunkSections,
					getEntityLoadingCallback(world, entities, blockEntities),
					BlendingData.fromSerialized(blendingData)
			);
		} else {
			SimpleTickScheduler<Block> blockTickScheduler = SimpleTickScheduler.tick(packedTicks.blocks());
			SimpleTickScheduler<Fluid> fluidTickScheduler = SimpleTickScheduler.tick(packedTicks.fluids());
			ProtoChunk protoChunk = new ProtoChunk(
					expectedPos,
					upgradeData,
					chunkSections,
					blockTickScheduler,
					fluidTickScheduler,
					world,
					palettesFactory,
					BlendingData.fromSerialized(blendingData)
			);
			chunk = protoChunk;
			protoChunk.setInhabitedTime(inhabitedTime);

			if (belowZeroRetrogen != null) {
				protoChunk.setBelowZeroRetrogen(belowZeroRetrogen);
			}

			protoChunk.setStatus(chunkStatus);

			if (chunkStatus.isAtLeast(ChunkStatus.INITIALIZE_LIGHT)) {
				protoChunk.setLightingProvider(lightingProvider);
			}
		}

		chunk.setLightOn(lightCorrect);
		EnumSet<Heightmap.Type> missingHeightmaps = EnumSet.noneOf(Heightmap.Type.class);

		for (Heightmap.Type type : chunk.getStatus().getHeightmapTypes()) {
			long[] heightmapData = heightmaps.get(type);

			if (heightmapData != null) {
				chunk.setHeightmap(type, heightmapData);
			} else {
				missingHeightmaps.add(type);
			}
		}

		Heightmap.populateHeightmaps(chunk, missingHeightmaps);
		chunk.setStructureStarts(readStructureStarts(
				StructureContext.from(world),
				structureData,
				world.getSeed()
		));
		chunk.setStructureReferences(readStructureReferences(
				world.getRegistryManager(),
				expectedPos,
				structureData
		));

		for (int sectionIndex = 0; sectionIndex < postProcessingSections.length; sectionIndex++) {
			ShortList shortList = postProcessingSections[sectionIndex];

			if (shortList != null) {
				chunk.markBlocksForPostProcessing(shortList, sectionIndex);
			}
		}

		if (chunkType == ChunkType.LEVELCHUNK) {
			return new WrapperProtoChunk((WorldChunk) chunk, false);
		}

		ProtoChunk protoChunk = (ProtoChunk) chunk;

		for (NbtCompound entityNbt : entities) {
			protoChunk.addEntity(entityNbt);
		}

		for (NbtCompound blockEntityNbt : blockEntities) {
			protoChunk.addPendingBlockEntityNbt(blockEntityNbt);
		}

		if (carvingMask != null) {
			protoChunk.setCarvingMask(new CarvingMask(carvingMask, chunk.getBottomY()));
		}

		return protoChunk;
	}

	private static void logRecoverableError(ChunkPos chunkPos, int sectionY, String message) {
		LOGGER.error(
				"Recoverable errors when loading section [{}, {}, {}]: {}",
				new Object[]{chunkPos.x, sectionY, chunkPos.z, message}
		);
	}

	/**
	 * Сериализует живой чанк в промежуточное представление для последующей записи на диск.
	 * Собирает данные всех секций, освещения, структур, тиков и карт высот.
	 *
	 * @param world серверный мир, из которого берётся провайдер освещения и время
	 * @param chunk чанк для сериализации; должен быть сериализуемым
	 * @return сериализованное представление чанка
	 * @throws IllegalArgumentException если чанк не поддерживает сериализацию
	 */
	public static SerializedChunk fromChunk(ServerWorld world, Chunk chunk) {
		if (!chunk.isSerializable()) {
			throw new IllegalArgumentException("Chunk can't be serialized: " + chunk);
		}

		ChunkPos chunkPos = chunk.getPos();
		List<SerializedChunk.SectionData> sections = new ArrayList<>();
		ChunkSection[] chunkSections = chunk.getSectionArray();
		LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();

		for (int sectionY = lightingProvider.getBottomY(); sectionY < lightingProvider.getTopY(); sectionY++) {
			int sectionIndex = chunk.sectionCoordToIndex(sectionY);
			boolean isValidSection = sectionIndex >= 0 && sectionIndex < chunkSections.length;
			ChunkNibbleArray rawBlockLight = lightingProvider
					.get(LightType.BLOCK)
					.getLightSection(ChunkSectionPos.from(chunkPos, sectionY));
			ChunkNibbleArray rawSkyLight = lightingProvider
					.get(LightType.SKY)
					.getLightSection(ChunkSectionPos.from(chunkPos, sectionY));
			ChunkNibbleArray blockLight = rawBlockLight != null && !rawBlockLight.isUninitialized()
					? rawBlockLight.copy()
					: null;
			ChunkNibbleArray skyLight = rawSkyLight != null && !rawSkyLight.isUninitialized()
					? rawSkyLight.copy()
					: null;

			if (isValidSection || blockLight != null || skyLight != null) {
				ChunkSection sectionCopy = isValidSection ? chunkSections[sectionIndex].copy() : null;
				sections.add(new SerializedChunk.SectionData(sectionY, sectionCopy, blockLight, skyLight));
			}
		}

		List<NbtCompound> blockEntities = new ArrayList<>(chunk.getBlockEntityPositions().size());

		for (BlockPos blockPos : chunk.getBlockEntityPositions()) {
			NbtCompound blockEntityNbt = chunk.getPackedBlockEntityNbt(blockPos, world.getRegistryManager());

			if (blockEntityNbt != null) {
				blockEntities.add(blockEntityNbt);
			}
		}

		List<NbtCompound> entities = new ArrayList<>();
		long[] carvingMaskData = null;

		if (chunk.getStatus().getChunkType() == ChunkType.PROTOCHUNK) {
			ProtoChunk protoChunk = (ProtoChunk) chunk;
			entities.addAll(protoChunk.getEntities());
			CarvingMask carvingMask = protoChunk.getCarvingMask();

			if (carvingMask != null) {
				carvingMaskData = carvingMask.getMask();
			}
		}

		Map<Heightmap.Type, long[]> heightmaps = new EnumMap<>(Heightmap.Type.class);

		for (Entry<Heightmap.Type, Heightmap> entry : chunk.getHeightmaps()) {
			if (chunk.getStatus().getHeightmapTypes().contains(entry.getKey())) {
				heightmaps.put(entry.getKey(), (long[]) entry.getValue().asLongArray().clone());
			}
		}

		Chunk.TickSchedulers tickSchedulers = chunk.getTickSchedulers(world.getTime());
		ShortList[] postProcessingSections = Arrays.stream(chunk.getPostProcessingLists())
				.map(list -> list != null && !list.isEmpty() ? new ShortArrayList(list) : null)
				.toArray(ShortList[]::new);
		NbtCompound structureData = writeStructures(
				StructureContext.from(world),
				chunkPos,
				chunk.getStructureStarts(),
				chunk.getStructureReferences()
		);

		return new SerializedChunk(
				world.getPalettesFactory(),
				chunkPos,
				chunk.getBottomSectionCoord(),
				world.getTime(),
				chunk.getInhabitedTime(),
				chunk.getStatus(),
				Nullables.map(chunk.getBlendingData(), BlendingData::toSerialized),
				chunk.getBelowZeroRetrogen(),
				chunk.getUpgradeData().copy(),
				carvingMaskData,
				heightmaps,
				tickSchedulers,
				postProcessingSections,
				chunk.isLightOn(),
				sections,
				entities,
				blockEntities,
				structureData
		);
	}

	/**
	 * Сериализует промежуточное представление в NBT-тег для записи на диск.
	 * Записывает все секции, освещение, структуры, тики, карты высот и метаданные.
	 *
	 * @return корневой NBT-тег чанка с актуальной версией данных
	 */
	public NbtCompound serialize() {
		NbtCompound root = NbtHelper.putDataVersion(new NbtCompound());
		root.putInt("xPos", chunkPos.x);
		root.putInt("yPos", minSectionY);
		root.putInt("zPos", chunkPos.z);
		root.putLong("LastUpdate", lastUpdateTime);
		root.putLong("InhabitedTime", inhabitedTime);
		root.putString("Status", Registries.CHUNK_STATUS.getId(chunkStatus).toString());
		root.putNullable("blending_data", BlendingData.Serialized.CODEC, blendingData);
		root.putNullable("below_zero_retrogen", BelowZeroRetrogen.CODEC, belowZeroRetrogen);

		if (!upgradeData.isDone()) {
			root.put("UpgradeData", upgradeData.toNbt());
		}

		NbtList sectionsNbt = new NbtList();
		Codec<PalettedContainer<BlockState>> blockStatesCodec = containerFactory.blockStatesContainerCodec();
		Codec<ReadableContainer<RegistryEntry<Biome>>> biomeCodec = containerFactory.biomeContainerCodec();

		for (SerializedChunk.SectionData section : sectionData) {
			NbtCompound sectionNbt = new NbtCompound();
			ChunkSection chunkSection = section.chunkSection;

			if (chunkSection != null) {
				sectionNbt.put("block_states", blockStatesCodec, chunkSection.getBlockStateContainer());
				sectionNbt.put("biomes", biomeCodec, chunkSection.getBiomeContainer());
			}

			if (section.blockLight != null) {
				sectionNbt.putByteArray("BlockLight", section.blockLight.asByteArray());
			}

			if (section.skyLight != null) {
				sectionNbt.putByteArray("SkyLight", section.skyLight.asByteArray());
			}

			if (!sectionNbt.isEmpty()) {
				sectionNbt.putByte("Y", (byte) section.y);
				sectionsNbt.add(sectionNbt);
			}
		}

		root.put("sections", sectionsNbt);

		if (lightCorrect) {
			root.putBoolean("isLightOn", true);
		}

		NbtList blockEntitiesNbt = new NbtList();
		blockEntitiesNbt.addAll(blockEntities);
		root.put("block_entities", blockEntitiesNbt);

		if (chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
			NbtList entitiesNbt = new NbtList();
			entitiesNbt.addAll(entities);
			root.put("entities", entitiesNbt);

			if (carvingMask != null) {
				root.putLongArray("carving_mask", carvingMask);
			}
		}

		serializeTicks(root, packedTicks);
		root.put("PostProcessing", toNbt(postProcessingSections));
		NbtCompound heightmapsNbt = new NbtCompound();
		heightmaps.forEach((type, values) -> heightmapsNbt.put(type.getId(), new NbtLongArray(values)));
		root.put("Heightmaps", heightmapsNbt);
		root.put("structures", structureData);

		return root;
	}

	private static void serializeTicks(NbtCompound nbt, Chunk.TickSchedulers schedulers) {
		nbt.put("block_ticks", BLOCK_TICKS_CODEC, schedulers.blocks());
		nbt.put("fluid_ticks", FLUID_TICKS_CODEC, schedulers.fluids());
	}

	public static ChunkStatus getChunkStatus(@Nullable NbtCompound nbt) {
		return nbt != null
				? nbt.<ChunkStatus>get("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY)
				: ChunkStatus.EMPTY;
	}

	private static WorldChunk.@Nullable EntityLoader getEntityLoadingCallback(
			ServerWorld world,
			List<NbtCompound> entities,
			List<NbtCompound> blockEntities
	) {
		if (entities.isEmpty() && blockEntities.isEmpty()) {
			return null;
		}

		return chunk -> {
			if (!entities.isEmpty()) {
				try (ErrorReporter.Logging logging = new ErrorReporter.Logging(
						chunk.getErrorReporterContext(),
						LOGGER
				)) {
					world.loadEntities(EntityType.streamFromData(
							NbtReadView.createList(logging, world.getRegistryManager(), entities),
							world,
							SpawnReason.LOAD
					));
				}
			}

			for (NbtCompound blockEntityNbt : blockEntities) {
				boolean keepPacked = blockEntityNbt.getBoolean("keepPacked", false);

				if (keepPacked) {
					chunk.addPendingBlockEntityNbt(blockEntityNbt);
				} else {
					BlockPos blockPos = BlockEntity.posFromNbt(chunk.getPos(), blockEntityNbt);
					BlockEntity blockEntity = BlockEntity.createFromNbt(
							blockPos,
							chunk.getBlockState(blockPos),
							blockEntityNbt,
							world.getRegistryManager()
					);

					if (blockEntity != null) {
						chunk.setBlockEntity(blockEntity);
					}
				}
			}
		};
	}

	private static NbtCompound writeStructures(
			StructureContext context,
			ChunkPos pos,
			Map<Structure, StructureStart> starts,
			Map<Structure, LongSet> references
	) {
		NbtCompound root = new NbtCompound();
		NbtCompound startsNbt = new NbtCompound();
		Registry<Structure> registry = context.registryManager().getOrThrow(RegistryKeys.STRUCTURE);

		for (Entry<Structure, StructureStart> entry : starts.entrySet()) {
			Identifier id = registry.getId(entry.getKey());
			startsNbt.put(id.toString(), entry.getValue().toNbt(context, pos));
		}

		root.put("starts", startsNbt);
		NbtCompound referencesNbt = new NbtCompound();

		for (Entry<Structure, LongSet> entry : references.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				Identifier id = registry.getId(entry.getKey());
				referencesNbt.putLongArray(id.toString(), entry.getValue().toLongArray());
			}
		}

		root.put("References", referencesNbt);

		return root;
	}

	private static Map<Structure, StructureStart> readStructureStarts(
			StructureContext context,
			NbtCompound nbt,
			long worldSeed
	) {
		Map<Structure, StructureStart> result = Maps.newHashMap();
		Registry<Structure> registry = context.registryManager().getOrThrow(RegistryKeys.STRUCTURE);
		NbtCompound startsNbt = nbt.getCompoundOrEmpty("starts");

		for (String key : startsNbt.getKeys()) {
			Identifier identifier = Identifier.tryParse(key);
			Structure structure = registry.get(identifier);

			if (structure == null) {
				LOGGER.error("Unknown structure start: {}", identifier);
				continue;
			}

			StructureStart structureStart = StructureStart.fromNbt(
					context,
					startsNbt.getCompoundOrEmpty(key),
					worldSeed
			);

			if (structureStart != null) {
				result.put(structure, structureStart);
			}
		}

		return result;
	}

	private static Map<Structure, LongSet> readStructureReferences(
			DynamicRegistryManager registryManager,
			ChunkPos pos,
			NbtCompound nbt
	) {
		Map<Structure, LongSet> result = Maps.newHashMap();
		Registry<Structure> registry = registryManager.getOrThrow(RegistryKeys.STRUCTURE);
		NbtCompound referencesNbt = nbt.getCompoundOrEmpty("References");

		referencesNbt.forEach((id, value) -> {
			Identifier identifier = Identifier.tryParse(id);
			Structure structure = registry.get(identifier);

			if (structure == null) {
				LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", identifier, pos);
				return;
			}

			Optional<long[]> packedPositions = value.asLongArray();

			if (packedPositions.isEmpty()) {
				return;
			}

			result.put(
					structure,
					new LongOpenHashSet(
							Arrays.stream(packedPositions.get())
									.filter(packedPos -> {
										ChunkPos refPos = new ChunkPos(packedPos);

										if (refPos.getChebyshevDistance(pos) > 8) {
											LOGGER.warn(
													"Found invalid structure reference [ {} @ {} ] for chunk {}.",
													new Object[]{identifier, refPos, pos}
											);
											return false;
										}

										return true;
									})
									.toArray()
					)
			);
		});

		return result;
	}

	private static NbtList toNbt(@Nullable ShortList[] lists) {
		NbtList result = new NbtList();

		for (ShortList shortList : lists) {
			NbtList sectionNbt = new NbtList();

			if (shortList != null) {
				for (int i = 0; i < shortList.size(); i++) {
					sectionNbt.add(NbtShort.of(shortList.getShort(i)));
				}
			}

			result.add(sectionNbt);
		}

		return result;
	}

	/**
	 * Исключение, выбрасываемое при критической ошибке загрузки чанка из NBT.
	 */
	public static class ChunkLoadingException extends NbtException {

		public ChunkLoadingException(String string) {
			super(string);
		}
	}

	/**
	 * Данные одной вертикальной секции чанка (16×16×16 блоков).
	 * Содержит секцию блоков и данные освещения для записи/чтения с диска.
	 */
	public record SectionData(
			int y,
			@Nullable ChunkSection chunkSection,
			@Nullable ChunkNibbleArray blockLight,
			@Nullable ChunkNibbleArray skyLight
	) {
	}
}
