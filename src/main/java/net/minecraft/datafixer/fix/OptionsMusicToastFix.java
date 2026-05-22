package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class OptionsMusicToastFix extends DataFix {

	public OptionsMusicToastFix(Schema outputSchema, boolean changesType) {
		super(outputSchema, changesType);
	}

	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"OptionsMusicToastFix",
				getInputSchema().getType(TypeReferences.OPTIONS),
				typed -> typed.update(
						DSL.remainderFinder(),
						options -> options.renameAndFixField(
								"showNowPlayingToast",
								"musicToast",
								value -> options.createString(
										value.asString("false").equals("false") ? "never" : "pause_and_toast")
						)
				)
		);
	}
}
