package net.minecraft.util.profiling.jfr.event;

import jdk.jfr.*;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

/**
 * JFR-событие генерации структуры. Фиксирует позицию чанка, идентификатор структуры,
 * измерение и результат (успех/неудача) для профилирования генерации мира.
 */
@Name("minecraft.StructureGeneration")
@Label("Structure Generation")
@Category({"Minecraft", "World Generation"})
@StackTrace(false)
@Enabled(false)
@DontObfuscate
public class StructureGenerationEvent extends Event {

	public static final String EVENT_NAME = "minecraft.StructureGeneration";
	public static final EventType TYPE = EventType.getEventType(StructureGenerationEvent.class);
	@Name("chunkPosX")
	@Label("Chunk X Position")
	public final int chunkPosX;
	@Name("chunkPosZ")
	@Label("Chunk Z Position")
	public final int chunkPosZ;
	@Name("structure")
	@Label("Structure")
	public final String structure;
	@Name("level")
	@Label("Level")
	public final String level;
	@Name("success")
	@Label("Success")
	public boolean success;

	public StructureGenerationEvent(
			ChunkPos chunkPos,
			RegistryEntry<Structure> structure,
			RegistryKey<World> dimension
	) {
		this.chunkPosX = chunkPos.x;
		this.chunkPosZ = chunkPos.z;
		this.structure = structure.getIdAsString();
		this.level = dimension.getValue().toString();
	}

	/** Строковые константы имён JFR-полей события для программного доступа к метаданным. */
	public interface Names {

		String CHUNK_POS_X = "chunkPosX";

		String CHUNK_POS_Z = "chunkPosZ";

		String STRUCTURE = "structure";

		String LEVEL = "level";

		String SUCCESS = "success";
	}
}
