package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class OptionsMenuBlurrinessFix extends DataFix {

	public OptionsMenuBlurrinessFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"OptionsMenuBlurrinessFix",
				getInputSchema().getType(TypeReferences.OPTIONS),
				optionsTyped -> optionsTyped.update(
						DSL.remainderFinder(), optionsDynamic -> optionsDynamic.update(
								"menuBackgroundBlurriness", dynamic -> {
									int i = this.update(dynamic.asString("0.5"));
									return dynamic.createString(String.valueOf(i));
								}
						)
				)
		);
	}

	private int update(String value) {
		try {
			return Math.round(Float.parseFloat(value) * 10.0F);
		}
		catch (NumberFormatException var3) {
			return 5;
		}
	}
}
