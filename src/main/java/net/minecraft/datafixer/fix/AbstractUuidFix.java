package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Базовый класс для фиксов, мигрирующих UUID из различных устаревших форматов
 * (строка, составной тег M/L, пара Most/Least) в новый формат int[4].
 */
public abstract class AbstractUuidFix extends DataFix {

	protected final TypeReference typeReference;

	public AbstractUuidFix(Schema outputSchema, TypeReference typeReference) {
		super(outputSchema, false);
		this.typeReference = typeReference;
	}

	/**
	 * Применяет функцию {@code updater} к remainder-данным конкретного именованного варианта типа.
	 *
	 * @param typed   входной типизированный объект
	 * @param name    имя варианта (например, {@code "minecraft:conduit"})
	 * @param updater функция преобразования Dynamic
	 */
	protected Typed<?> updateTyped(Typed<?> typed, String name, Function<Dynamic<?>, Dynamic<?>> updater) {
		Type<?> inputType = getInputSchema().getChoiceType(typeReference, name);
		Type<?> outputType = getOutputSchema().getChoiceType(typeReference, name);

		return typed.updateTyped(
			DSL.namedChoice(name, inputType),
			outputType,
			inner -> inner.update(DSL.remainderFinder(), updater)
		);
	}

	protected static Optional<Dynamic<?>> updateStringUuid(Dynamic<?> dynamic, String oldKey, String newKey) {
		return createArrayFromStringUuid(dynamic, oldKey)
			.map(array -> dynamic.remove(oldKey).set(newKey, array));
	}

	protected static Optional<Dynamic<?>> updateCompoundUuid(Dynamic<?> dynamic, String oldKey, String newKey) {
		return dynamic.get(oldKey)
			.result()
			.flatMap(AbstractUuidFix::createArrayFromCompoundUuid)
			.map(array -> dynamic.remove(oldKey).set(newKey, array));
	}

	protected static Optional<Dynamic<?>> updateRegularMostLeast(Dynamic<?> dynamic, String oldKey, String newKey) {
		String mostKey = oldKey + "Most";
		String leastKey = oldKey + "Least";

		return createArrayFromMostLeastTags(dynamic, mostKey, leastKey)
			.map(array -> dynamic.remove(mostKey).remove(leastKey).set(newKey, array));
	}

	protected static Optional<Dynamic<?>> createArrayFromStringUuid(Dynamic<?> dynamic, String key) {
		return dynamic.get(key).result().flatMap(value -> {
			String uuidString = value.asString(null);

			if (uuidString == null) {
				return Optional.empty();
			}

			try {
				UUID uuid = UUID.fromString(uuidString);
				return createArray(dynamic, uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
			} catch (IllegalArgumentException ignored) {
				return Optional.empty();
			}
		});
	}

	protected static Optional<Dynamic<?>> createArrayFromCompoundUuid(Dynamic<?> dynamic) {
		return createArrayFromMostLeastTags(dynamic, "M", "L");
	}

	protected static Optional<Dynamic<?>> createArrayFromMostLeastTags(
		Dynamic<?> dynamic,
		String mostBitsKey,
		String leastBitsKey
	) {
		long mostBits = dynamic.get(mostBitsKey).asLong(0L);
		long leastBits = dynamic.get(leastBitsKey).asLong(0L);

		return mostBits != 0L && leastBits != 0L
			? createArray(dynamic, mostBits, leastBits)
			: Optional.empty();
	}

	/**
	 * Упаковывает 128-битный UUID в массив из 4 int-значений (big-endian).
	 */
	protected static Optional<Dynamic<?>> createArray(Dynamic<?> dynamic, long mostBits, long leastBits) {
		return Optional.of(dynamic.createIntList(Arrays.stream(new int[]{
			(int) (mostBits >> 32),
			(int) mostBits,
			(int) (leastBits >> 32),
			(int) leastBits
		})));
	}
}
