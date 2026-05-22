package net.minecraft.datafixer;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;

import java.util.Set;

/**
 * Перечисление всех типов данных Minecraft, которые подлежат миграции через DataFixer.
 * Каждый элемент связан с соответствующим {@link TypeReference} из {@link TypeReferences}.
 */
public enum DataFixTypes {
	LEVEL(TypeReferences.LEVEL),
	LEVEL_SUMMARY(TypeReferences.LIGHTWEIGHT_LEVEL),
	PLAYER(TypeReferences.PLAYER),
	CHUNK(TypeReferences.CHUNK),
	HOTBAR(TypeReferences.HOTBAR),
	OPTIONS(TypeReferences.OPTIONS),
	STRUCTURE(TypeReferences.STRUCTURE),
	STATS(TypeReferences.STATS),
	SAVED_DATA_COMMAND_STORAGE(TypeReferences.SAVED_DATA_COMMAND_STORAGE),
	SAVED_DATA_FORCED_CHUNKS(TypeReferences.TICKETS_SAVED_DATA),
	SAVED_DATA_MAP_DATA(TypeReferences.SAVED_DATA_MAP_DATA),
	SAVED_DATA_MAP_INDEX(TypeReferences.SAVED_DATA_IDCOUNTS),
	SAVED_DATA_RAIDS(TypeReferences.SAVED_DATA_RAIDS),
	SAVED_DATA_RANDOM_SEQUENCES(TypeReferences.SAVED_DATA_RANDOM_SEQUENCES),
	SAVED_DATA_SCOREBOARD(TypeReferences.SAVED_DATA_SCOREBOARD),
	SAVED_DATA_STOPWATCHES(TypeReferences.STOPWATCHES_SAVED_DATA),
	SAVED_DATA_STRUCTURE_FEATURE_INDICES(TypeReferences.SAVED_DATA_STRUCTURE_FEATURE_INDICES),
	SAVED_DATA_WORLD_BORDER(TypeReferences.WORLD_BORDER_SAVED_DATA),
	ADVANCEMENTS(TypeReferences.ADVANCEMENTS),
	POI_CHUNK(TypeReferences.POI_CHUNK),
	WORLD_GEN_SETTINGS(TypeReferences.WORLD_GEN_SETTINGS),
	ENTITY_CHUNK(TypeReferences.ENTITY_CHUNK),
	DEBUG_PROFILE(TypeReferences.DEBUG_PROFILE);

	public static final Set<TypeReference> REQUIRED_TYPES = Set.of(LEVEL_SUMMARY.typeReference);

	private final TypeReference typeReference;

	DataFixTypes(final TypeReference typeReference) {
		this.typeReference = typeReference;
	}

	static int getSaveVersionId() {
		return SharedConstants.getGameVersion().dataVersion().id();
	}

	/**
	 * Создаёт {@link Codec}, который автоматически применяет DataFixer при декодировании
	 * и записывает текущую версию данных при кодировании.
	 *
	 * @param baseCodec          базовый кодек для сериализации объекта
	 * @param dataFixer          фиксер для миграции данных
	 * @param currentDataVersion версия данных, используемая как fallback при отсутствии поля DataVersion
	 */
	public <A> Codec<A> createDataFixingCodec(Codec<A> baseCodec, DataFixer dataFixer, int currentDataVersion) {
		return new Codec<>() {
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				return baseCodec.encode(input, ops, prefix)
					.flatMap(encoded -> ops.mergeToMap(
						encoded,
						ops.createString("DataVersion"),
						ops.createInt(DataFixTypes.getSaveVersionId())
					));
			}

			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				int version = ops.get(input, "DataVersion")
					.flatMap(ops::getNumberValue)
					.map(Number::intValue)
					.result()
					.orElse(currentDataVersion);

				Dynamic<T> dynamic = new Dynamic<>(ops, ops.remove(input, "DataVersion"));
				Dynamic<T> updated = DataFixTypes.this.update(dataFixer, dynamic, version);

				return baseCodec.decode(updated);
			}
		};
	}

	/**
	 * Обновляет данные с указанной старой версии до указанной новой версии.
	 *
	 * @param dataFixer  фиксер данных
	 * @param dynamic    входные данные
	 * @param oldVersion исходная версия данных
	 * @param newVersion целевая версия данных
	 */
	public <T> Dynamic<T> update(DataFixer dataFixer, Dynamic<T> dynamic, int oldVersion, int newVersion) {
		return dataFixer.update(typeReference, dynamic, oldVersion, newVersion);
	}

	public <T> Dynamic<T> update(DataFixer dataFixer, Dynamic<T> dynamic, int oldVersion) {
		return update(dataFixer, dynamic, oldVersion, getSaveVersionId());
	}

	public NbtCompound update(DataFixer dataFixer, NbtCompound nbt, int oldVersion, int newVersion) {
		return (NbtCompound) update(dataFixer, new Dynamic<>(NbtOps.INSTANCE, nbt), oldVersion, newVersion)
			.getValue();
	}

	public NbtCompound update(DataFixer dataFixer, NbtCompound nbt, int oldVersion) {
		return update(dataFixer, nbt, oldVersion, getSaveVersionId());
	}
}
