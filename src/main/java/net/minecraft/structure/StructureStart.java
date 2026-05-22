package net.minecraft.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.OceanMonumentStructure;
import net.minecraft.world.gen.structure.Structure;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

/**
 * Представляет стартовую точку сгенерированной структуры в мире.
 * Хранит список кусков {@link StructurePiece}, позицию чанка и счётчик ссылок.
 * Ссылки используются для предотвращения повторной генерации структуры.
 */
public final class StructureStart {

	public static final String INVALID = "INVALID";
	public static final StructureStart DEFAULT = new StructureStart(
		null,
		new ChunkPos(0, 0),
		0,
		new StructurePiecesList(List.of())
	);

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MIN_REFERENCES = 1;

	private final Structure structure;
	private final StructurePiecesList children;
	private final ChunkPos pos;
	private int references;
	private volatile @Nullable BlockBox boundingBox;

	public StructureStart(Structure structure, ChunkPos pos, int references, StructurePiecesList children) {
		this.structure = structure;
		this.pos = pos;
		this.references = references;
		this.children = children;
	}

	/**
	 * Десериализует старт структуры из NBT.
	 * Возвращает {@link #DEFAULT} для невалидных структур, {@code null} при ошибке.
	 */
	public static @Nullable StructureStart fromNbt(StructureContext context, NbtCompound nbt, long seed) {
		String id = nbt.getString("id", "");
		if (INVALID.equals(id)) {
			return DEFAULT;
		}

		Registry<Structure> registry = context.registryManager().getOrThrow(RegistryKeys.STRUCTURE);
		Structure structure = registry.get(Identifier.of(id));
		if (structure == null) {
			LOGGER.error("Unknown stucture id: {}", id);
			return null;
		}

		ChunkPos chunkPos = new ChunkPos(nbt.getInt("ChunkX", 0), nbt.getInt("ChunkZ", 0));
		int refCount = nbt.getInt("references", 0);
		NbtList childrenNbt = nbt.getListOrEmpty("Children");

		try {
			StructurePiecesList pieces = StructurePiecesList.fromNbt(childrenNbt, context);
			if (structure instanceof OceanMonumentStructure) {
				pieces = OceanMonumentStructure.modifyPiecesOnRead(chunkPos, seed, pieces);
			}

			return new StructureStart(structure, chunkPos, refCount, pieces);
		} catch (Exception exception) {
			LOGGER.error("Failed Start with id {}", id, exception);
			return null;
		}
	}

	/**
	 * Возвращает ограничивающий блок старта, вычисляя его лениво при первом обращении.
	 * Результат кешируется для последующих вызовов.
	 */
	public BlockBox getBoundingBox() {
		BlockBox cached = boundingBox;
		if (cached == null) {
			cached = structure.expandBoxIfShouldAdaptNoise(children.getBoundingBox());
			boundingBox = cached;
		}

		return cached;
	}

	/**
	 * Размещает все куски структуры в мире для заданного чанка.
	 * Передаёт нижнюю центральную точку первого куска как опорную для генерации.
	 */
	public void place(
		StructureWorldAccess world,
		StructureAccessor structureAccessor,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockBox chunkBox,
		ChunkPos chunkPos
	) {
		List<StructurePiece> pieces = children.pieces();
		if (pieces.isEmpty()) {
			return;
		}

		BlockBox firstBox = pieces.get(0).boundingBox;
		BlockPos center = firstBox.getCenter();
		BlockPos pivot = new BlockPos(center.getX(), firstBox.getMinY(), center.getZ());

		for (StructurePiece piece : pieces) {
			if (piece.getBoundingBox().intersects(chunkBox)) {
				piece.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot);
			}
		}

		structure.postPlace(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, children);
	}

	/**
	 * Сериализует старт структуры в NBT.
	 * Если кусков нет — записывает маркер {@link #INVALID}.
	 */
	public NbtCompound toNbt(StructureContext context, ChunkPos chunkPos) {
		NbtCompound nbt = new NbtCompound();
		if (!hasChildren()) {
			nbt.putString("id", INVALID);
			return nbt;
		}

		nbt.putString(
			"id",
			context.registryManager().getOrThrow(RegistryKeys.STRUCTURE).getId(structure).toString()
		);
		nbt.putInt("ChunkX", chunkPos.x);
		nbt.putInt("ChunkZ", chunkPos.z);
		nbt.putInt("references", references);
		nbt.put("Children", children.toNbt(context));
		return nbt;
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	public ChunkPos getPos() {
		return pos;
	}

	/**
	 * Возвращает {@code true}, если структура ещё не была достаточно обработана.
	 * Используется для предотвращения повторной генерации.
	 */
	public boolean isNeverReferenced() {
		return references < MIN_REFERENCES;
	}

	public void incrementReferences() {
		references++;
	}

	public int getReferences() {
		return references;
	}

	public Structure getStructure() {
		return structure;
	}

	public List<StructurePiece> getChildren() {
		return children.pieces();
	}
}
