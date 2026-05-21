package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;

import java.util.Locale;

/**
 * {@code ChoiceTypesFix}.
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
		TaggedChoiceType<?> taggedChoiceType = this.getInputSchema().findChoiceType(this.types);
		TaggedChoiceType<?> taggedChoiceType2 = this.getOutputSchema().findChoiceType(this.types);
		return this.fixChoiceTypes(taggedChoiceType, taggedChoiceType2);
	}

	@SuppressWarnings("unchecked")
	private <K> TypeRewriteRule fixChoiceTypes(
			TaggedChoiceType<K> inputChoiceType,
			TaggedChoiceType<?> outputChoiceType
	) {
		if (inputChoiceType.getKeyType() != outputChoiceType.getKeyType()) {
			throw new IllegalStateException("Could not inject: key type is not the same");
		}
		else {
			TaggedChoiceType<K> typedOutput = (TaggedChoiceType<K>) outputChoiceType;
			return this.fixTypeEverywhere(
					this.name, inputChoiceType, typedOutput, dynamicOps -> pair -> {
						if (!typedOutput.hasType(pair.getFirst())) {
							throw new IllegalArgumentException(String.format(
									Locale.ROOT,
									"%s: Unknown type %s in '%s'",
									this.name,
									pair.getFirst(),
									this.types.typeName()
							));
						}
						else {
							return pair;
						}
					}
			);
		}
	}
}
