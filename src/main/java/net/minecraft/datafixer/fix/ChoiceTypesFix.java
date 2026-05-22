package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;

import java.util.Locale;

/**
 * Мигрирует все варианты (choices) типа данных из входной схемы в выходную,
 * проверяя, что каждый встреченный вариант присутствует в новой схеме.
 */
public class ChoiceTypesFix extends DataFix {

	private final String name;
	private final TypeReference types;

	public ChoiceTypesFix(Schema outputSchema, String name, TypeReference types) {
		super(outputSchema, true);
		this.name = name;
		this.types = types;
	}

	public TypeRewriteRule makeRule() {
		TaggedChoiceType<?> inputChoiceType = getInputSchema().findChoiceType(types);
		TaggedChoiceType<?> outputChoiceType = getOutputSchema().findChoiceType(types);
		return fixChoiceTypes(inputChoiceType, outputChoiceType);
	}

	@SuppressWarnings("unchecked")
	private <K> TypeRewriteRule fixChoiceTypes(
		TaggedChoiceType<K> inputChoiceType,
		TaggedChoiceType<?> outputChoiceType
	) {
		if (inputChoiceType.getKeyType() != outputChoiceType.getKeyType()) {
			throw new IllegalStateException("Could not inject: key type is not the same");
		}

		TaggedChoiceType<K> typedOutput = (TaggedChoiceType<K>) outputChoiceType;

		return fixTypeEverywhere(
			name, inputChoiceType, typedOutput, dynamicOps -> pair -> {
				if (!typedOutput.hasType(pair.getFirst())) {
					throw new IllegalArgumentException(String.format(
						Locale.ROOT,
						"%s: Unknown type %s in '%s'",
						name,
						pair.getFirst(),
						types.typeName()
					));
				}

				return pair;
			}
		);
	}
}
