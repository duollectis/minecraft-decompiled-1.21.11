package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class OptionsAddTextBackgroundFix extends DataFix {

	public OptionsAddTextBackgroundFix(Schema schema, boolean bl) {
		super(schema, bl);
	}

	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"OptionsAddTextBackgroundFix",
				getInputSchema().getType(TypeReferences.OPTIONS),
				optionsTyped -> optionsTyped.update(
						DSL.remainderFinder(), optionsDynamic -> (Dynamic) DataFixUtils.orElse(
								optionsDynamic.get("chatOpacity").asString().map(string -> {
									double d = this.convertToTextBackgroundOpacity(string);
									return optionsDynamic.set(
											"textBackgroundOpacity",
											optionsDynamic.createString(String.valueOf(d))
									);
								}).result(), optionsDynamic
						)
				)
		);
	}

	private double convertToTextBackgroundOpacity(String chatOpacity) {
		try {
			double d = 0.9 * Double.parseDouble(chatOpacity) + 0.1;
			return d / 2.0;
		}
		catch (NumberFormatException var4) {
			return 0.5;
		}
	}
}
