package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

import java.util.Optional;

/**
 * Исправляет данные в формате DataFixer.
 */
public class ObjectiveRenderTypeFix extends DataFix {

	public ObjectiveRenderTypeFix(Schema schema) {
		super(schema, false);
	}

	private static String parseLegacyRenderType(String oldName) {
		return oldName.equals("health") ? "hearts" : "integer";
	}

	protected TypeRewriteRule makeRule() {
		Type<?> type = getInputSchema().getType(TypeReferences.OBJECTIVE);
		return fixTypeEverywhereTyped(
				"ObjectiveRenderTypeFix", type, typed -> typed.update(
						DSL.remainderFinder(), objective -> {
							Optional<String> optional = objective.get("RenderType").asString().result();
							if (optional.isEmpty()) {
								String string = objective.get("CriteriaName").asString("");
								String string2 = parseLegacyRenderType(string);
								return objective.set("RenderType", objective.createString(string2));
							}
							else {
								return objective;
							}
						}
				)
		);
	}
}
