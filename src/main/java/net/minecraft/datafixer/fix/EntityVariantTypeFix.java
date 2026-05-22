package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Конвертирует числовое поле варианта сущности (например, {@code Type}) в строковый
 * идентификатор ресурса (например, {@code "minecraft:temperate"}) и записывает
 * результат в унифицированное поле {@code variant}.
 * Используется для лошадей, кошек, лягушек и других сущностей с вариантами.
 */
public class EntityVariantTypeFix extends ChoiceFix {

	private final String variantKey;
	private final IntFunction<String> variantIntToId;

	public EntityVariantTypeFix(
		Schema outputSchema,
		String name,
		TypeReference type,
		String entityId,
		String variantKey,
		IntFunction<String> variantIntToId
	) {
		super(outputSchema, false, name, type, entityId);
		this.variantKey = variantKey;
		this.variantIntToId = variantIntToId;
	}

	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		return inputTyped.update(
			DSL.remainderFinder(),
			entity -> updateEntity(
				entity,
				variantKey,
				"variant",
				variantDynamic -> (Dynamic) DataFixUtils.orElse(
					variantDynamic
						.asNumber()
						.map(variantInt -> variantDynamic.createString(variantIntToId.apply(variantInt.intValue())))
						.result(),
					variantDynamic
				)
			)
		);
	}

	/**
	 * Читает числовое значение поля {@code oldVariantKey}, преобразует его через
	 * {@code variantConverter} и записывает результат в поле {@code newVariantKey}.
	 * Операция выполняется на уровне сырых данных {@link DynamicOps} для избежания
	 * лишних аллокаций при обходе типовой системы DFU.
	 */
	private static <T> Dynamic<T> updateEntity(
		Dynamic<T> entity,
		String oldVariantKey,
		String newVariantKey,
		Function<Dynamic<T>, Dynamic<T>> variantConverter
	) {
		return entity.map(rawData -> {
			DynamicOps<T> ops = entity.getOps();
			Function<T, T> rawConverter = raw -> variantConverter.apply(new Dynamic<>(ops, raw)).getValue();

			return ops.get(rawData, oldVariantKey)
				.map(oldValue -> ops.set(rawData, newVariantKey, rawConverter.apply(oldValue)))
				.result()
				.orElse(rawData);
		});
	}
}
