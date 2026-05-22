package net.minecraft.datafixer.fix;

import com.mojang.datafixers.*;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.util.Util;

/**
 * Абстрактный фикс для преобразования данных конкретного именованного варианта (choice)
 * с полным циклом write-read: данные читаются через входную схему, трансформируются
 * и записываются в выходную схему.
 */
public abstract class ChoiceWriteReadFix extends DataFix {

	private final String name;
	private final String choiceName;
	private final TypeReference type;

	public ChoiceWriteReadFix(Schema schema, boolean changesType, String name, TypeReference typeReference, String choiceName) {
		super(schema, changesType);
		this.name = name;
		this.type = typeReference;
		this.choiceName = choiceName;
	}

	public TypeRewriteRule makeRule() {
		Type<?> inputType = getInputSchema().getType(type);
		Type<?> inputChoiceType = getInputSchema().getChoiceType(type, choiceName);
		Type<?> outputType = getOutputSchema().getType(type);
		OpticFinder<?> choiceFinder = DSL.namedChoice(choiceName, inputChoiceType);
		Type<?> transitionalType = FixUtil.withTypeChanged(inputType, inputType, outputType);
		return buildRule(inputType, outputType, transitionalType, choiceFinder);
	}

	private <S, T, A> TypeRewriteRule buildRule(
		Type<S> inputType,
		Type<T> outputType,
		Type<?> transitionalType,
		OpticFinder<A> choiceFinder
	) {
		return fixTypeEverywhereTyped(
			name, inputType, outputType, typed -> {
				if (typed.getOptional(choiceFinder).isEmpty()) {
					return FixUtil.withType(outputType, typed);
				}

				Typed<?> transitional = FixUtil.withType(transitionalType, typed);
				return Util.apply((Typed<A>) transitional, outputType, this::transform);
			}
		);
	}

	protected abstract <T> Dynamic<T> transform(Dynamic<T> data);
}
