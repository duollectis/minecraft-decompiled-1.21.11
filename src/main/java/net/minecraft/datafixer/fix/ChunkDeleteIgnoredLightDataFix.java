package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет данные освещения ({@code BlockLight}, {@code SkyLight}) из секций чанка,
 * если флаг {@code isLightOn} не установлен — такие данные устарели и будут пересчитаны.
 */
public class ChunkDeleteIgnoredLightDataFix extends DataFix {

	public ChunkDeleteIgnoredLightDataFix(Schema outputSchema) {
		super(outputSchema, true);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> sectionsFinder = chunkType.findField("sections");

		return fixTypeEverywhereTyped(
			"ChunkDeleteIgnoredLightDataFix",
			chunkType,
			typed -> {
				boolean isLightOn = ((Dynamic<?>) typed.get(DSL.remainderFinder())).get("isLightOn").asBoolean(false);

				return isLightOn
					? typed
					: typed.updateTyped(
						sectionsFinder,
						section -> section.update(
							DSL.remainderFinder(),
							dynamic -> dynamic.remove("BlockLight").remove("SkyLight")
						)
					);
			}
		);
	}
}
