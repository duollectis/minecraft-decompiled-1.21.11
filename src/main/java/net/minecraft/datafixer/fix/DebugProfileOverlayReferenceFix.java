package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * {@code DebugProfileOverlayReferenceFix}.
 */
public class DebugProfileOverlayReferenceFix extends DataFix {

	public DebugProfileOverlayReferenceFix(Schema schema) {
		super(schema, false);
	}

	protected TypeRewriteRule makeRule() {
		return this.fixTypeEverywhereTyped(
				"DebugProfileOverlayReferenceFix",
				this.getInputSchema().getType(TypeReferences.DEBUG_PROFILE),
				typed -> typed.update(
						DSL.remainderFinder(),
						profile -> profile.update(
								"custom",
								map -> map.updateMapValues(pair -> pair.mapSecond(value ->
										value.asString("").equals("inF3") ? value.createString("inOverlay") : value))
						)
				)
		);
	}
}
