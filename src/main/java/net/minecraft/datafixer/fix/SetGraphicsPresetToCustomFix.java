package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class SetGraphicsPresetToCustomFix extends DataFix {

	public SetGraphicsPresetToCustomFix(Schema schema) {
		super(schema, true);
	}

	public TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"graphicsPreset set to \"custom\"",
				getInputSchema().getType(TypeReferences.OPTIONS),
				typed -> typed.update(
						DSL.remainderFinder(),
						options -> options.set("graphicsPreset", options.createString("custom"))
				)
		);
	}
}
