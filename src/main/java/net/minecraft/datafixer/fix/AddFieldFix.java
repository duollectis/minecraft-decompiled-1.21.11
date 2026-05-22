package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Добавляет новое поле с дефолтным значением в указанный тип данных.
 * Поддерживает опциональный путь вложенности через {@code copiedFields}.
 */
public class AddFieldFix extends DataFix {

	private final String description;
	private final TypeReference typeReference;
	private final String fieldName;
	private final String[] copiedFields;
	private final Function<Dynamic<?>, Dynamic<?>> defaultValueGetter;

	public AddFieldFix(
		Schema outputSchema,
		TypeReference typeReference,
		String fieldName,
		Function<Dynamic<?>, Dynamic<?>> defaultValueGetter,
		String... copiedFields
	) {
		super(outputSchema, false);
		description = "Adding field `" + fieldName + "` to type `"
			+ typeReference.typeName().toLowerCase(Locale.ROOT) + "`";
		this.typeReference = typeReference;
		this.fieldName = fieldName;
		this.copiedFields = copiedFields;
		this.defaultValueGetter = defaultValueGetter;
	}

	@Override
	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
			description,
			getInputSchema().getType(typeReference),
			getOutputSchema().getType(typeReference),
			typed -> typed.update(DSL.remainderFinder(), value -> fix(value, 0))
		);
	}

	/**
	 * Рекурсивно спускается по цепочке copiedFields, чтобы найти вложенный Dynamic,
	 * в который нужно добавить поле. Если цепочка исчерпана — добавляет поле с дефолтным значением.
	 */
	private Dynamic<?> fix(Dynamic<?> value, int index) {
		if (index >= copiedFields.length) {
			return value.set(fieldName, defaultValueGetter.apply(value));
		}

		Optional<? extends Dynamic<?>> nested = value.get(copiedFields[index]).result();

		return nested.isEmpty() ? value : fix(nested.get(), index + 1);
	}
}
