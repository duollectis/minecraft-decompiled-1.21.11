package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

import java.util.stream.Collectors;

/**
 * Исправляет данные в формате DataFixer.
 */
public class OptionsKeyTranslationFix extends DataFix {

	public OptionsKeyTranslationFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"OptionsKeyTranslationFix",
				getInputSchema().getType(TypeReferences.OPTIONS),
				optionsTyped -> optionsTyped.update(
						DSL.remainderFinder(),
						optionsDynamic -> optionsDynamic
								.getMapValues()
								.map(optionsMap -> optionsDynamic.createMap(optionsMap
										.entrySet()
										.stream()
										.map(entry -> {
											if (((Dynamic) entry.getKey()).asString("").startsWith("key_")) {
												String string = ((Dynamic) entry.getValue()).asString("");
												if (!string.startsWith("key.mouse")
														&& !string.startsWith("scancode.")) {
													return Pair.of((Dynamic) entry.getKey(),
															optionsDynamic.createString(
																	"key.keyboard." + string.substring("key.".length()))
													);
												}
											}

											return Pair.of((Dynamic) entry.getKey(), (Dynamic) entry.getValue());
										})
										.collect(Collectors.toMap(Pair::getFirst, Pair::getSecond))))
								.result()
								.orElse((Dynamic) optionsDynamic)
				)
		);
	}
}
