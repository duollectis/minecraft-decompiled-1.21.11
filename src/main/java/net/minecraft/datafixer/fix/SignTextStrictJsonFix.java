package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;

import java.util.List;

/**
 * Исправляет данные в формате DataFixer.
 */
public class SignTextStrictJsonFix extends ChoiceFix {

	private static final List<String> TEXT_KEYS = List.of("Text1", "Text2", "Text3", "Text4");

	public SignTextStrictJsonFix(Schema outputSchema) {
		super(outputSchema, false, "SignTextStrictJsonFix", TypeReferences.BLOCK_ENTITY, "Sign");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Typed<?> transform(Typed<?> inputTyped) {
		for (String string : TEXT_KEYS) {
			OpticFinder<?> opticFinder = inputTyped.getType().findField(string);
			OpticFinder<Pair<String, String>>
					opticFinder2 =
					(OpticFinder<Pair<String, String>>) DSL.typeFinder(this
							.getInputSchema()
							.getType(TypeReferences.TEXT_COMPONENT));
			inputTyped =
					inputTyped.updateTyped(
							opticFinder,
							typed -> typed.update(opticFinder2, pair -> pair.mapSecond(TextFixes::parseLenientJson))
					);
		}

		return inputTyped;
	}
}
