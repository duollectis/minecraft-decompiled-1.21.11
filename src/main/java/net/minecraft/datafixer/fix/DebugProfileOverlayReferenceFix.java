package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Переименовывает устаревшее значение {@code inF3} в {@code inOverlay}
 * в пользовательских настройках отображения профиля отладки.
 */
public class DebugProfileOverlayReferenceFix extends DataFix {

	public DebugProfileOverlayReferenceFix(Schema schema) {
		super(schema, false);
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"DebugProfileOverlayReferenceFix",
				getInputSchema().getType(TypeReferences.DEBUG_PROFILE),
				typed -> typed.update(
						DSL.remainderFinder(),
						profile -> profile.update(
								"custom",
								map -> map.updateMapValues(pair -> pair.mapSecond(
										value -> "inF3".equals(value.asString(""))
												? value.createString("inOverlay")
												: value
								))
						)
				)
		);
	}
}
