package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import net.minecraft.datafixer.TypeReferences;

/**
 * Удаляет устаревшие данные освещения из чанка: флаг {@code isLightOn} и массивы
 * {@code BlockLight}/{@code SkyLight} из каждой секции. Освещение будет пересчитано движком.
 */
public class ChunkDeleteLightFix extends DataFix {

	public ChunkDeleteLightFix(Schema outputSchema) {
		super(outputSchema, false);
	}

	protected TypeRewriteRule makeRule() {
		Type<?> chunkType = getInputSchema().getType(TypeReferences.CHUNK);
		OpticFinder<?> sectionsFinder = chunkType.findField("sections");

		return fixTypeEverywhereTyped(
			"ChunkDeleteLightFix for " + getOutputSchema().getVersionKey(),
			chunkType,
			typed -> typed
				.update(DSL.remainderFinder(), dynamic -> dynamic.remove("isLightOn"))
				.updateTyped(
					sectionsFinder,
					section -> section.update(
						DSL.remainderFinder(),
						dynamic -> dynamic.remove("BlockLight").remove("SkyLight")
					)
				)
		);
	}
}
