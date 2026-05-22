package net.minecraft.world.gen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.structure.Structure;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Предоставляет доступ к структурам мира для генерации и запросов.
 * Является фасадом над {@link StructureLocator} и {@link WorldAccess}.
 */
public class StructureAccessor {

	private final WorldAccess world;
	private final GeneratorOptions options;
	private final StructureLocator locator;

	public StructureAccessor(WorldAccess world, GeneratorOptions options, StructureLocator locator) {
		this.world = world;
		this.options = options;
		this.locator = locator;
	}

	/**
	 * Создаёт новый {@code StructureAccessor}, ограниченный регионом чанков.
	 * Проверяет, что регион принадлежит тому же серверному миру.
	 *
	 * @throws IllegalStateException если регион принадлежит другому миру
	 */
	public StructureAccessor forRegion(ChunkRegion region) {
		if (region.toServerWorld() != world) {
			throw new IllegalStateException(
				"Using invalid structure manager (source level: " + region.toServerWorld() + ", region: " + region
			);
		}

		return new StructureAccessor(region, options, locator);
	}

	public List<StructureStart> getStructureStarts(ChunkPos pos, Predicate<Structure> predicate) {
		Map<Structure, LongSet> references =
			world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_REFERENCES).getStructureReferences();
		Builder<StructureStart> builder = ImmutableList.builder();

		for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
			Structure structure = entry.getKey();

			if (predicate.test(structure)) {
				acceptStructureStarts(structure, entry.getValue(), builder::add);
			}
		}

		return builder.build();
	}

	public List<StructureStart> getStructureStarts(ChunkSectionPos sectionPos, Structure structure) {
		LongSet positions = world
			.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.STRUCTURE_REFERENCES)
			.getStructureReferences(structure);
		Builder<StructureStart> builder = ImmutableList.builder();
		acceptStructureStarts(structure, positions, builder::add);
		return builder.build();
	}

	/**
	 * Перебирает все стартовые позиции структуры и передаёт валидные старты в {@code consumer}.
	 * Старт считается валидным, если он не null и содержит дочерние элементы.
	 */
	public void acceptStructureStarts(
		Structure structure,
		LongSet structureStartPositions,
		Consumer<StructureStart> consumer
	) {
		for (long packedPos : structureStartPositions) {
			ChunkSectionPos sectionPos = ChunkSectionPos.from(new ChunkPos(packedPos), world.getBottomSectionCoord());
			StructureStart start = getStructureStart(
				sectionPos,
				structure,
				world.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.STRUCTURE_STARTS)
			);

			if (start != null && start.hasChildren()) {
				consumer.accept(start);
			}
		}
	}

	public @Nullable StructureStart getStructureStart(
		ChunkSectionPos pos,
		Structure structure,
		StructureHolder holder
	) {
		return holder.getStructureStart(structure);
	}

	public void setStructureStart(
		ChunkSectionPos pos,
		Structure structure,
		StructureStart structureStart,
		StructureHolder holder
	) {
		holder.setStructureStart(structure, structureStart);
	}

	public void addStructureReference(
		ChunkSectionPos pos,
		Structure structure,
		long reference,
		StructureHolder holder
	) {
		holder.addStructureReference(structure, reference);
	}

	public boolean shouldGenerateStructures() {
		return options.shouldGenerateStructures();
	}

	public StructureStart getStructureAt(BlockPos pos, Structure structure) {
		for (StructureStart start : getStructureStarts(ChunkSectionPos.from(pos), structure)) {
			if (start.getBoundingBox().contains(pos)) {
				return start;
			}
		}

		return StructureStart.DEFAULT;
	}

	public StructureStart getStructureContaining(BlockPos pos, TagKey<Structure> tag) {
		return getStructureContaining(pos, structure -> structure.isIn(tag));
	}

	public StructureStart getStructureContaining(BlockPos pos, RegistryEntryList<Structure> structures) {
		return getStructureContaining(pos, structures::contains);
	}

	/**
	 * Ищет структуру, содержащую позицию, среди структур удовлетворяющих предикату.
	 * Использует реестр структур для получения записи по сырому идентификатору.
	 */
	public StructureStart getStructureContaining(BlockPos pos, Predicate<RegistryEntry<Structure>> predicate) {
		Registry<Structure> registry = getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);

		for (StructureStart start : getStructureStarts(
			new ChunkPos(pos),
			structure -> registry.getEntry(registry.getRawId(structure)).map(predicate::test).orElse(false)
		)) {
			if (structureContains(pos, start)) {
				return start;
			}
		}

		return StructureStart.DEFAULT;
	}

	public StructureStart getStructureContaining(BlockPos pos, Structure structure) {
		for (StructureStart start : getStructureStarts(ChunkSectionPos.from(pos), structure)) {
			if (structureContains(pos, start)) {
				return start;
			}
		}

		return StructureStart.DEFAULT;
	}

	public boolean structureContains(BlockPos pos, StructureStart structureStart) {
		for (StructurePiece piece : structureStart.getChildren()) {
			if (piece.getBoundingBox().contains(pos)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasStructureReferences(BlockPos pos) {
		ChunkSectionPos sectionPos = ChunkSectionPos.from(pos);
		return world
			.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.STRUCTURE_REFERENCES)
			.hasStructureReferences();
	}

	public Map<Structure, LongSet> getStructureReferences(BlockPos pos) {
		ChunkSectionPos sectionPos = ChunkSectionPos.from(pos);
		return world
			.getChunk(sectionPos.getSectionX(), sectionPos.getSectionZ(), ChunkStatus.STRUCTURE_REFERENCES)
			.getStructureReferences();
	}

	public StructurePresence getStructurePresence(
		ChunkPos chunkPos,
		Structure structure,
		StructurePlacement placement,
		boolean skipReferencedStructures
	) {
		return locator.getStructurePresence(chunkPos, structure, placement, skipReferencedStructures);
	}

	public void incrementReferences(StructureStart structureStart) {
		structureStart.incrementReferences();
		locator.incrementReferences(structureStart.getPos(), structureStart.getStructure());
	}

	public DynamicRegistryManager getRegistryManager() {
		return world.getRegistryManager();
	}
}
