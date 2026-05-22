package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.datafixer.TypeReferences;

import java.util.function.Function;

/**
 * Мигрирует формат чанка: удаляет обёртку {@code Level}, переименовывает поля
 * ({@code TileEntities} → {@code block_entities}, {@code Sections} → {@code sections} и т.д.)
 * и поднимает данные уровня на верхний уровень NBT-структуры чанка.
 */
public class ChunkLevelTagRenameFix extends DataFix {

	public ChunkLevelTagRenameFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> inputChunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> levelFinder = inputChunkType.findField("Level");
		OpticFinder<?> structuresFinder = levelFinder.type().findField("Structures");
		Type<?> outputChunkType = getOutputSchema().getType(TypeReferences.CHUNK);
		Type<?> outputStructuresType = outputChunkType.findFieldType("structures");

		return fixTypeEverywhereTyped(
				"Chunk Renames; purge Level-tag",
				inputChunkType,
				outputChunkType,
				chunkTyped -> {
					Typed<?> levelTyped = chunkTyped.getTyped(levelFinder);
					Typed<?> result = labelWithChunk(levelTyped);
					result = result.set(
							DSL.remainderFinder(),
							mergeChunkData(chunkTyped, (Dynamic) levelTyped.get(DSL.remainderFinder()))
					);
					result = rename(result, "TileEntities", "block_entities");
					result = rename(result, "TileTicks", "block_ticks");
					result = rename(result, "Entities", "entities");
					result = rename(result, "Sections", "sections");
					result = result.updateTyped(
							structuresFinder,
							outputStructuresType,
							structuresTyped -> rename(structuresTyped, "Starts", "starts")
					);
					result = rename(result, "Structures", "structures");
					return result.update(DSL.remainderFinder(), dynamic -> dynamic.remove("Level"));
				}
		);
	}

	private static Typed<?> rename(Typed<?> typed, String oldKey, String newKey) {
		return rename(typed, oldKey, newKey, typed.getType().findFieldType(oldKey)).update(
				DSL.remainderFinder(),
				dynamic -> dynamic.remove(oldKey)
		);
	}

	private static <A> Typed<?> rename(Typed<?> typed, String oldKey, String newKey, Type<A> fieldType) {
		Type<Either<A, Unit>> oldFieldType = DSL.optional(DSL.field(oldKey, fieldType));
		Type<Either<A, Unit>> newFieldType = DSL.optional(DSL.field(newKey, fieldType));
		return typed.update(oldFieldType.finder(), newFieldType, Function.identity());
	}

	@SuppressWarnings("unchecked")
	private static <A> Typed<Pair<String, A>> labelWithChunk(Typed<A> levelTyped) {
		return new Typed<>(
				DSL.named("chunk", levelTyped.getType()),
				levelTyped.getOps(),
				Pair.of("chunk", levelTyped.getValue())
		);
	}

	/**
	 * Объединяет данные из корня чанка (xPos, zPos и т.д.) с данными тега Level,
	 * чтобы после удаления обёртки Level все поля оказались на верхнем уровне.
	 */
	@SuppressWarnings("unchecked")
	private static <T> Dynamic<T> mergeChunkData(Typed<?> chunkTyped, Dynamic<T> levelDynamic) {
		DynamicOps<T> ops = levelDynamic.getOps();
		Dynamic<T> chunkRemainder = ((Dynamic<T>) chunkTyped.get(DSL.remainderFinder())).convert(ops);
		DataResult<T> merged = ops
				.getMap(levelDynamic.getValue())
				.flatMap(mapLike -> ops.mergeToMap(chunkRemainder.getValue(), mapLike));
		return merged.result().map(value -> new Dynamic<>(ops, value)).orElse(levelDynamic);
	}
}
