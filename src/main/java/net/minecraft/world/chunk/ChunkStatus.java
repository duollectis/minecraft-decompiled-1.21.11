package net.minecraft.world.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Статус генерации чанка. Определяет, на каком этапе конвейера генерации
 * находится чанк — от пустого ({@link #EMPTY}) до полностью загруженного ({@link #FULL}).
 * Статусы образуют упорядоченную цепочку через поле {@link #previous}.
 */
public class ChunkStatus {

	public static final int RADIUS = 8;

	private static final EnumSet<Heightmap.Type> WORLD_GEN_HEIGHTMAP_TYPES = EnumSet.of(
		Heightmap.Type.OCEAN_FLOOR_WG,
		Heightmap.Type.WORLD_SURFACE_WG
	);

	public static final EnumSet<Heightmap.Type> NORMAL_HEIGHTMAP_TYPES = EnumSet.of(
		Heightmap.Type.OCEAN_FLOOR,
		Heightmap.Type.WORLD_SURFACE,
		Heightmap.Type.MOTION_BLOCKING,
		Heightmap.Type.MOTION_BLOCKING_NO_LEAVES
	);

	public static final ChunkStatus EMPTY = register("empty", null, WORLD_GEN_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK);
	public static final ChunkStatus STRUCTURE_STARTS = register(
		"structure_starts", EMPTY, WORLD_GEN_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus STRUCTURE_REFERENCES = register(
		"structure_references", STRUCTURE_STARTS, WORLD_GEN_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus BIOMES = register(
		"biomes", STRUCTURE_REFERENCES, WORLD_GEN_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus NOISE = register(
		"noise", BIOMES, WORLD_GEN_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus SURFACE = register(
		"surface", NOISE, WORLD_GEN_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus CARVERS = register(
		"carvers", SURFACE, NORMAL_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus FEATURES = register(
		"features", CARVERS, NORMAL_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus INITIALIZE_LIGHT = register(
		"initialize_light", FEATURES, NORMAL_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus LIGHT = register(
		"light", INITIALIZE_LIGHT, NORMAL_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus SPAWN = register(
		"spawn", LIGHT, NORMAL_HEIGHTMAP_TYPES, ChunkType.PROTOCHUNK
	);
	public static final ChunkStatus FULL = register(
		"full", SPAWN, NORMAL_HEIGHTMAP_TYPES, ChunkType.LEVELCHUNK
	);

	public static final Codec<ChunkStatus> CODEC = Registries.CHUNK_STATUS.getCodec();

	private final int index;
	private final ChunkStatus previous;
	private final ChunkType chunkType;
	private final EnumSet<Heightmap.Type> heightMapTypes;

	private static ChunkStatus register(
		String id,
		@Nullable ChunkStatus previous,
		EnumSet<Heightmap.Type> heightMapTypes,
		ChunkType chunkType
	) {
		return Registry.register(
			Registries.CHUNK_STATUS,
			id,
			new ChunkStatus(previous, heightMapTypes, chunkType)
		);
	}

	/**
	 * Строит упорядоченный список всех статусов от {@link #EMPTY} до {@link #FULL}.
	 * Обходит цепочку {@link #previous} в обратном направлении, затем разворачивает.
	 */
	public static List<ChunkStatus> createOrderedList() {
		List<ChunkStatus> list = Lists.newArrayList();

		ChunkStatus current;
		for (current = FULL; current.getPrevious() != current; current = current.getPrevious()) {
			list.add(current);
		}

		list.add(current);
		Collections.reverse(list);
		return list;
	}

	@VisibleForTesting
	protected ChunkStatus(
		@Nullable ChunkStatus previous,
		EnumSet<Heightmap.Type> heightMapTypes,
		ChunkType chunkType
	) {
		this.previous = previous == null ? this : previous;
		this.chunkType = chunkType;
		this.heightMapTypes = heightMapTypes;
		this.index = previous == null ? 0 : previous.getIndex() + 1;
	}

	public int getIndex() {
		return index;
	}

	public ChunkStatus getPrevious() {
		return previous;
	}

	public ChunkType getChunkType() {
		return chunkType;
	}

	public static ChunkStatus byId(String id) {
		return Registries.CHUNK_STATUS.get(Identifier.tryParse(id));
	}

	public EnumSet<Heightmap.Type> getHeightmapTypes() {
		return heightMapTypes;
	}

	public boolean isAtLeast(ChunkStatus other) {
		return index >= other.index;
	}

	public boolean isLaterThan(ChunkStatus other) {
		return index > other.index;
	}

	public boolean isAtMost(ChunkStatus other) {
		return index <= other.index;
	}

	public boolean isEarlierThan(ChunkStatus other) {
		return index < other.index;
	}

	public static ChunkStatus max(ChunkStatus a, ChunkStatus b) {
		return a.isLaterThan(b) ? a : b;
	}

	@Override
	public String toString() {
		return getId();
	}

	public String getId() {
		return Registries.CHUNK_STATUS.getId(this).toString();
	}
}
