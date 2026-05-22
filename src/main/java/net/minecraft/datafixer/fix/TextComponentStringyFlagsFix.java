package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class TextComponentStringyFlagsFix extends DataFix {

	public TextComponentStringyFlagsFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	@SuppressWarnings("unchecked")
	protected TypeRewriteRule makeRule() {
		Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>>
				type =
				(Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>>) this
						.getInputSchema()
						.getType(TypeReferences.TEXT_COMPONENT);
		return fixTypeEverywhere(
				"TextComponentStringyFlagsFix",
				type,
				dynamicOps -> pair -> pair.mapSecond(
						either -> either.mapRight(
								pairx -> pairx.mapSecond(
										pairxx -> pairxx.mapSecond(
												pairxxx -> pairxxx.mapSecond(
														dynamic -> dynamic
																.update(
																		"bold",
																		TextComponentStringyFlagsFix::convertStringToBoolean
																)
																.update(
																		"italic",
																		TextComponentStringyFlagsFix::convertStringToBoolean
																)
																.update(
																		"underlined",
																		TextComponentStringyFlagsFix::convertStringToBoolean
																)
																.update(
																		"strikethrough",
																		TextComponentStringyFlagsFix::convertStringToBoolean
																)
																.update(
																		"obfuscated",
																		TextComponentStringyFlagsFix::convertStringToBoolean
																)
												)
										)
								)
						)
				)
		);
	}

	private static <T> Dynamic<T> convertStringToBoolean(Dynamic<T> dynamic) {
		Optional<String> optional = dynamic.asString().result();
		return optional.isPresent() ? dynamic.createBoolean(Boolean.parseBoolean(optional.get())) : dynamic;
	}
}
