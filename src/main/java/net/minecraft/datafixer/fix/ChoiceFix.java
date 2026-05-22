package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;

/**
 * Абстрактный базовый класс для фиксов, применяемых к конкретному именованному варианту
 * (choice) внутри типа данных (сущность, блок-сущность и т.д.).
 * Подклассы реализуют {@link #transform(Typed)} для преобразования данных конкретного варианта.
 */
public abstract class ChoiceFix extends DataFix {

	private final String name;
	protected final String choiceName;
	protected final TypeReference type;

	public ChoiceFix(Schema outputSchema, boolean changesType, String name, TypeReference type, String choiceName) {
		super(outputSchema, changesType);
		this.name = name;
		this.type = type;
		this.choiceName = choiceName;
	}

	public TypeRewriteRule makeRule() {
		OpticFinder<?> opticFinder = DSL.namedChoice(choiceName, getInputSchema().getChoiceType(type, choiceName));

		return fixTypeEverywhereTyped(
			name,
			getInputSchema().getType(type),
			getOutputSchema().getType(type),
			typed -> typed.updateTyped(
				opticFinder,
				getOutputSchema().getChoiceType(type, choiceName),
				this::transform
			)
		);
	}

	protected abstract Typed<?> transform(Typed<?> inputTyped);
}
