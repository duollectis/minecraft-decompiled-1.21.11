package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

/**
 * Исправляет данные в формате DataFixer.
 */
public class PlayerRespawnDataFix extends DataFix {

	public PlayerRespawnDataFix(Schema schema) {
		super(schema, false);
	}

	protected TypeRewriteRule makeRule() {
		return fixTypeEverywhereTyped(
				"PlayerRespawnDataFix",
				getInputSchema().getType(TypeReferences.PLAYER),
				typed -> typed.update(
						DSL.remainderFinder(),
						dynamic -> dynamic.update(
								"respawn",
								dynamicx -> dynamicx
										.set(
												"dimension",
												dynamicx.createString(dynamicx
														.get("dimension")
														.asString("minecraft:overworld"))
										)
										.set("yaw", dynamicx.createFloat(dynamicx.get("angle").asFloat(0.0F)))
										.set("pitch", dynamicx.createFloat(0.0F))
										.remove("angle")
						)
				)
		);
	}
}
