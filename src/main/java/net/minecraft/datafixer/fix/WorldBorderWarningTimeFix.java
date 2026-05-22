package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class WorldBorderWarningTimeFix extends DataFix {

	public WorldBorderWarningTimeFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		return writeFixAndRead(
				"WorldBorderWarningTimeFix",
				getInputSchema().getType(TypeReferences.WORLD_BORDER_SAVED_DATA),
				getOutputSchema().getType(TypeReferences.WORLD_BORDER_SAVED_DATA),
				dynamic -> dynamic.update("data",
						dynamicx -> dynamicx.update(
								"warning_time",
								dynamic2 -> dynamicx.createInt(dynamic2.asInt(15) * 20)
						)
				)
		);
	}
}
